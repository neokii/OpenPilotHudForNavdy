package com.neokii.openpilot.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;


import com.neokii.openpilot.MainApp;
import com.neokii.openpilot.util.ParcelableUtil;
import com.neokii.openpilot.util.SettingUtil;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executors;

public abstract class BaseBluetooth
{
    private static final String TAG = "BaseBluetooth";

    protected abstract String getName();
    protected abstract UUID getUUID();
    protected abstract String getPrefrenceKeyConnectedAddress();

    public static final int MESSAGE_STATE_CHANGED = 1;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public static final int DATA_TYPE_BUNDLE = 1;


    private int state = STATE_NONE;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private AcceptThread acceptThread;
    private ClientThread clientThread;

    private List<OnStateChangedListener> stateChangedListeners = new ArrayList<>();
    private List<OnDataListener> dataListeners = new ArrayList<>();

    public interface OnStateChangedListener
    {
        void onStateChanged(int state);
    }

    public interface OnDataListener
    {
        void onData(Bundle bundle);
    }

    public void addStateChangedListener(OnStateChangedListener listener)
    {
        if(!stateChangedListeners.contains(listener))
            stateChangedListeners.add(listener);
    }

    public void removeStateChangedListener(OnStateChangedListener listener)
    {
        stateChangedListeners.remove(listener);
    }

    public void addDataListener(OnDataListener listener)
    {
        if(!dataListeners.contains(listener))
            dataListeners.add(listener);
    }

    public void removeDataListener(OnDataListener listener)
    {
        dataListeners.remove(listener);
    }

    public OutputStream getOutputStream()
    {
        try
        {
            return clientThread.getBluetoothSocket().getOutputStream();
        }
        catch(Exception e){}
        return null;
    }

    private class AcceptThread extends Thread
    {
        private BluetoothServerSocket serverSocket = null;

        public AcceptThread()
        {
            try
            {
                serverSocket = BluetoothAdapter.getDefaultAdapter()
                        .listenUsingRfcommWithServiceRecord(getName(), getUUID());
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void run()
        {
            Log.d(TAG, "AcceptThread run: " + serverSocket);

            setName("AcceptThread");

            while(!interrupted())
            {
                BluetoothSocket socket = null;

                try
                {
                    socket = serverSocket.accept();
                }
                /*catch(IOException e)
                {
                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch(Exception e2){}
                }*/
                catch(Exception e)
                {
                    e.printStackTrace();
                    break;
                }

                if(socket != null)
                {
                    connected(socket);
                }
                else
                {
                    try
                    {
                        socket.close();
                    }
                    catch(Exception e){}
                }
            }

            Log.i(TAG, "END AcceptThread");
        }

        public void close()
        {
            interrupt();

            try
            {
                serverSocket.close();
            }
            catch (Throwable e){}

            try
            {
                join(1000);
            }
            catch (Throwable e){}
        }
    }

    private synchronized void connected(BluetoothSocket socket)
    {
        if(clientThread != null)
        {
            try{ socket.close(); }catch(Exception e){}
            return;
        }

        String key = getPrefrenceKeyConnectedAddress();
        if(!TextUtils.isEmpty(key))
        {
            /*String address = SettingUtil.getString(HudApplication.getAppContext(), getPrefrenceKeyConnectedAddress(), "");
            if(!socket.getRemoteDevice().getAddress().equals(address))
                return;*/

            SettingUtil.setString(MainApp.getAppContext(), key, socket.getRemoteDevice().getAddress());
        }

        Log.i(TAG, "connected: " + socket);

        setState(STATE_CONNECTED);
        clientThread = new ClientThread(socket);
        clientThread.start();
    }

    private synchronized void setState(final int state)
    {
        if(this.state != state)
        {
            this.state = state;

            if(stateChangedListeners != null)
            {
                handler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for(OnStateChangedListener listener : stateChangedListeners)
                        {
                            listener.onStateChanged(getState());
                        }
                    }
                });
            }
        }
    }

    public synchronized int getState()
    {
        return state;
    }

    public void startServer()
    {
        Log.d(TAG, "startServer: " + acceptThread);

        if(acceptThread != null)
            return;

        //stopServer();

        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    public void stopServer()
    {
        try
        {
            if(acceptThread != null)
            {
                acceptThread.close();
                acceptThread = null;
            }
        }
        catch(Exception e){}
    }

    public void closeClient()
    {
        try
        {
            if(clientThread != null)
            {
                clientThread.close();
                clientThread = null;
            }
        }
        catch(Exception e){}

        setState(STATE_NONE);
    }


    public interface OnConnectCompletion
    {
        void onConnectCompletion(int state);
    }

    Queue<OnConnectCompletion> connectCompletions = new LinkedList<>();

    public void connect(final BluetoothDevice device, final OnConnectCompletion completion)
    {
        if(getState() != STATE_NONE)
        {
            if(getState() == STATE_CONNECTED)
            {
                if(completion != null)
                    completion.onConnectCompletion(getState());
            }
            else
            {
                if(completion != null)
                    connectCompletions.offer(completion);
            }

            return;
        }

        if(completion != null)
            connectCompletions.offer(completion);

        Log.d(TAG, "connect request: " + connectCompletions);

        setState(STATE_CONNECTING);

        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

        Executors.newCachedThreadPool().execute(new Runnable()
        {
            @Override
            public void run()
            {
                BluetoothSocket socket = null;
                try
                {
                    socket = device.createRfcommSocketToServiceRecord(getUUID());
                    socket.connect();
                    connected(socket);

                    handler.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Log.d(TAG, "connect result: " + connectCompletions);

                            while(!connectCompletions.isEmpty())
                                connectCompletions.poll().onConnectCompletion(getState());
                        }
                    });

                    return;
                }
                catch(Exception e)
                {
                    Log.d(TAG, "connect result error: " + e);
                }

                setState(STATE_NONE);

                handler.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Log.d(TAG, "connect result: " + connectCompletions);

                        while(!connectCompletions.isEmpty())
                            connectCompletions.poll().onConnectCompletion(getState());
                    }
                });
            }
        });
    }

    public void connect(OnConnectCompletion completion)
    {
        String address = SettingUtil.getString(MainApp.getAppContext(), getPrefrenceKeyConnectedAddress(), "");
        if(!TextUtils.isEmpty(address))
        {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
            connect(device, completion);
        }
    }

    private class ClientThread extends Thread
    {
        BluetoothSocket socket;

        public ClientThread(BluetoothSocket socket)
        {
            setName("ClientThread");
            this.socket = socket;
        }

        @Override
        public void run()
        {
            Log.i(TAG, "ClientThread run start");

            byte[] buffer;

            while(state == STATE_CONNECTED && !interrupted())
            {
                try
                {
                    DataInputStream stream = new DataInputStream(socket.getInputStream());

                    int type = stream.readShort();
                    int size = stream.readInt();
                    if(size < 0 || size > 1024*1024*10)
                        throw new IOException("Packet size is too much");

                    buffer = new byte[size];
                    stream.readFully(buffer);

                    processPacket(type, buffer);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    break;
                }
            }

            closeClient();

            Log.d(TAG, "END ClientThread");
        }

        public void write(byte[] buffer)
        {
            try
            {
                socket.getOutputStream().write(buffer);
            }
            catch(Throwable e)
            {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public BluetoothSocket getBluetoothSocket()
        {
            return socket;
        }

        public void close()
        {
            interrupt();

            try {
                socket.close();
            } catch (Throwable e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

        private void processPacket(int type, byte[] buf)
        {
            try
            {
                if(type == DATA_TYPE_BUNDLE)
                {
                    processBundle(ParcelableUtil.unmarshall(buf, Bundle.CREATOR));
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        private void processBundle(final Bundle bundle)
        {
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    for(OnDataListener listener : dataListeners)
                    {
                        listener.onData(bundle);
                    }
                }
            });
        }
    }
}

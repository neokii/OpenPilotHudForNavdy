package com.neokii.openpilot;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.neokii.openpilot.bluetooth.BaseBluetooth;
import com.neokii.openpilot.bluetooth.NavdyBT;
import com.neokii.openpilot.poller.Poller;
import com.neokii.openpilot.util.SettingUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ai.comma.openpilot.cereal.Car;
import ai.comma.openpilot.cereal.Log;

public class MainService extends Service
{
    private static final String TAG = "MainService";

    Timer timer;
    Handler handlerTimer;
    ScheduledExecutorService service;

    Sender[] senders = new Sender[] {
            new Sender("controlsState", 300),
            //new Sender("carControl", 300),
            new Sender("carState", -1),
            new Sender("pathPlan", 300),
            new Sender("radarState", 300),
            new Sender("thermal", 2000),
    };

    class Sender implements Poller.OnReceiveListener, Runnable
    {
        String endpoint;
        long period;

        ScheduledFuture<?> scheduledFuture;
        Poller poller;

        Sender(String endpoint, long period)
        {
            this.endpoint = endpoint;
            this.period = period;
        }

        public void start()
        {
            if(period > 0)
                scheduledFuture = service.scheduleWithFixedDelay(this, (long)(Math.random()*1000), period, TimeUnit.MILLISECONDS);

            poller = new Poller(this);
            poller.start(endpoint);
        }

        public void stop()
        {
            try
            {
                if(poller != null)
                {
                    poller.interrupt();
                    poller.join(1000);
                    poller = null;
                }
            }
            catch(Exception e){}

            if(scheduledFuture != null)
            {
                scheduledFuture.cancel(true);
                scheduledFuture = null;
            }
        }

        @Override
        public void run()
        {
            MainService.this.runSend(endpoint);
        }

        @Override
        public void onReceived(String endpoint, Log.Event.Reader reader)
        {
            MainService.this.onReceived(endpoint, reader);
        }
    }

    private void startTimer()
    {
        handlerTimer = new Handler(Looper.getMainLooper());
        timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                handlerTimer.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if(NavdyBT.instance().getState() == NavdyBT.STATE_NONE || NavdyBT.instance().getState() == NavdyBT.STATE_LISTEN)
                        {
                            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                            if(!adapter.isEnabled())
                                adapter.enable();

                            NavdyBT.instance().connect(null);
                        }
                    }
                });
            }
        }, 1000, 5000);
    }

    private void stopTimer()
    {
        if(timer != null)
            timer.cancel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        service = Executors.newScheduledThreadPool(4);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(!adapter.isEnabled())
            adapter.enable();

        for(Sender sender : senders)
            sender.start();

        startTimer();

        NavdyBT.instance().addStateChangedListener(new BaseBluetooth.OnStateChangedListener()
        {
            @Override
            public void onStateChanged(int state)
            {
                android.util.Log.d(TAG, "onStateChanged: " + state);
                carItem.init();
            }
        });

        NavdyBT.instance().startServer();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        NavdyBT.instance().stopServer();

        stopTimer();

        for(Sender sender : senders)
            sender.stop();

        try
        {
            service.shutdown();
            service.awaitTermination(500, TimeUnit.MILLISECONDS);
        }
        catch(Exception e){}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        checkBT();

        return START_STICKY;
    }

    private void checkBT()
    {
        int state = NavdyBT.instance().getState();

        if(state != NavdyBT.STATE_CONNECTED && state != NavdyBT.STATE_CONNECTING)
        {
            String address = SettingUtil.getString(this, "bt_address_nmirror_pair_device");

            if(TextUtils.isEmpty(address))
            {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            else
            {
                NavdyBT.instance().connect(new BaseBluetooth.OnConnectCompletion()
                {
                    @Override
                    public void onConnectCompletion(int state)
                    {
                        if(state == NavdyBT.STATE_CONNECTED)
                        {
                            Toast.makeText(getApplicationContext(), "Connected !!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }
    }

    private Gson gson = new Gson();
    private final Map<String, String> readerMap = new HashMap<>();

    CarItem carItem = new CarItem();

    private void onReceived(String endpoint, Log.Event.Reader reader)
    {
        String json = "";

        if(reader.hasControlsState() && endpoint.equals("controlsState"))
        {
            Log.ControlsState.Reader r = reader.getControlsState();
            json = carItem.getJson(r);
        }
        else if(reader.hasCarControl() && endpoint.equals("carControl"))
        {
            Car.CarControl.Reader r = reader.getCarControl();
            json = carItem.getJson(r);
        }
        else if(reader.hasPathPlan() && endpoint.equals("pathPlan"))
        {
            Log.PathPlan.Reader r = reader.getPathPlan();
            json = carItem.getJson(r);
        }
        else if(reader.hasRadarState() && endpoint.equals("radarState"))
        {
            Log.RadarState.Reader r = reader.getRadarState();
            json = carItem.getJson(r);
        }
        else if(reader.hasThermal() && endpoint.equals("thermal"))
        {
            Log.ThermalData.Reader r = reader.getThermal();
            json = carItem.getJson(r);
        }
        else if(reader.hasCarState() && endpoint.equals("carState"))
        {
            Car.CarState.Reader r = reader.getCarState();
            json = carItem.getJson(r);

            if(!TextUtils.isEmpty(json))
            {
                Bundle bundle = new Bundle();
                bundle.putString("type", endpoint);
                bundle.putString("json", json);
                NavdyBT.instance().sendBundle(bundle);
            }

            return;
        }

        if(!TextUtils.isEmpty(json))
        {
            synchronized(readerMap)
            {
                readerMap.put(endpoint, json);
            }
        }
    }

    private void runSend(String endpoint)
    {
        String json;
        synchronized(readerMap)
        {
            json = readerMap.get(endpoint);
            if(TextUtils.isEmpty(json))
                return;

            readerMap.remove(endpoint);
        }

        Bundle bundle = new Bundle();
        bundle.putString("type", endpoint);
        bundle.putString("json", json);
        NavdyBT.instance().sendBundle(bundle);
    }

    public static void startService(Context context)
    {
        Intent intent = new Intent(context, MainService.class);
        context.startService(intent);
    }
}

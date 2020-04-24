package com.neokii.openpilot.poller;

import org.capnproto.MessageReader;
import org.capnproto.Serialize;

import java.io.IOException;
import java.nio.ByteBuffer;

import ai.comma.messaging.Context;
import ai.comma.messaging.Message;
import ai.comma.messaging.SubSocket;
import ai.comma.openpilot.cereal.Log;

public class Poller extends Thread
{
    public interface OnReceiveListener
    {
        void onReceived(String endpoint, Log.Event.Reader reader);
    }

    private OnReceiveListener listener;

    private Context msgContext;
    private SubSocket subSocket;
    private String endpoint;

    public Poller(OnReceiveListener listener)
    {
        this.listener = listener;
        msgContext = new Context();
    }

    public void start(String endpoint)
    {
        this.endpoint = endpoint;
        subSocket = msgContext.subSocket(endpoint);
        subSocket.setTimeout(3000);
        start();
    }

    @Override
    public void run()
    {
        try
        {
            while(!interrupted())
            {
                Message msg = subSocket.receive();
                if(msg == null || msg.getSize() < 4)
                    continue;

                ByteBuffer msgbuf = ByteBuffer.wrap(msg.getData());
                MessageReader reader;

                try
                {
                    reader = Serialize.read(msgbuf);
                }
                catch(IOException e) {
                    continue;
                }
                finally {
                    msg.release();
                }

                listener.onReceived(endpoint, reader.getRoot(Log.Event.factory));
            }
        }
        catch(Exception e){}

        try
        {
            subSocket.close();
        }
        catch(Exception e){}
    }

}

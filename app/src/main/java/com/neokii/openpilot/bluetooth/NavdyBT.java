package com.neokii.openpilot.bluetooth;

import android.os.Bundle;

import com.neokii.openpilot.util.ParcelableUtil;

import java.io.DataOutputStream;
import java.util.UUID;

public class NavdyBT extends BaseBluetooth
{
    private static final String TAG = "nMirrorPairBluetooth";

    private static NavdyBT instance = new NavdyBT();
    public static NavdyBT instance()
    {
        return instance;
    }

    private NavdyBT(){}

    @Override
    protected String getName()
    {
        return "OpenPilotBluetooth";
    }

    @Override
    protected UUID getUUID()
    {
        return UUID.fromString("60782b78-9b7a-4b45-a738-2460dc1ddde0");
    }

    @Override
    protected String getPrefrenceKeyConnectedAddress()
    {
        return "bt_address_nmirror_pair_device";
    }

    public void sendBundle(final Bundle bundle)
    {
        sendBundle(bundle, false);
    }

    public void sendBundle(final Bundle bundle, boolean tryConnect)
    {
        if(getState() != STATE_CONNECTED)
        {
            if(tryConnect)
            {
                connect(new OnConnectCompletion()
                {
                    @Override
                    public void onConnectCompletion(int state)
                    {
                        try
                        {
                            write(ParcelableUtil.marshall(bundle));
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
        else
        {
            try
            {
                write(ParcelableUtil.marshall(bundle));
            }
            catch(Exception e){}
        }

    }

    private synchronized void write(byte[] bytes) throws Exception
    {
        DataOutputStream outputStream = new DataOutputStream(getOutputStream());
        outputStream.writeShort(DATA_TYPE_BUNDLE);
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);
    }
}

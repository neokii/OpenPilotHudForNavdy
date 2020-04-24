package com.neokii.openpilot.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

public class nMirrorPairBTDialog extends BaseBluetoothConnectDialog
{
    public nMirrorPairBTDialog(@NonNull Context context, OnChangedListener listener)
    {
        super(context, listener);
    }

    @Override
    protected String getPrefrenceKey()
    {
        return "bt_address_nmirror_pair_device";
    }

    @Override
    protected boolean isAvalable(BluetoothDevice device)
    {
        return !TextUtils.isEmpty(device.getName());
    }
}

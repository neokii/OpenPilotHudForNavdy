package com.neokii.openpilot.bluetooth;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.neokii.openpilot.R;
import com.neokii.openpilot.util.SettingUtil;
import com.neokii.openpilot.util.Util;

import java.util.ArrayList;

public abstract class BaseBluetoothConnectDialog extends Dialog
{
    TextView textMessage;
    ListView listView;

    Button btnRemove;
    Button btnClose;

    ListAdapter adapter;
    ArrayList<BluetoothDevice> items = new ArrayList<>();

    ProgressBar progressBar;

    OnChangedListener listener;

    public interface OnChangedListener
    {
        void onChanged(String key);
    }

    protected abstract String getPrefrenceKey();
    protected abstract boolean isAvalable(BluetoothDevice device);


    public BaseBluetoothConnectDialog(Context context, OnChangedListener listener)
    {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        try
        {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(getWindow().getAttributes());

            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;

            getWindow().setAttributes(layoutParams);
        }
        catch(Exception e){}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getContext().registerReceiver(receiver, filter);

        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter.isDiscovering())
                    adapter.cancelDiscovery();

                getContext().unregisterReceiver(receiver);
            }
        });

        setContentView(R.layout.bt_connect_layout);

        textMessage = findViewById(R.id.textMessage);
        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listView);
        btnRemove = findViewById(R.id.btnRemove);
        btnClose = findViewById(R.id.btnClose);

        btnRemove.setVisibility(View.GONE);

        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                SettingUtil.setString(getContext(), getPrefrenceKey(), "");
                dismiss();

                if(listener != null)
                    listener.onChanged(getPrefrenceKey());
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                dismiss();
            }
        });

        listView.setAdapter(this.adapter = new ListAdapter());

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                try
                {
                    SettingUtil.setString(getContext(), getPrefrenceKey(), items.get(position).getAddress());
                    dismiss();

                    if(listener != null)
                        listener.onChanged(getPrefrenceKey());
                }
                catch (Exception e){}
            }
        });

        doDiscovery();

        if(TextUtils.isEmpty(SettingUtil.getString(getContext(), getPrefrenceKey(), "")))
            btnRemove.setVisibility(View.GONE);
        else
            btnRemove.setVisibility(View.VISIBLE);
    }

    private void doDiscovery()
    {
        progressBar.setVisibility(View.VISIBLE);
        textMessage.setText(R.string.scanning_bluetooth);

        items.clear();

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if(!adapter.isEnabled())
            adapter.enable();
        else
        {
            for(BluetoothDevice device : adapter.getBondedDevices())
            {
                addItem(device);
            }

            this.adapter.notifyDataSetChanged();
        }

        if (adapter.isDiscovering())
            adapter.cancelDiscovery();

        adapter.startDiscovery();
    }

    private void addItem(BluetoothDevice device)
    {
        try
        {
            if(isAvalable(device))
                items.add(device);
        }
        catch (Exception e){}
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    addItem(device);
                    adapter.notifyDataSetChanged();
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                progressBar.setVisibility(View.GONE);

                if(items.size() == 0)
                {
                    textMessage.setText(R.string.not_found_bluetooth);
                }
                else
                {
                    textMessage.setText(R.string.scan_complete_bluetooth);
                }

                adapter.notifyDataSetChanged();
            }
            else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state)
                {
                    case BluetoothAdapter.STATE_ON:
                    {
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        for(BluetoothDevice device : bluetoothAdapter.getBondedDevices())
                        {
                            addItem(device);
                        }

                        adapter.notifyDataSetChanged();
                    }
                    break;
                }
            }
        }
    };

    class ListAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return items.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            TextView textView = (TextView)convertView;
            if(textView == null)
            {
                textView = new TextView(getContext());
                textView.setTextSize(25);
                textView.setGravity(Gravity.CENTER);
                textView.setTypeface(null, Typeface.BOLD);

                int p = Util.DP2PX(getContext(), 15);
                textView.setPadding(0, p, 0, p);
            }

            BluetoothDevice device = items.get(position);
            textView.setText(device.getName());

            return textView;
        }

        @Override
        public Object getItem(int position)
        {
            return null;
        }

        @Override
        public long getItemId(int position)
        {
            return 0;
        }
    }
}

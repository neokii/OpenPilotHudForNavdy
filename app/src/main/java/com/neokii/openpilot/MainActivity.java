package com.neokii.openpilot;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hariofspades.incdeclibrary.IncDecCircular;
import com.neokii.openpilot.bluetooth.BaseBluetooth;
import com.neokii.openpilot.bluetooth.BaseBluetoothConnectDialog;
import com.neokii.openpilot.bluetooth.nMirrorPairBTDialog;
import com.neokii.openpilot.bluetooth.NavdyBT;
import com.neokii.openpilot.poller.Poller;
import com.neokii.openpilot.util.SettingUtil;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import ai.comma.openpilot.cereal.Log;

public class MainActivity extends AppCompatActivity implements BaseBluetooth.OnStateChangedListener
{
    TextView textAddress, textTimer;
    Poller poller;
    Timer timer;
    int timerCount = 20;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            //actionBar.setDisplayShowHomeEnabled(true);
            //actionBar.setDisplayHomeAsUpEnabled(true);

            try
            {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                //actionBar.setTitle(getString(R.string.app_name) + " " + packageInfo.versionCode);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    actionBar.setTitle(getString(R.string.app_name) + " " + packageInfo.versionName + " (" + packageInfo.getLongVersionCode() + ")");
                else
                    actionBar.setTitle(getString(R.string.app_name) + " " + packageInfo.versionName + " (" + packageInfo.versionCode + ")");
            }
            catch(Exception e){}
        }

        textTimer = findViewById(R.id.textTimer);

        textAddress = findViewById(R.id.textAddress);
        textAddress.setText(SettingUtil.getString(this, "bt_address_nmirror_pair_device"));

        View btnPair = findViewById(R.id.btnPair);

        btnPair.setOnClickListener((View v) -> {

            cancelTimer();

            try
            {
                removeBond(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(SettingUtil.getString(this, "bt_address_nmirror_pair_device")));
            }
            catch(Exception e){}


            nMirrorPairBTDialog dialog = new nMirrorPairBTDialog(MainActivity.this, new BaseBluetoothConnectDialog.OnChangedListener()
            {
                @Override
                public void onChanged(String key)
                {
                    textAddress.setText(SettingUtil.getString(getApplicationContext(), key));

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
            });

            dialog.show();

        });

        findViewById(R.id.btnBluetoothSettings).setOnClickListener((View v)->{

            cancelTimer();
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.putExtra("extra_prefs_show_button_bar", true);
            startActivity(intent);
        });

        float speed_ratio = SettingUtil.getFloat(MainApp.getAppContext(), "speed_ratio", 1.0f);

        IncDecCircular incdec = (IncDecCircular) findViewById(R.id.incdec);
        incdec.setConfiguration(LinearLayout.HORIZONTAL, IncDecCircular.TYPE_FLOAT,
                IncDecCircular.DECREMENT, IncDecCircular.INCREMENT);

        incdec.setupValues(0.8f,1.3f, 0.05f, speed_ratio);
        incdec.setprecision("%.2f");
        incdec.enableLongPress(false,false,500);

        incdec.setOnValueChangeListener(new IncDecCircular.OnValueChangeListener()
        {
            @Override
            public void onValueChange(IncDecCircular view, float oldValue, float newValue)
            {
                cancelTimer();
                SettingUtil.setFloat(MainApp.getAppContext(), "speed_ratio", newValue);
            }
        });

        findViewById(R.id.btnFinish).setOnClickListener((View v) -> {
            finish();
        });

        NavdyBT.instance().addStateChangedListener(this);

        /*timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                new Handler(Looper.getMainLooper()).post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        timerCount--;

                        if(timerCount > 0)
                            updateTimerView();
                        else
                            finish();
                    }
                });
            }
        }, 1000, 1000);*/
    }

    private void removeBond(BluetoothDevice device)
    {
        try
        {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        }
        catch (Exception e){}
    }

    private void updateTimerView()
    {
        textTimer.setText(String.format(Locale.getDefault(), "Automatically close after %d seconds.", timerCount));
    }

    private void cancelTimer()
    {
        if(timer != null)
        {
            timer.cancel();
            timer = null;
            textTimer.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        NavdyBT.instance().removeStateChangedListener(this);

        try
        {
            poller.interrupt();
            poller.join();
        }
        catch(Exception e){}

        if(timer != null)
            timer.cancel();

        try
        {
            IncDecCircular incdec = (IncDecCircular) findViewById(R.id.incdec);
            SettingUtil.setFloat(MainApp.getAppContext(), "speed_ratio", Float.valueOf(incdec.getValue()));
        }
        catch(Exception e){}

    }

    @Override
    public void onStateChanged(int state)
    {
        if(state == NavdyBT.STATE_CONNECTED && timer != null)
            finish();
    }
}

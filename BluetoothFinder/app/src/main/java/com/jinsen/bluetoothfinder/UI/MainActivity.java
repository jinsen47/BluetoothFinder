package com.jinsen.bluetoothfinder.UI;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.jinsen.bluetoothfinder.Events.AddressMessage;
import com.jinsen.bluetoothfinder.Events.AlarmMessage;
import com.jinsen.bluetoothfinder.Events.StartupMessage;
import com.jinsen.bluetoothfinder.Events.StatusMessage;
import com.jinsen.bluetoothfinder.Events.TimeMessage;
import com.jinsen.bluetoothfinder.R;
import com.jinsen.bluetoothfinder.Service.BluetoothLeService;

import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;


public class MainActivity extends ActionBarActivity{

    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean D = true;


    // Intent request codes
    public static final int REQUEST_CONNECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;

    // Loop message
    public static final String LOOP_MESSAGE = "BluetoothFinder";

    //Toast Text
    private static final String BT_INVALID = "蓝牙不可用";

    // Setup Paramaters
    private String time = null;
    private String alarm = null;
    public String remoteDevice = null;

    // Layout views
    private ImageButton mSendButton;
    private boolean mSendButtonState = false;
    private ActionBar mTitle = null;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothLeService mBluetoothLeService = null;
    // Player to play the alarm
    private MediaPlayer mPlayer = null;

    // Remote device address
    private String mDeviceAddress = null;

    private static Boolean isQuit = false;
    private Timer mQuitTimer = new Timer();

    private static final Handler mHandler = new Handler();

    private static final long SCAN_INTERVAL_MS = 250;
    private static boolean mScanning = false;



    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        setContentView(R.layout.activity_main);

        //Initiate ActionBar
        mTitle = getActionBar();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // if Bluetooth is available
        if (mBluetoothAdapter == null) {
            showText(BT_INVALID);
            finish();
            return;
        }

        // Register Eventbus subcriber
        EventBus.getDefault().register(this);

        setupFinder();

        FragmentTransaction fmTrans = getFragmentManager().beginTransaction();
        fmTrans.replace(R.id.frame, SetupFragment.newInstance(), SetupFragment.TAG);
        fmTrans.addToBackStack(null);
        fmTrans.commit();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) mPlayer.stop();
        }
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService(mServiceConnection);
        mBluetoothLeService = null;

        // Unregister Eventbus
        EventBus.getDefault().unregister(this);
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }


    private void setupFinder() {
        Log.d(TAG, "setupFinder");

        // Initiate send button
        mSendButton = ((ImageButton) findViewById(R.id.sendButton));
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSendButtonState) {
                    // Running state, change to stop
                    mSendButton.setBackgroundResource(R.drawable.greenbutton);
                    mSendButtonState = false;
                    Log.d(TAG, "StopAlarm");
                    stopAlarm();
                } else {
                    // Stop state, change to running
                    mSendButton.setBackgroundResource(R.drawable.redbutton);
                    mSendButtonState = true;
                    startFinder();
                }

            }
        });

    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isQuit == false) {
                isQuit = true;
                showText("再按一次退出");
                TimerTask task = null;
                task = new TimerTask() {
                    @Override
                    public void run() {
                        isQuit = false;
                    }
                };
                mQuitTimer.schedule(task, 2000);
            } else {
                finish();
            }

        }
        return true;
    }

    private void showText(String string) {
        Toast.makeText(this, string, Toast.LENGTH_LONG).show();
    }

    public void onEvent(AddressMessage message) {
        String address = message.getAddress();
        Log.d(TAG, "onAddressMessage : " + address);
        if (address != null) {
            remoteDevice = address;

            // connect remote device thr mac address
            mBluetoothLeService.connect(remoteDevice);
        }
    }

    public void onEvent(AlarmMessage message) {
        String alarm = message.getAlarm();
        Log.d(TAG, "onAlarmMessage : " + message.getAlarm());
        if (alarm != null) {
            this.alarm = alarm;
        }
    }

    public void onEvent(TimeMessage message) {
        String time = message.getTime() + "";
        Log.d(TAG, "onTimeMessage : " + message.getTime());
        if (time != null) {
            this.time = time;
        }
    }

    public void onEvent(StartupMessage message) {
        String tempAlarm = message.getAlarm();
        String tempDevice = message.getAddress();
        String tempTime = message.getTime() + "";

        if (tempAlarm != null) alarm = tempAlarm;
        if (tempDevice != null) remoteDevice = tempDevice;
        if (tempTime != null) time = tempTime;
    }

    private void startFinder() {
        // Check that we're actually connected before trying anything
        if (mBluetoothLeService.getState() != mBluetoothLeService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
//            showText("还未连接到设备");
            mSendButton.setBackgroundResource(R.drawable.greenbutton);
            mSendButtonState = false;
            return;
        }
    }

    private void playAlarm() {
        Log.d(TAG, "Alarm:" + alarm);
        mPlayer = MediaPlayer.create(this, Uri.parse(alarm));
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mPlayer.start();
            }
        });
        mPlayer.setLooping(true);
    }

    private void stopAlarm() {
        Log.d(TAG, "StopAlarm");
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) mPlayer.stop();
        }
    }

    private void waitFinder() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallBack);
                } else if (!mBluetoothAdapter.startLeScan(mLeScanCallBack)){

                }
                mScanning = !mScanning;
                mHandler.postDelayed(this, SCAN_INTERVAL_MS);
            }
        });
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallBack = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device.getAddress().equals(mDeviceAddress)) {
                Log.d(TAG,"Refound device : " + device.getAddress());
                stopAlarm();

            }
        }
    };

    public void onEventBackgroundThread(StatusMessage message) {
        int state = message.getState();
        if (state == BluetoothLeService.STATE_CONNECTED) {
            stopAlarm();
        }
        if (state == BluetoothLeService.STATE_DISCONNECTED) {
            playAlarm();
        }
    }

}

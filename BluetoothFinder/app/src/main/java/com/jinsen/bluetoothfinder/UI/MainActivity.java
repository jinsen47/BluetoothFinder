package com.jinsen.bluetoothfinder.UI;

import android.app.ActionBar;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.jinsen.bluetoothfinder.Service.BluetoothChatService;
import com.jinsen.bluetoothfinder.R;
import com.jinsen.bluetoothfinder.Service.Counter;

import java.util.Set;


public class MainActivity extends ActionBarActivity implements SetupFragment.OnFragmentInteractionListener{

    // Debugging
    private static final String TAG = "MainActivity";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Message types sent from the Counter
//    public static final int COUNTER_TIMEOUT = 6;
//    public static final int MESSAGE_

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

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

    // Layout views
    private ImageButton mSendButton;
    private ActionBar mTitle = null;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    // Member object for the counter
    private Counter mCounter = null;

    private MediaPlayer mPlayer = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        setContentView(R.layout.activity_main);

        //Initiate ActionBar
        mTitle = getActionBar();

        // Request Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Intiate myHandler
//        mHandler = new Handler();

        if (mBluetoothAdapter == null) {
            showText(BT_INVALID);
            finish();
            return;
        }

        FragmentTransaction fmTrans = getFragmentManager().beginTransaction();
        fmTrans.replace(R.id.frame, SetupFragment.newInstance(), SetupFragment.TAG);
        fmTrans.addToBackStack(null);
        fmTrans.commit();

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
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupFinder();
            if (mCounter == null) setupCounter();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
        // In case there is a counter clicking
        if (mCounter != null) {
            if (mCounter.state != Counter.State.NONE) mCounter.state = Counter.State.NONE;
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
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if (mCounter != null) mCounter.state = Counter.State.NONE;
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }


    private void setupFinder() {
        Log.d(TAG, "setupFinder");

        // Initiate send button
        mSendButton = ((ImageButton) findViewById(R.id.sendButton));
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFinder();

                //Button pressed animation
//                mSendButton.set
            }
        });
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
//        mOutStringBuffer = new StringBuffer("");
    }

    private void setupCounter() {
        Log.d(TAG, "setupCounter");
        mCounter = new Counter(this, mHandler);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
//                            mTitle.setTitle("连接至");
//                            mTitle.setSubtitle(mConnectedDeviceName);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
//                            mTitle.setTitle("正在连接");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
//                            mTitle.setTitle("未连接");
                            break;
                        case Counter.COUNTER_TIME_OUT:
                            playAlarm();
                            if (D) showText("计时器超时！");
                            Log.d(TAG,"Counter timeout");
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    if (D) showText("writeMessage: " + writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    if (D) showText("readMessage " + readMessage);
                    mCounter.setState(Counter.State.NONE);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };



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

    private void showText(String string) {
        Toast.makeText(this, string, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onItemChanged(Bundle bundle) {
        String tempAlarm = bundle.getString(SetupFragment.KEY_ALARM);
        String tempDevice = bundle.getString(SetupFragment.KEY_DEVICE);
        String tempTime = bundle.getString(SetupFragment.KEY_TIME);

        if (tempAlarm != null) alarm = tempAlarm;
        if (tempDevice != null) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(tempDevice);
            mChatService.connect(device);
        }
        if (tempTime != null) time = tempTime;
    }

    private void startFinder() {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
//            showText("还未连接到设备");
            return;
        }
        // Get the message bytes and tell the BluetoothChatService to write
        byte[] send = LOOP_MESSAGE.getBytes();
        mChatService.write(send);
        if (time != null) {
            mCounter.start(Integer.valueOf(time));
        } else {
            showText("请选择报警时间");
        }
        mSendButton.setClickable(false);

    }

    private void playAlarm() {
        Log.d(TAG, "Alarm:" + alarm);
        mPlayer = MediaPlayer.create(this, Uri.parse(alarm));
        mPlayer.start();
    }
}

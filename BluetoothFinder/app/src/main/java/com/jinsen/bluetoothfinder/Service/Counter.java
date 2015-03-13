package com.jinsen.bluetoothfinder.Service;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.jinsen.bluetoothfinder.UI.MainActivity;

/**
 * Created by Jinsen47 on 2015/3/13.
 */
public class Counter {

    // TAG
    public static final String TAG = "Counter";
    // Message protocal
    public static final int COUNTER_TIME_OUT = 10;
    public static final int COUNTER_TIME_RESET = 11;

    //State
    public enum State {NONE,COUNTING}
    public State state;

    // Private data segment
    private final Handler mHandler;

    public Counter(Context context, Handler mHandler) {
        this.mHandler = mHandler;
        setState(State.NONE);
    }

    public void start(int time) {
        setState(State.COUNTING);
        CounterThread mCounterThread = new CounterThread(time);
        mCounterThread.start();
    }

    public synchronized void setState(State state) {
        this.state = state;
    }
    private class CounterThread extends Thread {
        private final int time;
        public CounterThread(int time) {
            this.time = time;
        }

        @Override
        public void run() {
            Log.d(TAG, "Begin counter");
            try {
                sleep(time * 1000);
                Log.d(TAG, "CounterState: " + state.toString());
                if (state != Counter.State.NONE) {
                    mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, COUNTER_TIME_OUT, -1).sendToTarget();
                }
                setState(Counter.State.NONE);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

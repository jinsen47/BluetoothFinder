package com.jinsen.bluetoothfinder.Events;

/**
 * Created by Jinsen on 15/4/28.
 */
public class TimeMessage {
    private int time;

    public TimeMessage(Integer time) {
        this.time = time.intValue();
    }

    public int getTime() {
        return time;
    }
}

package com.jinsen.bluetoothfinder.Events;

/**
 * Created by Jinsen on 15/4/28.
 */
public class StartupMessage {
    private int time;
    private String alarm;
    private String address;

    public StartupMessage(String alarm, int time, String address) {
        this.alarm = alarm;
        this.time = time;
        this.address = address;
    }

    public int getTime() {
        return time;
    }

    public String getAlarm() {
        return alarm;
    }

    public String getAddress() {
        return address;
    }
}

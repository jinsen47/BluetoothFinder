package com.jinsen.bluetoothfinder.Events;

/**
 * Created by Jinsen on 15/4/28.
 *
 * This is a message used for bluetooth gatt connection state change.
 */
public class StatusMessage {
    private int state;

    public StatusMessage(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}

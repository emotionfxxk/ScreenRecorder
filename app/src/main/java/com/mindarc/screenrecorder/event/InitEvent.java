package com.mindarc.screenrecorder.event;

/**
 * Created by sean on 7/10/15.
 */
public class InitEvent {
    public InitEvent(int error_id) {
        this.error_id = error_id;
    }
    public final int error_id;
}

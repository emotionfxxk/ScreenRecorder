package com.mindarc.screenrecorder.event;

/**
 * Created by sean on 7/10/15.
 */
public class RecorderEvent {
    public RecorderEvent(boolean isRecording, String fileName) {
        this.isRecording = isRecording;
        this.fileName = fileName;
    }
    public final boolean isRecording;
    public final String fileName;
}

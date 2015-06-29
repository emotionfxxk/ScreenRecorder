package com.mindarc.screenrecorder.core;

/**
 * Created by sean on 6/29/15.
 */
public class ScreenRecorder {
    public static native boolean init(int width, int height,
                                  int bitrate, int timeLimit, boolean rotate, String destFile);
    public static native boolean start();
    public static native boolean stop();
}

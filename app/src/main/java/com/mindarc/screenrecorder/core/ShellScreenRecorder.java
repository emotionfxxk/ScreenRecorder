package com.mindarc.screenrecorder.core;

import android.os.Process;

import com.mindarc.screenrecorder.utils.LogUtil;
import com.mindarc.screenrecorder.utils.Shell;

/**
 * Created by sean on 7/6/15.
 */
public class ShellScreenRecorder {
    private final static String MUDULE_NAME = "ShellScreenRecorder";
    //private final static String SYS_RECORD_COMMAND = "/system/bin/screenrecord_limit30";
    private final static String SYS_RECORD_COMMAND = "/system/bin/screenrecord";

    private ShellScreenRecorder() {}
    private static volatile boolean sIsRecording = false;
    private final static Object sStateLock = new Object();
    public static boolean isRecording() {
        synchronized (sStateLock) {
            return sIsRecording;
        }
    }
    public static boolean start(String fileName, int width, int height,
                                int bitRate, int timeLimit, boolean rotate) {
        LogUtil.i(MUDULE_NAME, "try start with file name: " + fileName);
        boolean isRecording;
        synchronized (sStateLock) {
            isRecording = sIsRecording;
            if(!sIsRecording) {
                sIsRecording = true;
            }
        }
        if(isRecording) {
            LogUtil.i(MUDULE_NAME, "record already running!" + fileName);
            return false;
        } else {
            new RecorderThread(fileName, width, height, bitRate, timeLimit, rotate).start();
            return true;
        }
    }


    public static void stop() {
        if(!isRecording()) {
            LogUtil.i(MUDULE_NAME, "stop: recorder is not running");
            return;
        }
        Shell.Result res = Shell.execCommand("ps | grep " + SYS_RECORD_COMMAND);
        LogUtil.i(MUDULE_NAME, "res:" + res.errorMsg + "|" + res.succeedMsg + "|" + res.result);
        if(res.succeedMsg != null && !res.succeedMsg.equals("")) {
            String[] processInfo = res.succeedMsg.split("\\s+");
            for(String info : processInfo) {
                LogUtil.i(MUDULE_NAME, "info: [" + info + "]");
            }
            if(processInfo.length >= 2) {
                LogUtil.i(MUDULE_NAME, "pid: [" + processInfo[1] + "]");
                res = Shell.execCommand("kill -2 " + processInfo[1]);
                LogUtil.i(MUDULE_NAME, "res:" + res.result);
            }
        }
    }

    private static class RecorderThread extends Thread {
        private final static int FALLBACK_WIDTH = 1280;
        private final static int FALLBACK_HEIGHT = 720;
        private final static int FALLBACK_BITRATE = 4000000;    // 4M
        private final static int MAX_TIME_LIMIT = 1800;         // 30 minutes
        private final String mFileName;
        private int mWidth, mHeight, mBitrate, mTimelimit;
        private boolean mRotate;
        public RecorderThread(String fileName, int width, int height,
                              int bitRate, int timeLimit, boolean rotate) {
            mFileName = fileName;
            mWidth = (width <= 0 || height <= 0) ? FALLBACK_WIDTH : width;
            mHeight = (width <= 0 || height <= 0) ? FALLBACK_HEIGHT : height;

            mBitrate = (bitRate > 0) ? bitRate : FALLBACK_BITRATE;
            mTimelimit = (timeLimit <= 0 || timeLimit > MAX_TIME_LIMIT) ? MAX_TIME_LIMIT : timeLimit;
            mRotate = rotate;
        }
        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            LogUtil.i(MUDULE_NAME, "entering record thread...");
            StringBuilder commandLine = new StringBuilder();
            commandLine.append(SYS_RECORD_COMMAND);
            commandLine.append(" --size ").append(mWidth)
                    .append("x").append(mHeight);
            commandLine.append(" --bit-rate ").append(mBitrate);
            commandLine.append(" --time-limit ").append(mTimelimit);
            if(mRotate) {
                commandLine.append(" --rotate");
            }
            commandLine.append(" ").append(mFileName);
            LogUtil.i(MUDULE_NAME, "command:" + commandLine.toString());
            Shell.execCommand(commandLine.toString());

            synchronized (sStateLock) {
                sIsRecording = false;
            }
            LogUtil.i(MUDULE_NAME, "leaving record thread...");
        }
    }
}

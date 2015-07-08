package com.mindarc.screenrecorder.core;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Process;

import com.mindarc.screenrecorder.utils.AssetsHelper;
import com.mindarc.screenrecorder.utils.LogUtil;
import com.mindarc.screenrecorder.utils.Shell;

import java.io.File;

/**
 * Created by sean on 7/6/15.
 */
public class ShellScreenRecorder {
    private final static String MUDULE_NAME = "ShellScreenRecorder";
    private static String FULL_PATH_OF_RECORD_COMMAND;

    private final static String BINARY_SUB_PATH = "bin";
    private final static String BINARY_ASSET_SUB_PATH = "armebi";
    private final static String BINARY_NAME = "screenrecord_limit30";

    private ShellScreenRecorder() {}

    private enum State {
        UNINITIALIZED,
        FREE,
        RECORDING,
    }
    private final static Object sStateLock = new Object();
    private static State sState = State.UNINITIALIZED;

    private static StateListener sStateListener;

    private static Context sAppContext;

    private static Handler sHandler;

    private final static class _MESSAGE {
        public final static int ON_INITIALIZED = 0;
        public final static int ON_START_RECORDER = 1;
        public final static int ON_STOP_RECORDER = 2;
    }

    private static Handler.Callback sMessageDispatcher = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            StateListener l;
            synchronized (sStateLock) {
                l = sStateListener;
            }
            if(l == null) {
                LogUtil.w(MUDULE_NAME, "?????? handleMessage  sStateListener be null");
                return false;
            }
            switch (msg.what) {
                case _MESSAGE.ON_INITIALIZED:
                    l.onInitialized();
                    break;
                case _MESSAGE.ON_START_RECORDER:
                    l.onStartRecorder((String)msg.obj);
                    break;
                case _MESSAGE.ON_STOP_RECORDER:
                    l.onStopRecorder((String) msg.obj);
                    break;
            }
            return true;
        }
    };

    public interface StateListener {
        void onInitialized();
        void onStartRecorder(String fileName);
        void onStopRecorder(String fileName);
    }

    public static void setsStateListener(StateListener l) {
        synchronized (sStateLock) {
            sStateListener = l;
        }
    }

    public static boolean isRecording() {
        synchronized (sStateLock) {
            return sState == State.RECORDING;
        }
    }

    public static boolean isIsInitialized() {
        synchronized (sStateLock) {
            return sState != State.UNINITIALIZED;
        }
    }

    private static void updateState(final State s, final String fileName) {
        State old;
        synchronized (sStateLock) {
            old = sState;
            sState = s;
        }

        if (old == State.UNINITIALIZED && s == State.FREE) {
            sHandler.sendEmptyMessage(_MESSAGE.ON_INITIALIZED);
        } else if (old == State.FREE && s == State.RECORDING) {
            sHandler.sendMessage(sHandler.obtainMessage(_MESSAGE.ON_START_RECORDER, fileName));
        } else if (old == State.RECORDING && s == State.FREE) {
            sHandler.sendMessage(sHandler.obtainMessage(_MESSAGE.ON_STOP_RECORDER, fileName));
        }
    }

    public static boolean init(Context ctx) {
        if (isIsInitialized()) {
            return true;
        }
        sAppContext = ctx.getApplicationContext();
        sHandler = new Handler(sAppContext.getMainLooper(), sMessageDispatcher);
        new InitThread().start();
        return false;
    }

    public static boolean start(String fileName, int width, int height,
                                int bitRate, int timeLimit, boolean rotate) {
        LogUtil.i(MUDULE_NAME, "try start with file name: " + fileName);
        if (!isIsInitialized()) {
            throw new IllegalStateException("not initialized! sState:" + sState);
        }
        synchronized (sStateLock) {
            if (sState == State.FREE) {
                updateState(State.RECORDING, fileName);
                new RecorderThread(fileName, width, height, bitRate, timeLimit, rotate).start();
                return true;
            } else {
                LogUtil.i(MUDULE_NAME, "failed to start record on state!" + sState);
                return false;
            }
        }
    }

    public static void stop() {
        if (!isIsInitialized()) {
            throw new IllegalStateException("not initialized! sState:" + sState);
        }
        if (!isRecording()) {
            LogUtil.i(MUDULE_NAME, "stop: recorder is not running");
            return;
        }
        Shell.Result res = Shell.execCommand("ps | grep " + FULL_PATH_OF_RECORD_COMMAND);
        LogUtil.i(MUDULE_NAME, "res:" + res.errorMsg + "|" + res.succeedMsg + "|" + res.result);
        if (res.succeedMsg != null && !res.succeedMsg.equals("")) {
            String[] processInfo = res.succeedMsg.split("\\s+");
            for (String info : processInfo) {
                LogUtil.i(MUDULE_NAME, "info: [" + info + "]");
            }
            if (processInfo.length >= 2) {
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
            commandLine.append(FULL_PATH_OF_RECORD_COMMAND);
            commandLine.append(" --size ").append(mWidth)
                    .append("x").append(mHeight);
            commandLine.append(" --bit-rate ").append(mBitrate);
            commandLine.append(" --time-limit ").append(mTimelimit);
            if (mRotate) {
                commandLine.append(" --rotate");
            }
            commandLine.append(" ").append(mFileName);
            LogUtil.i(MUDULE_NAME, "command:" + commandLine.toString());
            Shell.execCommand(commandLine.toString());
            updateState(ShellScreenRecorder.State.FREE, mFileName);
            LogUtil.i(MUDULE_NAME, "leaving record thread...");
        }
    }

    private static class InitThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            LogUtil.i(MUDULE_NAME, "entering init thread...");
            FULL_PATH_OF_RECORD_COMMAND = sAppContext.getApplicationInfo().dataDir + "/" +
                    BINARY_SUB_PATH + "/" + BINARY_NAME;
            File binaryFile = new File(FULL_PATH_OF_RECORD_COMMAND);
            boolean exist = binaryFile.exists();
            LogUtil.i(MUDULE_NAME, "binaryFullPath:" + FULL_PATH_OF_RECORD_COMMAND + ", exist:" + exist);
            if (!exist) {
                // copy file from assets
                AssetsHelper.cloneFile(sAppContext, BINARY_ASSET_SUB_PATH + "/" + BINARY_NAME,
                        FULL_PATH_OF_RECORD_COMMAND);
            }

            // add execution permission to binary
            String commandLine = "chmod 777 " + FULL_PATH_OF_RECORD_COMMAND;
            Shell.execCommand(commandLine);

            updateState(ShellScreenRecorder.State.FREE, null);
            LogUtil.i(MUDULE_NAME, "leaving init thread...");
        }
    }
}

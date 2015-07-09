package com.mindarc.screenrecorder.core;

import android.content.Context;
import android.os.Build;
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
    //private final static String BINARY_ASSET_SUB_PATH = "armeabi";
    private final static String BINARY_NAME = "screenrecord_limit30";

    private final static String[] SUPPORT_ABI_TYPES = {"armeabi-v7a", "armeabi"};

    private ShellScreenRecorder() {}

    private enum State {
        UNINITIALIZED,
        FREE,
        RECORDING,
        FAILED_TO_INIT,
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
        public final static int ON_FAILED_TO_INIT = 3;
    }

    public final static class ErrorId {
        public final static int NO_ERROR = 0;
        public final static int NOT_ROOTED = 1;
        public final static int ABI_NOT_SUPPORTED = 2;
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
                case _MESSAGE.ON_FAILED_TO_INIT:
                    l.onFailedToInit(msg.arg1);
                    break;
            }
            return true;
        }
    };

    public interface StateListener {
        void onInitialized();
        void onStartRecorder(String fileName);
        void onStopRecorder(String fileName);
        void onFailedToInit(int reason);
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

    private static void updateState(final State s, final String message, final int errId) {
        State old;
        synchronized (sStateLock) {
            old = sState;
            sState = s;
        }

        if (old == State.UNINITIALIZED && s == State.FREE) {
            sHandler.sendEmptyMessage(_MESSAGE.ON_INITIALIZED);
        } else if (old == State.FREE && s == State.RECORDING) {
            sHandler.sendMessage(sHandler.obtainMessage(_MESSAGE.ON_START_RECORDER, message));
        } else if (old == State.RECORDING && s == State.FREE) {
            sHandler.sendMessage(sHandler.obtainMessage(_MESSAGE.ON_STOP_RECORDER, message));
        } else if (s == State.FAILED_TO_INIT) {
            sHandler.sendEmptyMessage(_MESSAGE.ON_FAILED_TO_INIT);
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
                updateState(State.RECORDING, fileName, ErrorId.NO_ERROR);
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
        Shell.Result res = Shell.execCommandAsSu("ps | grep " + FULL_PATH_OF_RECORD_COMMAND);
        LogUtil.i(MUDULE_NAME, "res:" + res.errorMsg + "|" + res.succeedMsg + "|" + res.result);
        if (res.succeedMsg != null && !res.succeedMsg.equals("")) {
            String[] processInfo = res.succeedMsg.split("\\s+");
            for (String info : processInfo) {
                LogUtil.i(MUDULE_NAME, "info: [" + info + "]");
            }
            if (processInfo.length >= 2) {
                LogUtil.i(MUDULE_NAME, "pid: [" + processInfo[1] + "]");
                res = Shell.execCommandAsSu("kill -2 " + processInfo[1]);
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
            Shell.execCommandAsSu(commandLine.toString());
            updateState(ShellScreenRecorder.State.FREE, mFileName, ErrorId.NO_ERROR);
            LogUtil.i(MUDULE_NAME, "leaving record thread...");
        }
    }

    private static class InitThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            LogUtil.i(MUDULE_NAME, "entering init thread...");
            LogUtil.i(MUDULE_NAME, "Build.CPU_ABI:" + Build.CPU_ABI +
                    ", CPU_ABI2:" + Build.CPU_ABI2);
            String chooseAbiType = null;
            for (String supportAbi : SUPPORT_ABI_TYPES) {
                if(supportAbi.equals(Build.CPU_ABI) || supportAbi.equals(Build.CPU_ABI2)) {
                    chooseAbiType = supportAbi;
                    break;
                }
            }
            if(chooseAbiType != null) {
                // then we check root first
                boolean rooted = Shell.requestRootPermission();
                LogUtil.i(MUDULE_NAME, "rooted:" + rooted);
                if(!rooted) {
                    updateState(ShellScreenRecorder.State.FAILED_TO_INIT, null, ErrorId.NOT_ROOTED);
                } else {
                    FULL_PATH_OF_RECORD_COMMAND = sAppContext.getApplicationInfo().dataDir + "/" +
                            BINARY_SUB_PATH + "/" + BINARY_NAME;
                    File binaryFile = new File(FULL_PATH_OF_RECORD_COMMAND);
                    boolean exist = binaryFile.exists();
                    LogUtil.i(MUDULE_NAME, "binaryFullPath:" + FULL_PATH_OF_RECORD_COMMAND + ", exist:" + exist);
                    if (!exist) {
                        // copy file from assets
                        AssetsHelper.cloneFile(sAppContext, chooseAbiType + "/" + BINARY_NAME,
                                FULL_PATH_OF_RECORD_COMMAND);
                    }

                    // add execution permission to binary
                    String commandLine = "chmod 777 " + FULL_PATH_OF_RECORD_COMMAND;
                    Shell.execCommandAsSu(commandLine);

                    updateState(ShellScreenRecorder.State.FREE, null, ErrorId.NO_ERROR);
                }
            } else {
                updateState(ShellScreenRecorder.State.FAILED_TO_INIT, null, ErrorId.ABI_NOT_SUPPORTED);
                LogUtil.w(MUDULE_NAME, "abi type not support!!!...");
            }
            LogUtil.i(MUDULE_NAME, "leaving init thread...");
        }
    }
}

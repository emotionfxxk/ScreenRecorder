package com.mindarc.screenrecorder;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.mindarc.screenrecorder.core.ShellScreenRecorder;
import com.mindarc.screenrecorder.utils.LogUtil;

import java.lang.ref.WeakReference;

/**
 * Created by sean on 7/9/15.
 */
public class RecorderService extends Service implements ShellScreenRecorder.StateListener {
    private final static String MODULE_TAG = "RecorderService";
    public class LocalBinder extends Binder {
        RecorderService getService() {
            return RecorderService.this;
        }
    }
    private WeakReference<ShellScreenRecorder.StateListener> mRecorderListener;
    private final IBinder mBinder = new LocalBinder();

    public void prepare() {
        ShellScreenRecorder.init(this);
    }

    public void startRecorder(String fileName, int width, int height,
        int bitRate, int timeLimit, boolean rotate) {
        ShellScreenRecorder.start(fileName, width, height, bitRate, timeLimit, rotate);
    }

    public void stopRecorder() {
        ShellScreenRecorder.stop();
    }

    public boolean isRecording() {
        return ShellScreenRecorder.isRecording();
    }

    public void setRecorderListener(ShellScreenRecorder.StateListener listener) {
        mRecorderListener = new WeakReference<ShellScreenRecorder.StateListener>(listener);
    }

    @Override
    public void onInitialized() {
        LogUtil.i(MODULE_TAG, "onInitialized");
        ShellScreenRecorder.StateListener ls = mRecorderListener.get();
        if (ls != null) {
            ls.onInitialized();
        }
    }

    @Override
    public void onStartRecorder(String fileName) {
        LogUtil.i(MODULE_TAG, "onStartRecorder");
        ShellScreenRecorder.StateListener ls = mRecorderListener.get();
        if (ls != null) {
            ls.onStartRecorder(fileName);
        }
    }

    @Override
    public void onStopRecorder(String fileName) {
        LogUtil.i(MODULE_TAG, "onStopRecorder");
        ShellScreenRecorder.StateListener ls = mRecorderListener.get();
        if (ls != null) {
            ls.onStopRecorder(fileName);
        }
    }

    @Override
    public void onAbiNotSupported() {
        LogUtil.i(MODULE_TAG, "onAbiNotSupported");
        ShellScreenRecorder.StateListener ls = mRecorderListener.get();
        if (ls != null) {
            ls.onAbiNotSupported();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.i(MODULE_TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
}

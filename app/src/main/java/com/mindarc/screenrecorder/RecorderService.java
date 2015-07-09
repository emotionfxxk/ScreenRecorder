package com.mindarc.screenrecorder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.mindarc.screenrecorder.core.ShellScreenRecorder;
import com.mindarc.screenrecorder.utils.LogUtil;

/**
 * Created by sean on 7/9/15.
 */
public class RecorderService extends Service implements ShellScreenRecorder.StateListener {
    private final static String MODULE_TAG = "RecorderService";

    @Override
    public void onInitialized() {
        LogUtil.i(MODULE_TAG, "onInitialized");
        Intent bcIntent = new Intent();
        bcIntent.setAction(Constants.Action.ON_REC_STATE_CHANGED);
        bcIntent.putExtra(Constants.Key.OLD_STATE, Constants.State.UNINITIALIZED);
        bcIntent.putExtra(Constants.Key.STATE, Constants.State.FREE);
        sendBroadcast(bcIntent);
    }

    @Override
    public void onStartRecorder(String fileName) {
        LogUtil.i(MODULE_TAG, "onStartRecorder");
        Intent bcIntent = new Intent();
        bcIntent.setAction(Constants.Action.ON_REC_STATE_CHANGED);
        bcIntent.putExtra(Constants.Key.OLD_STATE, Constants.State.FREE);
        bcIntent.putExtra(Constants.Key.STATE, Constants.State.RECORDING);
        bcIntent.putExtra(Constants.Key.FILE_NAME, fileName);
        sendBroadcast(bcIntent);
    }

    @Override
    public void onStopRecorder(String fileName) {
        LogUtil.i(MODULE_TAG, "onStopRecorder");
        Intent bcIntent = new Intent();
        bcIntent.setAction(Constants.Action.ON_REC_STATE_CHANGED);
        bcIntent.putExtra(Constants.Key.OLD_STATE, Constants.State.RECORDING);
        bcIntent.putExtra(Constants.Key.STATE, Constants.State.FREE);
        bcIntent.putExtra(Constants.Key.FILE_NAME, fileName);
        sendBroadcast(bcIntent);
    }

    @Override
    public void onFailedToInit(int reason) {
        LogUtil.i(MODULE_TAG, "onFailedToInit reason:" + reason);
        Intent bcIntent = new Intent();
        bcIntent.setAction(Constants.Action.ON_REC_STATE_CHANGED);
        bcIntent.putExtra(Constants.Key.OLD_STATE, Constants.State.UNINITIALIZED);
        bcIntent.putExtra(Constants.Key.STATE, Constants.State.FAILED_TO_INIT);
        bcIntent.putExtra(Constants.Key.ERROR_ID, reason);
        sendBroadcast(bcIntent);
    }

    @Override
    public void onCreate() {
        LogUtil.i(MODULE_TAG, "onCreate");
        super.onCreate();

        // set listener
        ShellScreenRecorder.setsStateListener(this);
    }

    @Override
    public void onDestroy() {
        LogUtil.i(MODULE_TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        LogUtil.i(MODULE_TAG, "Received start id " + startId + ", action:" + action);

        if(action != null) {
            if (action.equals(Constants.Action.INIT)) {
                ShellScreenRecorder.init(this);
            }
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
}

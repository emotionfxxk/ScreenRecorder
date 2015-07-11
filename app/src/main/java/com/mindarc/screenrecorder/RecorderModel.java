package com.mindarc.screenrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.mindarc.screenrecorder.event.InitEvent;
import com.mindarc.screenrecorder.event.RecorderEvent;
import com.mindarc.screenrecorder.utils.LogUtil;

import de.greenrobot.event.EventBus;

/**
 * Created by sean on 7/10/15.
 */
public class RecorderModel {
    private final static String MODULE_NAME = "RecorderModel";
    private RecorderModel() {}
    private static RecorderModel sModel;
    private Context mAppCtx;
    private int mRecorderState = Constants.State.UNINITIALIZED;
    public static RecorderModel getModel() {
        if (sModel == null) {
            sModel = new RecorderModel();
        }
        return sModel;
    }

    private BroadcastReceiver mBc = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.i(MODULE_NAME, "onReceive: " + action);
            if (action != null && action.equals(Constants.Action.ON_REC_STATE_CHANGED)) {
                onStateChanged(intent);
            }
        }
    };

    public void init(Context ctx) {
        if(mAppCtx == null) {
            mAppCtx = ctx.getApplicationContext();
            registerRecorderBr();
        }
    }

    public boolean isRecorderRunning() {
        return mRecorderState ==  Constants.State.RECORDING;
    }

    public boolean isInitialized() {
        return mRecorderState != Constants.State.UNINITIALIZED &&
            mRecorderState != Constants.State.FAILED_TO_INIT;
    }

    private void registerRecorderBr() {
        if (mAppCtx != null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Constants.Action.ON_REC_STATE_CHANGED);
            mAppCtx.registerReceiver(mBc, intentFilter);
        }
    }

    private void onStateChanged(Intent intent) {
        int state = intent.getIntExtra(Constants.Key.STATE, Constants.State.UNINITIALIZED);
        int oldState = intent.getIntExtra(Constants.Key.OLD_STATE, Constants.State.UNINITIALIZED);
        int errorId = intent.getIntExtra(Constants.Key.ERROR_ID, Constants.ErrorId.NO_ERROR);
        String fileName = intent.getStringExtra(Constants.Key.FILE_NAME);
        LogUtil.i(MODULE_NAME, "onStateChanged state: " + state + ", oldState:" + oldState +
                ", errorId:" + errorId + ", fileName:" + fileName);

        mRecorderState = state;
        if (oldState == Constants.State.UNINITIALIZED &&
                state == Constants.State.FREE) {
            EventBus.getDefault().post(new InitEvent(Constants.ErrorId.NO_ERROR));
        } else if (oldState == Constants.State.FREE &&
                state == Constants.State.RECORDING) {
            EventBus.getDefault().post(new RecorderEvent(true, fileName));
        } else if (oldState == Constants.State.RECORDING &&
                state == Constants.State.FREE) {
            EventBus.getDefault().post(new RecorderEvent(false, fileName));
        } else if (oldState == Constants.State.UNINITIALIZED &&
                state == Constants.State.FAILED_TO_INIT) {
            EventBus.getDefault().post(new InitEvent(errorId));
        }
    }
}

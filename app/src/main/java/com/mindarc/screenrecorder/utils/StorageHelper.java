package com.mindarc.screenrecorder.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by sean on 7/8/15.
 *
 */
public class StorageHelper {
    private final static String MUDULE_TAG = "StorageHelper";
    private final static String FOLDER_NAME = "ScreenRecorder";
    private boolean mIsInit = false;
    private boolean mStorageAvailable = false;
    private String mFolderPath;
    public final static StorageHelper sStorageHelper = new StorageHelper();
    private StorageHelper() {}
    private BroadcastReceiver mExternalStorageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.i(MUDULE_TAG, "Storage: " + intent.getData());
            updateStorageState();
        }
    };
    public final synchronized void init(Context ctx) {
        if(!mIsInit) {
            updateStorageState();

            // register BC
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            filter.addAction(Intent.ACTION_MEDIA_REMOVED);
            ctx.getApplicationContext().registerReceiver(mExternalStorageReceiver, filter);
            mIsInit = true;
        }
    }

    public final synchronized boolean isAvailable() {
        return mIsInit && mStorageAvailable;
    }

    public final synchronized void deInit(Context ctx) {
        LogUtil.i(MUDULE_TAG, "deinit!!");
        mIsInit = false;
        try {
            ctx.unregisterReceiver(mExternalStorageReceiver);
        } catch (Exception e) {
        }
    }

    public final synchronized String generateFileName() {
        if(!mIsInit || !mStorageAvailable)
            throw new IllegalStateException("Not initialized! mStorageAvailable:" + mStorageAvailable);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date();
        String fileName = mFolderPath + "/" + fmt.format(date);
        String tempName = fileName;
        int index = 0;
        while (true) {
            File file = new File(tempName + ".mp4");
            if(file.exists()) {
                tempName = fileName + "_" + (++index);
            } else {
                break;
            }
        }
        fileName = tempName + ".mp4";
        LogUtil.i(MUDULE_TAG, "fileName:" + fileName);
        return fileName;
    }

    private synchronized void updateStorageState() {
        String state = Environment.getExternalStorageState();
        LogUtil.i(MUDULE_TAG, "updateStorageState state:" + state);
        if(state.equals(Environment.MEDIA_MOUNTED)) {
            mStorageAvailable = true;
            mFolderPath = Environment.getExternalStorageDirectory().getPath() + "/" + FOLDER_NAME;
            LogUtil.i(MUDULE_TAG, "init mFolderPath:" + mFolderPath);
            File folder = new File(mFolderPath);
            if(!folder.exists()) folder.mkdirs();
        } else {
            // not available
            mStorageAvailable = false;
        }
    }
}

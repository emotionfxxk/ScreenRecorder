package com.mindarc.screenrecorder.utils;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by sean on 7/8/15.
 *
 */
public class StorageHelper {
    private final static String MODULE_TAG = "StorageHelper";
    private final static String FOLDER_NAME = "AppRecorder";
    private boolean mIsInit = false;
    private boolean mStorageAvailable = false;
    private String mFolderPath;
    private Cursor mCursor;
    private Context mAppCtx;
    public final static StorageHelper sStorageHelper = new StorageHelper();
    private StorageHelper() {}
    private BroadcastReceiver mExternalStorageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.i(MODULE_TAG, "Storage: " + intent.getData());
            updateStorageState();
        }
    };
    public final synchronized void init(Context ctx) {
        if(!mIsInit) {
            updateStorageState();
            mAppCtx = ctx.getApplicationContext();
            // register BC
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            filter.addAction(Intent.ACTION_MEDIA_REMOVED);
            mAppCtx.registerReceiver(mExternalStorageReceiver, filter);
            mIsInit = true;

            Cursor cursor = queryVideoClips();
            if (cursor != null) {
                mCursor = cursor;
            } else {
                // TODO:
            }
        }
    }

    public Cursor getVideoClips() {
        return mCursor;
    }

    public final synchronized boolean isAvailable() {
        return mIsInit && mStorageAvailable;
    }

    public final synchronized void deInit(Context ctx) {
        LogUtil.i(MODULE_TAG, "deinit!!");
        mIsInit = false;
        try {
            ctx.unregisterReceiver(mExternalStorageReceiver);
        } catch (Exception e) {
        }
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
            mCursor = null;
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
        LogUtil.i(MODULE_TAG, "fileName:" + fileName);
        return fileName;
    }

    public Cursor queryVideoClips() {
        ContentResolver cr = mAppCtx.getContentResolver();
        String selection = MediaStore.Video.Media.DATA + " like?";
        String[] selectionArgs = new String[]{ "%" + FOLDER_NAME + "%" };
        String[] parameters = { MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME};
        return cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                parameters, selection, selectionArgs, MediaStore.Video.Media.DATE_TAKEN + " DESC");
    }

    private synchronized void updateStorageState() {
        String state = Environment.getExternalStorageState();
        LogUtil.i(MODULE_TAG, "updateStorageState state:" + state);
        if(state.equals(Environment.MEDIA_MOUNTED)) {
            mStorageAvailable = true;
            mFolderPath = System.getenv("EXTERNAL_STORAGE") + File.separator + FOLDER_NAME;
            LogUtil.i(MODULE_TAG, "init mFolderPath:" + mFolderPath);
            File folder = new File(mFolderPath);
            if(!folder.exists()) folder.mkdirs();
        } else {
            // not available
            mStorageAvailable = false;
        }
    }
}

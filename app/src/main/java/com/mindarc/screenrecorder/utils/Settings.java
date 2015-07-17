package com.mindarc.screenrecorder.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.mindarc.screenrecorder.R;

import java.util.ArrayList;

/**
 * Created by sean on 7/9/15.
 */
public class Settings {
    private final static String TAG = "Settings";
    private Settings() {}
    public final static int FALLBACK_WIDTH = 720;
    public final static int FALLBACK_HEIGHT = 1280;
    public final static int FALLBACK_BITRATE = 2000000;    // 2M
    public final static int MAX_TIME_LIMIT = 24 * 60 * 60;         // 30 minutes
    public final static boolean FULLBACK_ROTATE = false;

    private final static String SETTINGS_PREF_NAME = "recorder_settings";
    private final static class Keys {
        public final static String RESOLUTION = "res";
        public final static String BITRATE = "bitrate";
        public final static String ROTATE = "rotate";
    }

    private Context mAppCtx;
    private SharedPreferences mSettingsPref;
    private boolean mInit = false;
    private ArrayList<String> mAvailResList;
    private int[] mAvailBitrates;
    private String[] mBitrateName;
    private int mChosenResIndex, mChosenBitrateIndex;
    private boolean mIsRotate;

    private static Settings sInstance;
    public static Settings instance() {
        if (sInstance == null) sInstance = new Settings();
        return sInstance;
    }

    public void init(Context ctx) {
        if (mInit) return;
        mAppCtx = ctx.getApplicationContext();
        mSettingsPref = mAppCtx.getSharedPreferences(SETTINGS_PREF_NAME, Context.MODE_MULTI_PROCESS);

        WindowManager wm = (WindowManager) mAppCtx.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        LogUtil.i(TAG, "getAvaiableResolutions width:" + size.x + ", height:" + size.y);

        loading(size);
    }

    public ArrayList<String> getAvailResList() {
        checkState();
        return mAvailResList;
    }

    public int getChosenResIndex() {
        checkState();
        return mChosenResIndex;
    }

    public void setChoosedRes(int index) {
        checkState();
        mChosenResIndex = index;
        mSettingsPref.edit().putString(Keys.RESOLUTION, mAvailResList.get(index)).commit();
        reloadBitrate();
    }

    public boolean isRotate() {
        checkState();
        return mIsRotate;
    }

    public String getChosenRes() {
        checkState();
        return mAvailResList.get(mChosenResIndex);
    }

    public void setRotate(boolean rotate) {
        checkState();
        mIsRotate = rotate;
        mSettingsPref.edit().putBoolean(Keys.ROTATE, rotate).commit();
    }

    public int[] getBitrates() {
        checkState();
        return mAvailBitrates;
    }

    public int getChosenBitrateIndex() {
        checkState();
        return mChosenBitrateIndex;
    }

    public void setChoosedBitrate(int index) {
        checkState();
        mChosenBitrateIndex = index;
        mSettingsPref.edit().putString(Keys.BITRATE, mBitrateName[index]).commit();
    }

    public int getChoosedBitrate() {
        checkState();
        return mAvailBitrates[mChosenBitrateIndex];
    }

    private void loading(Point screenSize) {
        mAvailResList = new ArrayList<String>();
        mAvailResList.add(String.valueOf(screenSize.x) + "x" + String.valueOf(screenSize.y));
        if (screenSize.x > 1080 && screenSize.y > 1920) {
            mAvailResList.add("1080x1920");
            mAvailResList.add("720x1280");
        } else if (screenSize.x > 720 && screenSize.y > 1280) {
            mAvailResList.add("720x1280");
        }

        SharedPreferences.Editor editor = mSettingsPref.edit();

        String res = mSettingsPref.getString(Keys.RESOLUTION, "");
        if (res.equals("")) {
            mChosenResIndex = 0;
            editor.putString(Keys.RESOLUTION, mAvailResList.get(mChosenResIndex));
        } else {
            mChosenResIndex = 0;
            int index = 0, length = mAvailResList.size();
            for (; index < length; ++index) {
                if (mAvailResList.get(index).equals(res)) {
                    mChosenResIndex = index;
                    break;
                }
            }
        }

        reloadBitrate();

        mIsRotate = mSettingsPref.getBoolean(Keys.ROTATE, false);
        editor.putBoolean(Keys.ROTATE, mIsRotate);

        editor.commit();
        mInit = true;

        StringBuilder sb = new StringBuilder();
        sb.append("mChosenResIndex:").append(mChosenResIndex)
                .append(", res:").append(mAvailResList.get(mChosenResIndex))
                .append(", bitrate name:").append(mBitrateName[mChosenBitrateIndex])
                .append(", bitrate:").append(mAvailBitrates[mChosenBitrateIndex])
                .append(", rotate:").append(mIsRotate);
        LogUtil.i(TAG, "after loading:" + sb.toString());
    }

    private void reloadBitrate() {
        SharedPreferences.Editor editor = mSettingsPref.edit();
        String choosedRes = mAvailResList.get(mChosenResIndex);
        String[] wh = choosedRes.split("x");
        int w = Integer.valueOf(wh[0]);
        int h = Integer.valueOf(wh[1]);

        mBitrateName = mAppCtx.getResources().getStringArray(R.array.bitrate_level_name);
        if (w >= 1080 && h >= 1920) {
            mAvailBitrates = mAppCtx.getResources().getIntArray(R.array.fullhd_bitrate);
        } else {
            mAvailBitrates = mAppCtx.getResources().getIntArray(R.array.hd_bitrate);
        }

        String chosenBitrateName = mSettingsPref.getString(Keys.BITRATE, "");
        if (chosenBitrateName.equals("")) {
            mChosenBitrateIndex = 0;
            editor.putString(Keys.BITRATE, mBitrateName[mChosenBitrateIndex]);
        } else {
            mChosenBitrateIndex = 0;
            int index = 0, length = mBitrateName.length;
            for (; index < length; ++index) {
                if (mBitrateName[index].equals(chosenBitrateName)) {
                    mChosenBitrateIndex = index;
                    break;
                }
            }
        }
        editor.commit();
    }

    private void checkState() {
        if (!mInit) throw new IllegalStateException("Not initialized!!!");
    }
}

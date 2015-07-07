package com.mindarc.screenrecorder.utils;

import android.util.Log;

import com.mindarc.screenrecorder.BuildConfig;

/**
 * Created by sean on 7/6/15.
 */
public class LogUtil {
    private static String APP_TAG = "SreenRecorder";
    private LogUtil() {}
    public static void setAppTag(String appTag) {
        APP_TAG = appTag;
    }
    public static void i(String moduleTag, String message) {
        if(BuildConfig.DEBUG) {
            Log.i(APP_TAG + ":" + moduleTag, message);
        }
    }
    public static void d(String moduleTag, String message) {
        if(BuildConfig.DEBUG) {
            Log.d(APP_TAG + ":" + moduleTag, message);
        }
    }
    public static void v(String moduleTag, String message) {
        if(BuildConfig.DEBUG) {
            Log.v(APP_TAG + ":" + moduleTag, message);
        }
    }
    public static void e(String moduleTag, String message) {
        if(BuildConfig.DEBUG) {
            Log.e(APP_TAG + ":" + moduleTag, message);
        }
    }
    public static void w(String moduleTag, String message) {
        if(BuildConfig.DEBUG) {
            Log.w(APP_TAG + ":" + moduleTag, message);
        }
    }

}

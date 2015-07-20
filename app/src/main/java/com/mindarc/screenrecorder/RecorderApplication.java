package com.mindarc.screenrecorder;

import android.app.Application;

import com.mindarc.screenrecorder.utils.ImageLoader;


/**
 * Created by sean on 7/17/15.
 */
public class RecorderApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ImageLoader.getGlobleImageLoader().initialize(this);
    }
}

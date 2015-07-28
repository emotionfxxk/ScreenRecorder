package com.mindarc.screenrecorder.utils;

import android.content.Context;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.mindarc.screenrecorder.RecorderApplication;

/**
 * Created by sean on 5/28/15.
 */
public class EventManager {
    public final static String CATEGORY_BUTTON = "Button";
    public final static String ACTION_BUTTON_CLICK = "Click";
    public final static String LABEL_START = "startRec";
    public final static String LABEL_STOP = "stopRec";

    public static void sendStartEvent(Context ctx) {
        Tracker t = ((RecorderApplication) ctx.getApplicationContext()).getTracker(RecorderApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory(CATEGORY_BUTTON)
                .setAction(ACTION_BUTTON_CLICK)
                .setLabel(LABEL_START)
                .build());
    }

    public static void sendStopEvent(Context ctx) {
        Tracker t = ((RecorderApplication) ctx.getApplicationContext()).getTracker(RecorderApplication.TrackerName.APP_TRACKER);
        t.send(new HitBuilders.EventBuilder()
                .setCategory(CATEGORY_BUTTON)
                .setAction(ACTION_BUTTON_CLICK)
                .setLabel(LABEL_STOP)
                .build());
    }
}

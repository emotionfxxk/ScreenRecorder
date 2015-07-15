package com.mindarc.screenrecorder.core;

/**
 * Created by sean on 7/9/15.
 */
public class Profile {
    private Profile() {}
    public final static int FALLBACK_WIDTH = 1280;
    public final static int FALLBACK_HEIGHT = 720;
    public final static int FALLBACK_BITRATE = 4000000;    // 4M
    public final static int MAX_TIME_LIMIT = 24 * 60 * 60;         // 30 minutes
    public final static boolean FULLBACK_ROTATE = false;

    private int width, height, bitrate, timeLimit;
    private boolean rotate;

    public static Profile get480PProfile() {
        Profile p = new Profile();
        p.width = 640;
        p.height = 480;
        p.bitrate = 1200000;
        p.timeLimit = MAX_TIME_LIMIT;
        p.rotate = false;
        return p;
    }

    public static Profile get480PProfileForGame() {
        Profile p = new Profile();
        p.width = 640;
        p.height = 480;
        p.bitrate = 1200000;
        p.timeLimit = MAX_TIME_LIMIT;
        p.rotate = true;
        return p;
    }

    public static Profile get720PProfile() {
        Profile p = new Profile();
        p.width = 1280;
        p.height = 720;
        p.bitrate = 2500000;
        p.timeLimit = MAX_TIME_LIMIT;
        p.rotate = false;
        return p;
    }

    public static Profile get720PProfileForGame() {
        Profile p = new Profile();
        p.width = 1280;
        p.height = 720;
        p.bitrate = 2500000;
        p.timeLimit = MAX_TIME_LIMIT;
        p.rotate = true;
        return p;
    }
}

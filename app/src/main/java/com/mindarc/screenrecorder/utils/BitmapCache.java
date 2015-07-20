package com.mindarc.screenrecorder.utils;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

/**
 * Created by sean on 5/27/15.
 */
public class BitmapCache {
    private final static String TAG = "BitmapCache";
    private static BitmapCache sInstance;
    private BitmapCache() { initialize(); }
    private LruCache<String, Bitmap> mMemoryCache;
    public static BitmapCache getGlobalBitmapCache() {
        if(sInstance == null) {
            sInstance = new BitmapCache();
        }
        return sInstance;
    }

    private void initialize() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/6th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 6;
        LogUtil.i(TAG, "cacheSize:" + cacheSize);

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }

}

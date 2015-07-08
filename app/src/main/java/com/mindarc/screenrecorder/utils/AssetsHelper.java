package com.mindarc.screenrecorder.utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by sean on 7/8/15.
 */
public class AssetsHelper {
    private final static String MODULE_TAG = "AssetsHelper";
    private AssetsHelper() {}

    /**
     * Clone file to dest path from android assets
     * @param ctx
     * @param assetPath
     * @param destPath
     */
    public static void cloneFile(Context ctx, String assetPath, String destPath) {
        try {
            File file = new File(destPath);
            if(!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } else {
                LogUtil.i(MODULE_TAG, destPath + "already exist!!!");
                return;
            }
            InputStream in = ctx.getAssets().open(assetPath);
            FileOutputStream out = new FileOutputStream(destPath);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

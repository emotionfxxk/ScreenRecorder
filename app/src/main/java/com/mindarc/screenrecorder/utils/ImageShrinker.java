package com.mindarc.screenrecorder.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by sean on 5/13/15.
 */
public class ImageShrinker {
    private final static String TAG = "ImageShrinker";
    private ImageShrinker() {}

    public static Bitmap shrinkImage(String imagePath, int outputWidth, int outputHeight, ImageView.ScaleType scaleType) {
        if(outputWidth == 0 || outputWidth == 0) {
            throw new IllegalArgumentException("outputWidth or outputHeight should not be zero!");
        }
        if(scaleType != ImageView.ScaleType.CENTER_CROP &&
                scaleType != ImageView.ScaleType.CENTER_INSIDE) {
            throw new IllegalArgumentException("Only support CENTER_CROP or CENTER_INSIDE now!");
        }
        Bitmap outImage = null;
        BitmapFactory.Options option = new BitmapFactory.Options();

        // step1: get resolution of bitmap
        option.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, option);
        Log.i(TAG, "option.outWidth:" + option.outWidth + ", option.outHeight:" + option.outHeight);

        if(option.outWidth == 0 || option.outHeight == 0) {
            Log.e(TAG, "Mal-format image");
            return null;
        }

        // step2: decode bitmap at appropriated scale rate
        float ratioX = option.outWidth / (float)outputWidth;
        float ratioY = option.outHeight / (float)outputHeight;

        option.inJustDecodeBounds = false;
        // note: the decoder uses a final value based on powers of 2, any other value will
        // be rounded down to the nearest power of 2.
        option.inSampleSize = (scaleType == ImageView.ScaleType.CENTER_CROP) ?
                (int)Math.min(ratioX, ratioY) : (int)Math.max(ratioX, ratioY);
        Log.i(TAG, "ratioX:" + ratioX + ", ratioY:" + ratioY + ", option.inSampleSize:" + option.inSampleSize);
        Bitmap tmpImage = BitmapFactory.decodeFile(imagePath, option);
        if(tmpImage == null) {
            return null;
        }

        // step3: scale image to the final size
        ratioX = (float)outputWidth / tmpImage.getWidth();
        ratioY = (float)outputHeight / tmpImage.getHeight();

        Log.i(TAG, "tmpImage w:" + tmpImage.getWidth() + ", h:" + tmpImage.getHeight() +
                ", ratioX:" + ratioX + ", ratioY:" + ratioY + ", outputWidth:" + outputWidth + ", outputHeight:" + outputHeight);
        Matrix matrix = new Matrix();
        float scaleRatio = 1;
        int drawX = 0, drawY = 0;
        int drawWidth = 0, drawHeight = 0;
        if(scaleType == ImageView.ScaleType.CENTER_CROP) {
            scaleRatio = Math.min(1.0f, Math.max(ratioX, ratioY));
            if(ratioX < ratioY) {
                drawX = Math.max(0, (int)((tmpImage.getWidth() - outputWidth / scaleRatio) / 2));
                drawWidth = tmpImage.getWidth() - 2 * drawX;
                drawHeight = tmpImage.getHeight();
            } else {
                drawY = Math.max(0, (int)((tmpImage.getHeight() - outputHeight / scaleRatio) / 2));
                drawWidth = tmpImage.getWidth();
                drawHeight = tmpImage.getHeight() - 2 * drawY;
            }
        } else if(scaleType == ImageView.ScaleType.CENTER_INSIDE) {
            scaleRatio = Math.min(1.0f, Math.min(ratioX, ratioY));
            drawWidth = tmpImage.getWidth();
            drawHeight = tmpImage.getHeight();
        }
        matrix.preScale(scaleRatio, scaleRatio);
        Log.i(TAG, "drawWidth:" + drawWidth + ", drawHeight:" + drawHeight);
        outImage = Bitmap.createBitmap(tmpImage, drawX, drawY, drawWidth, drawHeight, matrix, true);
        if (outImage != tmpImage) {
            tmpImage.recycle();
        }
        Log.i(TAG, "outImage w:" + outImage.getWidth() + ", h:" + outImage.getHeight());
        return outImage;
    }
}

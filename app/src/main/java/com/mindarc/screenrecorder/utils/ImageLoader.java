package com.mindarc.screenrecorder.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageView;



import java.lang.ref.WeakReference;

/**
 * Created by sean on 5/27/15.
 */
public class ImageLoader {
    private final static String TAG = "ImageLoader";
    private static ImageLoader sImageLoader;
    private Context mAppCtx;
    private ImageLoader() {}
    public static ImageLoader getGlobleImageLoader() {
        if(sImageLoader == null) {
            sImageLoader = new ImageLoader();
        }
        return sImageLoader;
    }
    public void initialize(Context appCtx) {
        mAppCtx = appCtx;
    }
    public void loadBitmap(long imageId, ImageView imageView) {
        if(mAppCtx == null) throw new IllegalStateException("Please initialize app context before using!");
        final String imageKey = String.valueOf(imageId);
        final Bitmap bitmap = BitmapCache.getGlobalBitmapCache().getBitmapFromMemCache(imageKey);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            return;
        }

        if (cancelPotentialWork(imageId, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(bitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(imageId);
        }
    }

    public void loadBitmap(String path, String specifiedKey, int placeHolderImageResId, ImageView imageView,
                           int desiredWidth, int desiredHeight, ImageView.ScaleType scaleType) {
        if(mAppCtx == null) throw new IllegalStateException("Please initialize app context before using!");
        final String imageKey = (specifiedKey == null) ? (path + desiredWidth + "x" + desiredHeight) : specifiedKey;
        final Bitmap bitmap = BitmapCache.getGlobalBitmapCache().getBitmapFromMemCache(imageKey);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            // cancel image loader task if have
            if(cancelPotentialWork(path, imageView)) {
                // set place holder
                if(placeHolderImageResId != 0) {
                    imageView.setImageResource(placeHolderImageResId);
                } else {
                    imageView.setImageBitmap(null);
                }
                // schedule load task
                ImageLoaderTask imageLoaderTask =
                        new ImageLoaderTask(imageView, path, imageKey, desiredWidth, desiredHeight, scaleType);
                imageLoaderTask.execute();
                imageView.setTag(imageLoaderTask);
            }
        }
    }

    private boolean cancelPotentialWork(String path, ImageView imageView) {
        final ImageLoaderTask loadImageTask = (ImageLoaderTask)imageView.getTag();
        if(loadImageTask != null && loadImageTask.path.equals(path)) {
            // The same work is already in progress
            return false;
        } else {
            if(loadImageTask != null) {
                loadImageTask.cancel(true);
            }
            imageView.setTag(null);
            return true;
        }
    }

    private boolean cancelPotentialWork(long data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            final long bitmapData = bitmapWorkerTask.data;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == 0 || bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;
        public AsyncDrawable(Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(null, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }
        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    class BitmapWorkerTask extends AsyncTask<Long, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private long data = 0;
        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
        }
        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Long... params) {
            data = params[0];
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap =  MediaStore.Video.Thumbnails.getThumbnail(
                    mAppCtx.getContentResolver(), data,
                    MediaStore.Images.Thumbnails.MINI_KIND, options);
            LogUtil.i(TAG, "id:" + params[0] + ", bitmap:" + bitmap);
            BitmapCache.getGlobalBitmapCache().addBitmapToMemoryCache(String.valueOf(params[0]), bitmap);
            return bitmap;
        }
        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap.recycle();
                bitmap = null;
            }
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask =
                        getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private static class ImageLoaderTask extends AsyncTask<Void, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        public final String path, imageKey;
        private final ImageView.ScaleType mScaleType;
        private int desiredWidth, desiredHeight;
        public ImageLoaderTask(ImageView imageView, String path, String imageKey,
                               int desiredWidth, int desiredHeight, ImageView.ScaleType scaleType) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
            this.path = path;
            this.imageKey = imageKey;
            this.desiredWidth = desiredWidth;
            this.desiredHeight = desiredHeight;
            mScaleType = scaleType;
        }
        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap = ImageShrinker.shrinkImage(path, desiredWidth, desiredHeight, mScaleType);
            BitmapCache.getGlobalBitmapCache().addBitmapToMemoryCache(imageKey, bitmap);
            return bitmap;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                if(bitmap != null) {
                    bitmap.recycle();
                    bitmap = null;
                }
            }
            if(bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if(imageView != null) imageView.setImageBitmap(bitmap);
                else bitmap.recycle();
            }
        }
    }
}

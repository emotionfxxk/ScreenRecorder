package com.mindarc.screenrecorder.fragment;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.provider.MediaStore;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mindarc.screenrecorder.R;
import com.mindarc.screenrecorder.utils.ImageLoader;
import com.mindarc.screenrecorder.utils.LogUtil;

/**
 * Created by sean on 7/20/15.
 */
public class ClipsAdapter extends CursorAdapter {
    private final static String TAG = "ClipsAdapter";
    private int mItemWidth, mItemHeight;
    private class _ViewHolder {
        public _ViewHolder(ImageView thumb, TextView name) {
            mThumbnail = thumb;
            mName = name;
        }
        public ImageView mThumbnail;
        public TextView mName;
    }

    public ClipsAdapter(Context context, Cursor c) {
        super(context, c, false);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        LogUtil.i(TAG, "ClipsAdapter width:" + size.x + ", height:" + size.y);

        Resources res = context.getResources();
        int marginInPx = 2 * (res.getDimensionPixelSize(R.dimen.grid_horizontal_margin) +
                res.getDimensionPixelSize(R.dimen.grid_cell_space_margin));

        mItemHeight = mItemWidth = (size.x - marginInPx) / 3;
        LogUtil.i(TAG, "ClipsAdapter marginInPx:" + marginInPx + ", mItemWidth:" + mItemWidth);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View viewRoot = LayoutInflater.from(context).inflate(R.layout.video_clip_item, parent, false);
        viewRoot.setTag(new _ViewHolder((ImageView)viewRoot.findViewById(R.id.thumbnail),
                (TextView)viewRoot.findViewById(R.id.file_name)));
        ViewGroup.LayoutParams lp = viewRoot.getLayoutParams();
        lp.width = mItemWidth;
        lp.height = mItemHeight;
        viewRoot.setLayoutParams(lp);
        return viewRoot;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        _ViewHolder vh = (_ViewHolder) view.getTag();
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
            int columnNameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
            long id = cursor.getLong(columnIndex);
            String name = cursor.getString(columnNameIndex);
            LogUtil.i(TAG, "id:" + id + ", name:" + name);
            ImageLoader.getGlobleImageLoader().loadBitmap(id, vh.mThumbnail);
            vh.mName.setText(name);
        }
    }
}

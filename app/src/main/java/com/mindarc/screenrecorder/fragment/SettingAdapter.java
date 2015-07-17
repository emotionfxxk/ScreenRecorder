package com.mindarc.screenrecorder.fragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.mindarc.screenrecorder.R;
import com.mindarc.screenrecorder.utils.LogUtil;
import com.mindarc.screenrecorder.utils.Settings;
import com.mindarc.screenrecorder.utils.StorageHelper;

import java.util.ArrayList;
import android.text.format.Formatter;

/**
 * Created by sean on 7/16/15.
 */
public class SettingAdapter extends BaseExpandableListAdapter implements ExpandableListView.OnChildClickListener {
    private final static String TAG = "SettingAdapter";
    private Context mContext;

    public static class GroupHeader {
        public String mTitle;
        public String mValue;
        public GroupHeader(String title, String value) {
            mTitle = title;
            mValue = value;
        }
    }

    private ArrayList<GroupHeader> mHeaders;

    public SettingAdapter(Context ctx) {
        mContext = ctx;
        init();
    }

    private void init() {
        mHeaders = new ArrayList<GroupHeader>();
        Settings settings = Settings.instance();
        mHeaders.add(new GroupHeader(mContext.getString(R.string.resolution),
                settings.getChosenRes()));
        mHeaders.add(new GroupHeader(mContext.getString(R.string.bitrate),
                Formatter.formatShortFileSize(mContext, settings.getChoosedBitrate())));
        mHeaders.add(new GroupHeader(mContext.getString(R.string.rotation),
                settings.isRotate() ? mContext.getString(R.string.orien_landscape) :
                        mContext.getString(R.string.orien_portrait)));
    }

    @Override
    public int getGroupCount() {
        return mHeaders.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        int childCount = 0;
        switch (groupPosition) {
            case 0:
                childCount = Settings.instance().getAvailResList().size();
                break;
            case 1:
                childCount = Settings.instance().getBitrates().length;
                break;
            case 2:
                childCount = 2;
                break;
        }
        return childCount;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mHeaders.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition * 8 + childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupHeader header = (GroupHeader) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.setting_group_header, null);
        }

        TextView titleLeft = (TextView) convertView
                .findViewById(R.id.group_title_left);
        titleLeft.setText(header.mTitle);

        TextView value = (TextView) convertView
                .findViewById(R.id.subtitle);
        value.setText(header.mValue);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.setting_single_choose_item, null);
        }

        String value = null;
        CheckedTextView titleLeft = (CheckedTextView) convertView
                .findViewById(R.id.vaule);

        boolean isChecked = false;
        if (groupPosition == 0) {
            value = Settings.instance().getAvailResList().get(childPosition);
            isChecked = (Settings.instance().getChosenResIndex() == childPosition);
        } else if(groupPosition == 1) {
            value = Formatter.formatShortFileSize(mContext, Settings.instance().getBitrates()[childPosition]);
            isChecked = (Settings.instance().getChosenBitrateIndex() == childPosition);
        } else if(groupPosition == 2) {
            value = childPosition == 0 ? mContext.getString(R.string.orien_portrait) :
                    mContext.getString(R.string.orien_landscape);
            isChecked = ((Settings.instance().isRotate() ? 1 : 0) == childPosition);
        }
        titleLeft.setText(value);
        titleLeft.setChecked(isChecked);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        LogUtil.i(TAG, "groupPosition:" + groupPosition + ", childPosition:" + childPosition);
        if (groupPosition == 0) {
            Settings.instance().setChoosedRes(childPosition);
            parent.collapseGroup(groupPosition);
            init();
            notifyDataSetChanged();
        } else if(groupPosition == 1) {
            Settings.instance().setChoosedBitrate(childPosition);
            parent.collapseGroup(groupPosition);
            init();
            notifyDataSetChanged();
        } else if(groupPosition == 2) {
            Settings.instance().setRotate(childPosition == 1);
            parent.collapseGroup(groupPosition);
            init();
            notifyDataSetChanged();
        }
        return false;
    }
}

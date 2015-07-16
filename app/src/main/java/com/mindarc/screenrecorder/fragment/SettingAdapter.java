package com.mindarc.screenrecorder.fragment;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.mindarc.screenrecorder.R;

/**
 * Created by sean on 7/16/15.
 */
public class SettingAdapter extends BaseExpandableListAdapter {
    private Context mContext;

    public static class GroupHeader {
        public String mTitle;
        public String mValue;
        public GroupHeader(String title, String value) {
            mTitle = title;
            mValue = value;
        }
    }

    public static class Value {
        public String mValue;
        public boolean mSelected;
        public Value(String value, boolean selected) {
            mSelected = selected;
            mValue = value;
        }
    }


    private GroupHeader[] mHeaders = new GroupHeader[] {
            new GroupHeader("Resolution", "1280x720"),
            new GroupHeader("Bitrate", "1.5M"),
            new GroupHeader("Orientation", "Portrait"),
            new GroupHeader("Name", "1984.12.12.mp4"),
    };

    private Value[] mValues = new Value[] {
        new Value("1280x720", true),
        new Value("1920x1080", false),
        new Value("640x360", false),
    };

    public SettingAdapter(Context ctx) {
        mContext = ctx;
    }

    @Override
    public int getGroupCount() {
        return mHeaders.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mValues.length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mHeaders[groupPosition];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mValues[childPosition];
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
        Value value = (Value) getChild(groupPosition, childPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.setting_single_choose_item, null);
        }

        TextView titleLeft = (TextView) convertView
                .findViewById(R.id.value_title_left);
        titleLeft.setText(value.mValue);

        CheckBox cb = (CheckBox) convertView.findViewById(R.id.checked);
        cb.setChecked(value.mSelected);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}

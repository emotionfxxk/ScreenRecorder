package com.mindarc.screenrecorder.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.GridView;

import com.mindarc.screenrecorder.Constants;
import com.mindarc.screenrecorder.R;
import com.mindarc.screenrecorder.RecorderModel;
import com.mindarc.screenrecorder.RecorderService;
import com.mindarc.screenrecorder.event.RecorderEvent;
import com.mindarc.screenrecorder.utils.LogUtil;
import com.mindarc.screenrecorder.utils.Settings;
import com.mindarc.screenrecorder.utils.StorageHelper;

import de.greenrobot.event.EventBus;
import in.srain.cube.views.GridViewWithHeaderAndFooter;

/**
 * Created by sean on 7/10/15.
 * TODO: Add recorder setting here
 * 1. Resolution: phone resolution(default), 720P, 1080P
 * 2. Bitrate: 720P {2M(default network), 2.5M(normal), 3M(HQ)}
 * 1080P {4M(default network), 5M(normal), 7.5M(HQ)}
 * 3. Orientation:portrait(default), landscape(gaming)
 * 4. Default name(data), or rename manually
 */
public class RecorderFragment extends Fragment implements View.OnClickListener {
    private final static String MODULE_TAG = "RecorderFragment";
    private CheckedTextView mShutter;
    private ExpandableListView mSettings;
    private SettingAdapter mAdapter;
    private ClipsAdapter mClipsAdapter;

    private GridViewWithHeaderAndFooter mClips;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.i(MODULE_TAG, "onCreate()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.recorder_fragment, container, false);
        mShutter = (CheckedTextView) rootView.findViewById(R.id.shutter);
        mShutter.setOnClickListener(this);
        mSettings = (ExpandableListView) rootView.findViewById(R.id.settings);
        mAdapter = new SettingAdapter(getActivity());
        mSettings.setAdapter(mAdapter);
        mSettings.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                int groupCount = mAdapter.getGroupCount();
                for (int i = 0; i < groupCount; ++i) {
                    if (i != groupPosition) {
                        mSettings.collapseGroup(i);
                    }
                }
            }
        });
        mClipsAdapter = new ClipsAdapter(getActivity().getApplicationContext(),
                StorageHelper.sStorageHelper.getVideoClips());
        mClips = (GridViewWithHeaderAndFooter) rootView.findViewById(R.id.clips);
        mClips.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LogUtil.i(MODULE_TAG, "position:" + position);
                Cursor cursor = mClipsAdapter.getCursor();
                LogUtil.i(MODULE_TAG, "cursor:" + cursor);
                if (cursor != null) {
                    cursor.moveToFirst();
                    if (cursor.move(position)) {
                        int columnIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
                        long mediaId = cursor.getLong(columnIndex);
                        Uri uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Long.toString(mediaId));
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.setDataAndType(uri, "video/mp4");
                        startActivity(intent);
                    }
                }
            }
        });

        Resources res = getActivity().getResources();
        View header = new View(getActivity());
        header.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                res.getDimensionPixelSize(R.dimen.grid_horizontal_margin)));


        int placeHolderHeight = 2 * res.getDimensionPixelSize(R.dimen.shutter_vertical_padding) +
                res.getDimensionPixelSize(R.dimen.shutter_size) + res.getDimensionPixelSize(R.dimen.setting_menu_height);
        View footer = new View(getActivity());
        footer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                placeHolderHeight));

        mClips.addHeaderView(header);
        mClips.addFooterView(footer);
        mClips.setAdapter(mClipsAdapter);

        mSettings.setOnChildClickListener(mAdapter);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        LogUtil.i(MODULE_TAG, "onAttach()");
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LogUtil.i(MODULE_TAG, "onDetach()");
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onClick(View v) {
        boolean isRecording = RecorderModel.getModel().isRecorderRunning();
        LogUtil.i(MODULE_TAG, "onClick() isRecording=" + isRecording);
        mShutter.toggle();
        mShutter.setEnabled(false);
        if (!isRecording) {
            start_rec();
            getActivity().finish();
            EventBus.getDefault().unregister(this);
        } else {
            stop_rec();
        }
    }

    public void onEvent(RecorderEvent event) {
        LogUtil.i(MODULE_TAG, "onEvent isRecording:" + event.isRecording +
            ", fileName:" + event.fileName);
        mShutter.setEnabled(true);
        mShutter.setChecked(event.isRecording);
    }

    private void start_rec() {
        // Send init request
        Intent intent = new Intent(getActivity(), RecorderService.class);
        String fileName = StorageHelper.sStorageHelper.generateFileName();
        LogUtil.i(MODULE_TAG, "start_rec fileName:" + fileName);
        intent.setAction(Constants.Action.START_REC);
        intent.putExtra(Constants.Key.FILE_NAME, fileName);
        intent.putExtra(Constants.Key.TIME_LIMIT, 24 * 60 * 60);
        String res = Settings.instance().getChosenRes();
        String[] values = res.split("x");
        intent.putExtra(Constants.Key.WIDTH, values[0]);
        intent.putExtra(Constants.Key.HEIGHT, values[1]);
        intent.putExtra(Constants.Key.BITRATE, Settings.instance().getChoosedBitrate());
        intent.putExtra(Constants.Key.ROTATE, Settings.instance().isRotate());
        getActivity().startService(intent);
    }

    private void stop_rec() {
        // Send init request
        Intent intent = new Intent(getActivity(), RecorderService.class);
        intent.setAction(Constants.Action.STOP_REC);
        getActivity().startService(intent);
    }

}

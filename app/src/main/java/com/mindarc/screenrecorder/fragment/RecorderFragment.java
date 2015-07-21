package com.mindarc.screenrecorder.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mindarc.screenrecorder.Constants;
import com.mindarc.screenrecorder.R;
import com.mindarc.screenrecorder.RecorderModel;
import com.mindarc.screenrecorder.RecorderService;
import com.mindarc.screenrecorder.event.RecorderEvent;
import com.mindarc.screenrecorder.utils.LogUtil;
import com.mindarc.screenrecorder.utils.Settings;
import com.mindarc.screenrecorder.utils.StorageHelper;

import java.util.ArrayList;
import java.util.List;

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
    private final static int RESERVED_SUB_MENU_ITEM_COUNT = 3;
    private CheckedTextView mShutter;
    private ClipsAdapter mClipsAdapter;
    private TextView mChosenBitrate, mChosenResolution, mChosenRotate;
    private LinearLayout mSubMenuContrainer;
    private List<CheckedTextView> mSubMenuItems;
    private int mOpenedMenuId = -1;

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

        rootView.findViewById(R.id.bitrate_menu).setOnClickListener(this);
        rootView.findViewById(R.id.resolution_menu).setOnClickListener(this);
        rootView.findViewById(R.id.rotate_menu).setOnClickListener(this);

        mChosenBitrate = (TextView) rootView.findViewById(R.id.bitrate_value);
        mChosenResolution = (TextView) rootView.findViewById(R.id.resolution_value);
        mChosenRotate = (TextView) rootView.findViewById(R.id.rotate_value);

        mSubMenuContrainer = (LinearLayout) rootView.findViewById(R.id.sub_menu_contrainer);
        mSubMenuItems = new ArrayList<CheckedTextView>();
        for (int index = 0; index < RESERVED_SUB_MENU_ITEM_COUNT; ++index) {
            CheckedTextView ctv = (CheckedTextView)inflater.inflate(R.layout.sub_menu_items,
                    mSubMenuContrainer, false);
            ctv.setId(index);
            ctv.setOnClickListener(this);
            mSubMenuItems.add(ctv);
        }

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

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtil.i(MODULE_TAG, "onResume()");
        updateSettings();
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
        switch (v.getId()) {
            case R.id.shutter:
                onClickShutter();
                break;
            case R.id.bitrate_menu:
            case R.id.resolution_menu:
            case R.id.rotate_menu:
                onClickMenuItem(v.getId());
                break;
            default:
                onClickSubMenuItem(v.getId());
                break;
        }
    }

    public void onEvent(RecorderEvent event) {
        LogUtil.i(MODULE_TAG, "onEvent isRecording:" + event.isRecording +
            ", fileName:" + event.fileName);
        mShutter.setEnabled(true);
        mShutter.setChecked(event.isRecording);
    }

    private void updateSettings() {
        final Settings settings = Settings.instance();
        mChosenBitrate.setText(Formatter.formatShortFileSize(getActivity(), settings.getChoosedBitrate()));
        mChosenResolution.setText(settings.getChosenRes());
        mChosenRotate.setText(settings.isRotate() ? getActivity().getString(R.string.rotate_on) :
                getActivity().getString(R.string.rotate_off));
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

    private void onClickShutter() {
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

    private void onClickMenuItem(int menuId) {
        final Settings settings = Settings.instance();
        if (mOpenedMenuId == menuId) {
            mOpenedMenuId = -1;
            // close menu
            mSubMenuContrainer.setVisibility(View.INVISIBLE);
            mSubMenuContrainer.removeAllViews();
        } else {
            mOpenedMenuId = menuId;
            // open menu here
            ArrayList<String> values = new ArrayList<String>();
            int chosenIndex = 0;
            if (menuId == R.id.resolution_menu) {
                values = settings.getAvailResList();
                chosenIndex = settings.getChosenResIndex();
            } else if (menuId == R.id.bitrate_menu) {
                int[] bitrates = settings.getBitrates();
                for (int bitrate : bitrates) {
                    values.add(Formatter.formatShortFileSize(getActivity(), bitrate));
                }
                chosenIndex = settings.getChosenBitrateIndex();
            } else if (menuId == R.id.rotate_menu) {
                values.add(getActivity().getString(R.string.rotate_on));
                values.add(getActivity().getString(R.string.rotate_off));
                chosenIndex = settings.isRotate() ? 0 : 1;
            }
            if (values != null) {
                mSubMenuContrainer.removeAllViews();
                for (int index = 0; index < values.size(); ++index) {
                    CheckedTextView ctv = mSubMenuItems.get(index);
                    ctv.setText(values.get(index));
                    ctv.setChecked(chosenIndex == index);
                    mSubMenuContrainer.addView(ctv);
                }
                mSubMenuContrainer.setVisibility(View.VISIBLE);
            }
        }
    }

    private void onClickSubMenuItem(int id) {
        final Settings settings = Settings.instance();
        if (mOpenedMenuId == R.id.resolution_menu) {
            settings.setChoosedRes(id);
        } else if (mOpenedMenuId == R.id.bitrate_menu) {
            settings.setChoosedBitrate(id);
        } else if (mOpenedMenuId == R.id.rotate_menu) {
            settings.setRotate(id == 0);
        }
        mOpenedMenuId = -1;
        // close menu
        mSubMenuContrainer.setVisibility(View.INVISIBLE);
        mSubMenuContrainer.removeAllViews();

        updateSettings();
    }

}

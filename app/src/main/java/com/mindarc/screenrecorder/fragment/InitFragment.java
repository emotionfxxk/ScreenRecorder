package com.mindarc.screenrecorder.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mindarc.screenrecorder.R;
import com.mindarc.screenrecorder.utils.LogUtil;

/**
 * Created by sean on 7/10/15.
 */
public class InitFragment extends Fragment {
    private final static String MODULE_TAG = "InitFragment";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.i(MODULE_TAG, "onCreate()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.init_fragment, container, false);
        return rootView;
    }

}

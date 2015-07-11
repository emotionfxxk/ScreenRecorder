package com.mindarc.screenrecorder.fragment;

import android.app.Activity;
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
public class ErrorFragment extends Fragment {
    private final static String MODULE_TAG = "ErrorFragment";
    private final int mErrorId;
    private ErrorFragment(int error_Id) {
        mErrorId = error_Id;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.i(MODULE_TAG, "onCreate()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.error_fragment, container, false);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtil.i(MODULE_TAG, "onAttach()");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        LogUtil.i(MODULE_TAG, "onAttach()");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LogUtil.i(MODULE_TAG, "onDetach()");
    }
}

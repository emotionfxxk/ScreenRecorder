package com.mindarc.screenrecorder.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mindarc.screenrecorder.Constants;
import com.mindarc.screenrecorder.R;
import com.mindarc.screenrecorder.utils.LogUtil;

/**
 * Created by sean on 7/10/15.
 */
public class ErrorFragment extends Fragment {
    private final static String MODULE_TAG = "ErrorFragment";
    private int mErrorId;
    private TextView mErrorMessage;

    public static ErrorFragment newInstance(int errorId) {
        ErrorFragment fragment = new ErrorFragment();
        fragment.mErrorId = errorId;
        return fragment;
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
        mErrorMessage = (TextView) rootView.findViewById(R.id.error_message);

        switch (mErrorId) {
            case Constants.ErrorId.ABI_NOT_SUPPORTED:
                mErrorMessage.setText(R.string.abi_not_supported);
                break;
            case Constants.ErrorId.NOT_ROOTED:
                mErrorMessage.setText(R.string.not_rooted);
                break;
        }
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

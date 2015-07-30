package com.mindarc.screenrecorder;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.baidu.mobads.AdSettings;
import com.baidu.mobads.AdView;
import com.baidu.mobads.AdViewListener;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.mindarc.screenrecorder.event.InitEvent;
import com.mindarc.screenrecorder.fragment.ErrorFragment;
import com.mindarc.screenrecorder.fragment.InitFragment;
import com.mindarc.screenrecorder.fragment.RecorderFragment;
import com.mindarc.screenrecorder.utils.LogUtil;
import com.mindarc.screenrecorder.utils.Settings;
import com.mindarc.screenrecorder.utils.StorageHelper;

import org.json.JSONObject;

import de.greenrobot.event.EventBus;


public class RecorderActivity extends ActionBarActivity {
    private final static String MODULE_NAME = "RecorderActivity";
    protected Tracker mAppTracker;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);
        RecorderModel.getModel().init(this);
        StorageHelper.sStorageHelper.init(this);
        Settings.instance().init(this);
        init();

        if (!RecorderModel.getModel().isInitialized()) {
            showInitFragment();
        } else {
            showRecorderFragment();
        }
        mAppTracker = ((RecorderApplication) getApplication()).getTracker(RecorderApplication.TrackerName.APP_TRACKER);

        AdSettings.setKey(new String[]{"baidu", "中 国 "});
        AdSettings.setHob(new String[]{"应用", "游戏", "手机"});
        adView = new AdView(this);
        adView.setListener(new AdViewListener() {
            @Override
            public void onAdReady(AdView adView) {
            }

            @Override
            public void onAdShow(JSONObject jsonObject) {
            }

            @Override
            public void onAdClick(JSONObject jsonObject) {
            }

            @Override
            public void onAdFailed(String s) {
            }

            @Override
            public void onAdSwitch() {
            }

            @Override
            public void onVideoStart() {
            }

            @Override
            public void onVideoFinish() {
            }

            @Override
            public void onVideoError() {
            }

            @Override
            public void onVideoClickClose() {
            }

            @Override
            public void onVideoClickAd() {
            }

            @Override
            public void onVideoClickReplay() {
            }
        });
        LinearLayout.LayoutParams rllp = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        //rllp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        LinearLayout rootView = (LinearLayout)findViewById(R.id.view_root);
        rootView.addView(adView, rllp);
    }

    @Override
    public void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO);
            //getSupportActionBar().
            getSupportActionBar().setLogo(R.drawable.ic_launcher);
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(getResources().getColor(R.color.color_main)));
        }
    }

    @Override
    protected void onDestroy() {
        adView.destroy();
        super.onDestroy();
        StorageHelper.sStorageHelper.deInit(this);
        EventBus.getDefault().unregister(this);
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    public void onEvent(InitEvent event) {
        LogUtil.i(MODULE_NAME, "event error_id:" + event.error_id);
        //event = new InitEvent(Constants.ErrorId.NOT_ROOTED);
        if (event.error_id == Constants.ErrorId.NO_ERROR) {
            showRecorderFragment();
        } else {
            showErrorFragment(event.error_id);
            hideRecorderFragment();
        }
    }

    private void hideRecorderFragment() {
        Fragment recFragment = getSupportFragmentManager().findFragmentByTag(RecorderFragment.class.getName());
        if (recFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(recFragment).commit();
        }
    }

    private void showInitFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_container, new InitFragment(), InitFragment.class.getName());
        transaction.commit();
    }

    private void showRecorderFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_container, new RecorderFragment(), RecorderFragment.class.getName());
        transaction.commit();
    }

    private void showErrorFragment(int errorId) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_container, ErrorFragment.newInstance(errorId), ErrorFragment.class.getName());
        transaction.commit();
    }

    private void init() {
        // Send init request
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(Constants.Action.INIT);
        startService(intent);
    }
}

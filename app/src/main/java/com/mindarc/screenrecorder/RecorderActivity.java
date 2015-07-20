package com.mindarc.screenrecorder;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.mindarc.screenrecorder.event.InitEvent;
import com.mindarc.screenrecorder.fragment.ErrorFragment;
import com.mindarc.screenrecorder.fragment.InitFragment;
import com.mindarc.screenrecorder.fragment.RecorderFragment;
import com.mindarc.screenrecorder.utils.LogUtil;
import com.mindarc.screenrecorder.utils.Settings;
import com.mindarc.screenrecorder.utils.StorageHelper;

import de.greenrobot.event.EventBus;


public class RecorderActivity extends ActionBarActivity {
    private final static String MODULE_NAME = "RecorderActivity";

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
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_USE_LOGO);
            //getSupportActionBar().
            getSupportActionBar().setLogo(R.drawable.ic_logo);
            getSupportActionBar().setBackgroundDrawable(
                    new ColorDrawable(getResources().getColor(R.color.color_main)));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StorageHelper.sStorageHelper.deInit(this);
        EventBus.getDefault().unregister(this);
    }

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
    }

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

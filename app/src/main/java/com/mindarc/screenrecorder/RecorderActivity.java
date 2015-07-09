package com.mindarc.screenrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.mindarc.screenrecorder.utils.LogUtil;


public class RecorderActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String MUDULE_NAME = "RecorderActivity";
    private Button mShutter;
    private int mRecorderState = Constants.State.UNINITIALIZED;
    private BroadcastReceiver mBc = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.i(MUDULE_NAME, "onReceive: " + action);
            if (action != null && action.equals(Constants.Action.ON_REC_STATE_CHANGED)) {
                onStateChanged(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mShutter = (Button) findViewById(R.id.shutter);
        mShutter.setOnClickListener(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.Action.ON_REC_STATE_CHANGED);
        getApplicationContext().registerReceiver(mBc, intentFilter);

        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterStateReceiver();
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

    @Override
    public void onClick(View v) {
        if (mRecorderState == Constants.State.FREE) {
            start_rec();
        } else if (mRecorderState == Constants.State.RECORDING) {
            stop_rec();
        }
    }

    private void unregisterStateReceiver() {
        try {
            getApplicationContext().unregisterReceiver(mBc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void init() {
        // TODO: update UI before init
        mShutter.setVisibility(View.INVISIBLE);

        // Send init request
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(Constants.Action.INIT);
        startService(intent);
    }

    private void start_rec() {
        // TODO: update UI before start
        mShutter.setVisibility(View.INVISIBLE);

        // Send init request
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(Constants.Action.START_REC);
        intent.putExtra(Constants.Key.FILE_NAME, "/sdcard/test1.mp4");
        intent.putExtra(Constants.Key.TIME_LIMIT, 15);
        intent.putExtra(Constants.Key.WIDTH, 720);
        intent.putExtra(Constants.Key.HEIGHT, 1280);
        intent.putExtra(Constants.Key.BITRATE, 4000000);
        intent.putExtra(Constants.Key.ROTATE, false);
        startService(intent);

        // back to home
        unregisterStateReceiver();
        finish();
    }

    private void stop_rec() {
        // TODO: update UI stop start
        mShutter.setText(R.string.shutter_stoping);
        mShutter.setVisibility(View.VISIBLE);
        mShutter.setEnabled(false);

        // Send init request
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(Constants.Action.STOP_REC);
        startService(intent);
    }

    private void onStateChanged(Intent intent) {
        int state = intent.getIntExtra(Constants.Key.STATE, Constants.State.UNINITIALIZED);
        int oldState = intent.getIntExtra(Constants.Key.OLD_STATE, Constants.State.UNINITIALIZED);
        LogUtil.i(MUDULE_NAME, "onStateChanged state: " + state + ", oldState:" + oldState);

        mRecorderState = state;
        if (oldState == Constants.State.UNINITIALIZED && state == Constants.State.FREE) {
            // TODO: succeed to init
            mShutter.setText(R.string.shutter_start);
            mShutter.setVisibility(View.VISIBLE);
            mShutter.setEnabled(true);
        } else if (oldState == Constants.State.FREE && state == Constants.State.RECORDING) {
            // TODO: succeed to start
            mShutter.setText(R.string.shutter_stop);
            mShutter.setVisibility(View.VISIBLE);
            mShutter.setEnabled(true);
        } else if (oldState == Constants.State.RECORDING && state == Constants.State.FREE) {
            // TODO: succeed to stop
            mShutter.setText(R.string.shutter_start);
            mShutter.setVisibility(View.VISIBLE);
            mShutter.setEnabled(true);
        } else if (oldState == Constants.State.UNINITIALIZED && state == Constants.State.FAILED_TO_INIT) {
            // TODO: succeed to init
        }
    }
}

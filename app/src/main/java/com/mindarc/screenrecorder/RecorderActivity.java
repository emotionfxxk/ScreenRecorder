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
        getApplicationContext().unregisterReceiver(mBc);
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

    }

    private void init() {
        // TODO: update UI before init

        // Send init request
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(Constants.Action.INIT);
        startService(intent);
    }

    private void start_rec() {
        // TODO: update UI before start

        // Send init request
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(Constants.Action.START_REC);
        startService(intent);
    }

    private void stop_rec() {
        // TODO: update UI stop start

        // Send init request
        Intent intent = new Intent(this, RecorderService.class);
        intent.setAction(Constants.Action.START_REC);
        startService(intent);
    }

    private void onStateChanged(Intent intent) {

    }
}

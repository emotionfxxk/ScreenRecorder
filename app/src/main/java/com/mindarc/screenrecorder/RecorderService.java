package com.mindarc.screenrecorder;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;

import com.mindarc.screenrecorder.core.Profile;
import com.mindarc.screenrecorder.core.ShellScreenRecorder;
import com.mindarc.screenrecorder.utils.LogUtil;

/**
 * Created by sean on 7/9/15.
 */
public class RecorderService extends Service implements ShellScreenRecorder.StateListener {
    private final static String MODULE_TAG = "RecorderService";

    @Override
    public void onInitialized() {
        LogUtil.i(MODULE_TAG, "onInitialized");
        Intent bcIntent = new Intent();
        bcIntent.setAction(Constants.Action.ON_REC_STATE_CHANGED);
        bcIntent.putExtra(Constants.Key.OLD_STATE, Constants.State.UNINITIALIZED);
        bcIntent.putExtra(Constants.Key.STATE, Constants.State.FREE);
        sendBroadcast(bcIntent);
    }

    @Override
    public void onStartRecorder(String fileName) {
        LogUtil.i(MODULE_TAG, "onStartRecorder");
        Intent bcIntent = new Intent();
        bcIntent.setAction(Constants.Action.ON_REC_STATE_CHANGED);
        bcIntent.putExtra(Constants.Key.OLD_STATE, Constants.State.FREE);
        bcIntent.putExtra(Constants.Key.STATE, Constants.State.RECORDING);
        bcIntent.putExtra(Constants.Key.FILE_NAME, fileName);
        sendBroadcast(bcIntent);
    }

    @Override
    public void onStopRecorder(String fileName) {
        LogUtil.i(MODULE_TAG, "onStopRecorder");
        // start activity
        Intent intent = new Intent(this, RecorderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);

        // then we send broadcast
        Intent bcIntent = new Intent();
        bcIntent.setAction(Constants.Action.ON_REC_STATE_CHANGED);
        bcIntent.putExtra(Constants.Key.OLD_STATE, Constants.State.RECORDING);
        bcIntent.putExtra(Constants.Key.STATE, Constants.State.FREE);
        bcIntent.putExtra(Constants.Key.FILE_NAME, fileName);
        sendBroadcast(bcIntent);
    }

    @Override
    public void onFailedToInit(int reason) {
        LogUtil.i(MODULE_TAG, "onFailedToInit reason:" + reason);
        Intent bcIntent = new Intent();
        bcIntent.setAction(Constants.Action.ON_REC_STATE_CHANGED);
        bcIntent.putExtra(Constants.Key.OLD_STATE, Constants.State.UNINITIALIZED);
        bcIntent.putExtra(Constants.Key.STATE, Constants.State.FAILED_TO_INIT);
        bcIntent.putExtra(Constants.Key.ERROR_ID, reason);
        sendBroadcast(bcIntent);
    }

    @Override
    public void onCreate() {
        LogUtil.i(MODULE_TAG, "onCreate");
        super.onCreate();

        // set listener
        ShellScreenRecorder.setsStateListener(this);
    }

    @Override
    public void onDestroy() {
        LogUtil.i(MODULE_TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        LogUtil.i(MODULE_TAG, "Received start id " + startId + ", action:" + action);

        if(action != null) {
            if (action.equals(Constants.Action.INIT)) {
                if(ShellScreenRecorder.init(this)) {
                    if(ShellScreenRecorder.isRecording()) {
                        onStartRecorder(ShellScreenRecorder.getFileName());
                    } else {
                        onInitialized();
                    }
                }
            } else if (action.equals(Constants.Action.START_REC)) {
                String fileName = intent.getStringExtra(Constants.Key.FILE_NAME);
                int timeLimit = intent.getIntExtra(Constants.Key.TIME_LIMIT, Profile.MAX_TIME_LIMIT);
                int width = intent.getIntExtra(Constants.Key.WIDTH, Profile.FALLBACK_WIDTH);
                int height = intent.getIntExtra(Constants.Key.HEIGHT, Profile.FALLBACK_HEIGHT);
                int bitrate = intent.getIntExtra(Constants.Key.BITRATE, Profile.FALLBACK_BITRATE);
                boolean rotate = intent.getBooleanExtra(Constants.Key.ROTATE, Profile.FULLBACK_ROTATE);
                ShellScreenRecorder.start(fileName, width, height, bitrate, timeLimit, rotate);

                // setup notification
                setupNotification();
            } else if (action.equals(Constants.Action.STOP_REC)) {
                ShellScreenRecorder.stop();
                stopForeground(true);
            }
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void setupNotification() {
        Intent notificationIntent = new Intent(this, RecorderService.class);
        notificationIntent.setAction(Constants.Action.STOP_REC);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0,
                notificationIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_stop);

        Notification notification = new Notification.Builder(this)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setTicker(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.notification_action_stop))
                .setSmallIcon(R.mipmap.ic_stop)
                .setLargeIcon(
                        Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        startForeground(Constants.FOREGROUND_SERVICE_ID,
                notification);
    }
}

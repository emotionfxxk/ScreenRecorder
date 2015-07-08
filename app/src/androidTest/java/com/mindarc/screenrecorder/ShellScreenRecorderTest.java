package com.mindarc.screenrecorder;

import android.test.AndroidTestCase;

import com.mindarc.screenrecorder.core.ShellScreenRecorder;
import com.mindarc.screenrecorder.utils.LogUtil;
import com.mindarc.screenrecorder.utils.Shell;

import java.io.File;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by sean on 7/7/15.
 */
public class ShellScreenRecorderTest extends AndroidTestCase implements ShellScreenRecorder.StateListener {
    private final static String MODULE_TAG = "ShellScreenRecorderTest";
    final Lock mListenerLock = new ReentrantLock();
    final Condition onInit = mListenerLock.newCondition();

    private Object onStoppedLock = new Object();

    @Override
    public void onInitialized() {
        LogUtil.i(MODULE_TAG, "onInitialized " + Thread.currentThread().getName());
        mListenerLock.lock();
        try {
            onInit.signalAll();
        } finally {
            mListenerLock.unlock();
        }
    }

    @Override
    public void onStartRecorder(String fileName) {
        LogUtil.i(MODULE_TAG, "onStartRecorder " + Thread.currentThread().getName() +
                ", fileName:" + fileName);        mListenerLock.lock();
    }

    @Override
    public void onStopRecorder(String fileName) {
        LogUtil.i(MODULE_TAG, "onStopRecorder " + Thread.currentThread().getName() +
                ", fileName:" + fileName);
        synchronized (onStoppedLock) {
            onStoppedLock.notifyAll();
        }
    }

    @Override
    protected void setUp() throws Exception {
        Shell.requestRootPermission();
        ShellScreenRecorder.setsStateListener(this);
        boolean inited = ShellScreenRecorder.init(getContext());

        LogUtil.i(MODULE_TAG, "setUp before await " + Thread.currentThread().getName());
        if(!inited) {
            mListenerLock.lock();
            try {
                onInit.await();
            } catch (Exception e) {
            } finally {
                mListenerLock.unlock();
            }
        }
        super.setUp();
    }

    public void test_5_s_screen_record() {
        boolean rooted = Shell.requestRootPermission();
        assertEquals(true, rooted);
        final String fileName = "/sdcard/my.mp4";
        {
            File clip_5_s = new File(fileName);
            if(clip_5_s.exists())
                clip_5_s.delete();
        }

        ShellScreenRecorder.start(fileName, 1280, 720, 4000000, 5, false);
        assertEquals(true, ShellScreenRecorder.isRecording());

        if (ShellScreenRecorder.isRecording()) {
            synchronized (onStoppedLock) {
                try {
                    onStoppedLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        assertEquals(false, ShellScreenRecorder.isRecording());
        {
            File clip_5_s = new File(fileName);
            assertEquals(true, clip_5_s.exists());
        }
    }

    public void test_15_s_screen_record() {
        boolean rooted = Shell.requestRootPermission();
        assertEquals(true, rooted);
        final String fileName = "/sdcard/my_15s.mp4";
        {
            File clip_15_s = new File(fileName);
            if(clip_15_s.exists())
                clip_15_s.delete();
        }

        ShellScreenRecorder.start(fileName, 1280, 720, 4000000, 15, false);
        assertEquals(true, ShellScreenRecorder.isRecording());

        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }

        ShellScreenRecorder.stop();

        if (ShellScreenRecorder.isRecording()) {
            synchronized (onStoppedLock) {
                try {
                    onStoppedLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        assertEquals(false, ShellScreenRecorder.isRecording());
    }
}

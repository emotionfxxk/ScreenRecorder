package com.mindarc.screenrecorder;

import android.test.AndroidTestCase;

import com.mindarc.screenrecorder.core.ShellScreenRecorder;
import com.mindarc.screenrecorder.utils.Shell;

import java.io.File;

/**
 * Created by sean on 7/7/15.
 */
public class ShellScreenRecorderTest extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
        Shell.requestRootPermission();
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

        try {
            Thread.sleep(8000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(false, ShellScreenRecorder.isRecording());
        {
            File clip_5_s = new File(fileName);
            assertEquals(true, clip_5_s.exists());
        }
    }
}

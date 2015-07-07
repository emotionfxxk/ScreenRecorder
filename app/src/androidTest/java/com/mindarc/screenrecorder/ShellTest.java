package com.mindarc.screenrecorder;

import android.test.AndroidTestCase;

import com.mindarc.screenrecorder.utils.Shell;

/**
 * Created by sean on 7/7/15.
 */
public class ShellTest extends AndroidTestCase {
    public void test_check_root_permission() {
        boolean rooted = Shell.requestRootPermission();
        assertEquals(true, rooted);
    }
}

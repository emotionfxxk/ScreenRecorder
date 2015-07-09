package com.mindarc.screenrecorder;

/**
 * Created by sean on 7/9/15.
 */
public class Constants {
    public static class Action {
        public static String INIT = "com.mindarc.screenrecorder.action.init";
        public static String START_REC = "com.mindarc.screenrecorder.action.start_rec";
        public static String STOP_REC = "com.mindarc.screenrecorder.action.stop_rec";

        public static String ON_REC_STATE_CHANGED = "com.mindarc.screenrecorder.action.on_rec_state_changed";
    }

    public static class Key {
        public static String OLD_STATE = "old_state";
        public static String STATE = "state";
        public static String FILE_NAME = "fileName";
        public static String ERROR_ID = "error_id";

        public static String WIDTH = "width";
        public static String HEIGHT = "height";
        public static String BITRATE = "bitrate";
        public static String ROTATE = "rotate";
        public static String TIME_LIMIT = "timeLimit";
    }

    public static class State {
        public final static int UNINITIALIZED = 0;
        public final static int FREE = 1;
        public final static int RECORDING = 2;
        public final static int FAILED_TO_INIT = 3;
    }

    public static class ErrorId {
        public final static int NO_ERROR = 0;
        public final static int NOT_ROOTED = 1;
        public final static int ABI_NOT_SUPPORTED = 2;
    }

    public static int FOREGROUND_SERVICE_ID = 621;
}

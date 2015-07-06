package com.mindarc.screenrecorder.utils;

import com.mindarc.screenrecorder.LogUtil;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Created by sean on 7/6/15.
 */
public class Shell {
    private final static String MODULE_TAG = "Shell";
    // Disable constructor
    private Shell() {}

    public static class Result {
        public int result;
        public String succeedMsg;
        public String errorMsg;
        public Result(int result, String succeedMsg, String errorMsg) {
            this.result = result;
            this.succeedMsg = succeedMsg;
            this.errorMsg = errorMsg;
        }
        public Result(int result) {
            this.result = result;
        }
    }

    public static boolean requestRootPermission() {
        Process p;
        boolean rooted = false;
        try {
            // Preform su to get root privilege
            p = Runtime.getRuntime().exec("su");
            // Attempt to write a file to a root-only
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("echo \"Do I have root?\" >/system/sd/temporary.txt\n");

            // Close the terminal
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {
                    rooted = true;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                LogUtil.e(MODULE_TAG, "exception while waitFor: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rooted;
    }

    public static Result execCommand(String command) {
        int result = -1;
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        OutputStreamWriter osw = null;
        try {
            proc = runtime.exec("su");
            osw = new OutputStreamWriter(proc.getOutputStream());
            osw.write(command);
            osw.write("exit\n");
            osw.flush();
            osw.close();

            result = proc.waitFor();
            StringBuilder successMsg = new StringBuilder();
            StringBuilder errorMsg = new StringBuilder();
            BufferedReader successResult = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            BufferedReader errorResult = new BufferedReader(
                    new InputStreamReader(proc.getErrorStream()));
            String s;
            while ((s = successResult.readLine()) != null) {
                successMsg.append(s);
            }
            while ((s = errorResult.readLine()) != null) {
                errorMsg.append(s);
            }
            return new Result(result, successMsg.toString(), errorMsg.toString());
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(MODULE_TAG, "Command resulted in an IO Exception: " + command);
            return new Result(result);
        } finally {
            if (osw != null) {
                try {
                    osw.close();
                }
                catch (IOException e){}
            }
        }
    }

}

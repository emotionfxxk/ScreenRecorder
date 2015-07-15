package com.mindarc.screenrecorder.utils;

import java.io.BufferedReader;
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
        LogUtil.i(MODULE_TAG, "requestRootPermission");
        int result = -1;
        Runtime runtime = Runtime.getRuntime();
        OutputStreamWriter osw = null;
        try {
            Process proc = runtime.exec("su");
            osw = new OutputStreamWriter(proc.getOutputStream());
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
            LogUtil.i(MODULE_TAG, "result:" + result + ", successMsg:" + successMsg +
                    ", errorMsg:" + errorMsg);
            //return new Result(result, successMsg.toString(), errorMsg.toString());
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(MODULE_TAG, "Command resulted in an IO Exception: " + e.getMessage());
            //return new Result(result);
        } finally {
            if (osw != null) {
                try {
                    osw.close();
                }
                catch (IOException e){}
            }
        }

        return (result == 0);
    }

    public static Result execCommandAsSu(String command) {
        LogUtil.i(MODULE_TAG, "execCommandAsSu: " + command);
        int result = -1;
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        OutputStreamWriter osw = null;
        try {
            proc = runtime.exec("su");
            osw = new OutputStreamWriter(proc.getOutputStream());
            osw.write(command + "\n");
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

    public static Result execCommand(String command) {
        LogUtil.i(MODULE_TAG, "execCommand: " + command);
        int result = -1;
        Runtime runtime = Runtime.getRuntime();
        try {
            Process proc = runtime.exec("command");
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
        }
    }

}

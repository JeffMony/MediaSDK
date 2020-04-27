package com.android.baselib.utils;

import android.util.Log;

public class LogUtils {
    private static final String TAG = "MediaSDK";
    private static final int ERROR_LEVEL = 5;
    private static final int WARN_LEVEL = 4;
    private static final int DEBUG_LEVEL = 3;
    private static final int INFO_LEVEL = 2;
    private static int sLogLevel = 2;

    public static void e(String msg) {
        if (sLogLevel <= ERROR_LEVEL) {
            Log.e(TAG, msg);
        }
    }

    public static void i(String msg) {
        if (sLogLevel <= INFO_LEVEL) {
            Log.i(TAG, msg);
        }
    }

    public static void d(String msg) {
        if (sLogLevel <= DEBUG_LEVEL) {
            Log.d(TAG, msg);
        }
    }

    public static void w(String msg) {
        if (sLogLevel <= WARN_LEVEL) {
            Log.w(TAG, msg);
        }
    }
}

package com.android.player.utils;

import android.util.Log;

public class LogUtils {
    private static final String TAG = "PlayerLib";
    private static final boolean sDebug = true;

    public static void e(String msg) {
        if (sDebug) {
            Log.e(TAG, msg);
        }
    }

    public static void i(String msg) {
        if (sDebug) {
            Log.i(TAG, msg);
        }
    }

    public static void d(String msg) {
        if (sDebug) {
            Log.d(TAG, msg);
        }
    }

    public static void w(String msg) {
        if (sDebug) {
            Log.w(TAG, msg);
        }
    }

    public static void v(String msg) {
        if (sDebug) {
            Log.v(TAG, msg);
        }
    }
}

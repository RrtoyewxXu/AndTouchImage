package com.rrtoyewx.touchimageviewlibrary.util;

import android.util.Log;

/**
 * Created by Rrtoyewx on 2016/11/2.
 */

public class L {
    private static final String TAG = "touch_image";
    private static boolean sDebug = true;

    public static void switchDebug(boolean debug) {
        sDebug = debug;
    }

    public static void e(String errorMessage) {
        if (sDebug) {
            Log.e(TAG, errorMessage);
        }
    }

    public static void a(String assertMessage) {
        if (sDebug) {
            Log.e(TAG, assertMessage);
        }
    }

    public static void d(String debugMessage) {
        if (sDebug) {
            Log.d(TAG, debugMessage);
        }
    }

    public static void w(String warnMessage) {
        if (sDebug) {
            Log.w(TAG, warnMessage);
        }
    }

    public static void i(String infoMessage) {
        if (sDebug) {
            Log.i(TAG, infoMessage);
        }
    }
}

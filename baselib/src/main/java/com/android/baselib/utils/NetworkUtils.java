package com.android.baselib.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class NetworkUtils {

    public static final int CONNECTION_TYPE_NULL = 0;
    public static final int CONNECTION_TYPE_WIFI = 2;
    public static final int CONNECTION_TYPE_4G = 3;
    public static final int CONNECTION_TYPE_3G = 4;
    public static final int CONNECTION_TYPE_2G = 5;

    @SuppressWarnings({"MissingPermission"})
    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null) {
                return networkInfo.isAvailable();
            }
        }
        return false;
    }

    @SuppressWarnings({"MissingPermission"})
    public static boolean isWifiConnected(Context context) {
        if (context != null) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return networkInfo.isAvailable();
            }
        }
        return false;
    }

    @SuppressWarnings({"MissingPermission"})
    public static boolean isMobileConnected(Context context) {
        if (context != null) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                return networkInfo.isAvailable();
            }
        }
        return false;
    }

    @SuppressWarnings({"MissingPermission"})
    public static int getAPNType(Context context) {
        int netType = CONNECTION_TYPE_NULL;
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return CONNECTION_TYPE_NULL;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_WIFI) {
            netType = CONNECTION_TYPE_WIFI;
        } else if (nType == ConnectivityManager.TYPE_MOBILE) {
            int nSubType = networkInfo.getSubtype();
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (nSubType == TelephonyManager.NETWORK_TYPE_LTE
                    && !telephonyManager.isNetworkRoaming()) {
                netType = CONNECTION_TYPE_4G;
            } else if (nSubType == TelephonyManager.NETWORK_TYPE_UMTS
                    || nSubType == TelephonyManager.NETWORK_TYPE_HSDPA
                    || nSubType == TelephonyManager.NETWORK_TYPE_EVDO_0
                    && !telephonyManager.isNetworkRoaming()) {
                netType = CONNECTION_TYPE_3G;
            } else if (nSubType == TelephonyManager.NETWORK_TYPE_GPRS
                    || nSubType == TelephonyManager.NETWORK_TYPE_EDGE
                    || nSubType == TelephonyManager.NETWORK_TYPE_CDMA
                    && !telephonyManager.isNetworkRoaming()) {
                netType = CONNECTION_TYPE_2G;
            } else {
                netType = CONNECTION_TYPE_2G;
            }
        }
        return netType;
    }

    public static String getNetworkType(Context context) {
        int apnType = getAPNType(context);
        if (apnType == CONNECTION_TYPE_NULL) {
            return "NULL";
        } else if (apnType == CONNECTION_TYPE_WIFI) {
            return "WIFI";
        } else if (apnType == CONNECTION_TYPE_4G) {
            return "4G";
        } else if (apnType == CONNECTION_TYPE_3G) {
            return "3G";
        } else if (apnType == CONNECTION_TYPE_2G) {
            return "2G";
        }
        return "NULL";
    }
}

package com.android.baselib.utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utility {

    public static String getSize(long size) {
        StringBuffer bytes = new StringBuffer();
        DecimalFormat format = new DecimalFormat("###.0");
        if (size >= 1024 * 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0 * 1024.0));
            bytes.append(format.format(i)).append("GB");
        } else if (size >= 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0));
            bytes.append(format.format(i)).append("MB");
        } else if (size >= 1024) {
            double i = (size / (1024.0));
            bytes.append(format.format(i)).append("KB");
        } else if (size < 1024) {
            if (size <= 0) {
                bytes.append("0B");
            } else {
                bytes.append((int) size).append("B");
            }
        }
        return bytes.toString();
    }

    public static String getTime(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
        String formatStr = formatter.format(new Date(time));
        return formatStr;
    }

    public static String getTimeShow(long timeMillis) {

        String timeShow;
        float time = timeMillis / 1000f / 60f;
        if (time < 1) {
            time = timeMillis / 1000f;
            timeShow = time + " s ";
        } else {
            timeShow = time + " min ";
        }
        return timeShow;
    }
}

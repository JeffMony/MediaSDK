package com.android.baselib.utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utility {

    public static String getSize(long size) {
        StringBuffer sb = new StringBuffer();
        DecimalFormat format = new DecimalFormat("###.00");
        if (size >= 1024 * 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0 * 1024.0));
            sb.append(format.format(i)).append("GB");
        } else if (size >= 1024 * 1024) {
            double i = (size / (1024.0 * 1024.0));
            sb.append(format.format(i)).append("MB");
        } else if (size >= 1024) {
            double i = (size / (1024.0));
            sb.append(format.format(i)).append("KB");
        } else if (size < 1024) {
            if (size <= 0) {
                sb.append("0B");
            } else {
                sb.append((int) size).append("B");
            }
        }
        return sb.toString();
    }

    public static String getPercent(float percent) {
        DecimalFormat format = new DecimalFormat("###.00");
        return format.format(percent) + "%";
    }

    public static String getTime(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
        String formatStr = formatter.format(new Date(time));
        return formatStr;
    }

    public static String getVideoTimeString(long duration) {
        duration /= 1000;
        String DateTimes = null;
        long hours = (duration % ( 60 * 60 * 24)) / (60 * 60);
        long minutes = (duration % ( 60 * 60)) /60;
        long seconds = duration % 60;

        DateTimes=String.format("%02d:", hours)+ String.format("%02d:", minutes) + String.format("%02d", seconds);
        String.format("%2d:", hours);
        return DateTimes;
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

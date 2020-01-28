package com.media.cache.utils;

public class LocalProxyUtils {

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
}

package com.media.cache;

import com.android.baselib.utils.LogUtils;

import java.io.File;

public class CacheManager {

    public static void deleteCacheFile() {
        File file = new File(getCachePath());
        LogUtils.w("deleteCacheFile file path = " + file.getAbsolutePath());
        deleteCacheFile(file);
    }

    private static void deleteCacheFile(File file) {
        LogUtils.w(""+file);
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for (File f : listFiles) {
                if (f.isDirectory()) {
                    deleteCacheFile(f);
                    f.delete();
                } else {
                    f.delete();
                }
            }
        } else {
            file.delete();
        }
    }

    public static String getCachedSize() {
        File file = new File(getCachePath());
        if (!file.exists()) {
            return "Null";
        }

        long totalSize = getFileSize(file);

        return totalSize / 1024 / 1024 + " MB";
    }

    private static long getFileSize(File file) {
        if (file == null) {
            return 0L;
        }
        long totalSize = 0L;
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for (File f : listFiles) {
                if (f.isDirectory()) {
                    totalSize += getFileSize(f);
                } else {
                    totalSize += f.length();
                }
            }
        } else {
            totalSize += file.length();
        }
        return totalSize;
    }

    public static String getCachePath() {
        return VideoDownloadManager.getInstance().getCacheFilePath();
    }

}

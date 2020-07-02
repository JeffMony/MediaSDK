package com.media.cache.utils;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class StorageUtils {

    public static File getVideoCacheDir(Context context) {
        return new File(context.getExternalCacheDir(), ".local");
    }

    public static void clearVideoCacheDir(Context context) throws IOException {
        File videoCacheDir = getVideoCacheDir(context);
        cleanDirectory(videoCacheDir);
    }

    private static void cleanDirectory(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        File[] contentFiles = file.listFiles();
        if (contentFiles != null) {
            for (File contentFile : contentFiles) {
                delete(contentFile);
            }
        }
    }

    public static void delete(File file)throws IOException {
        if (file.isFile() && file.exists()) {
            deleteOrThrow(file);
        } else {
            cleanDirectory(file);
            deleteOrThrow(file);
        }
    }

    private static void deleteOrThrow(File file) throws IOException {
        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (!isDeleted) {
                throw new IOException(
                        String.format("File %s can't be deleted", file.getAbsolutePath()));
            }
        }
    }

    public static List<File> getLruFileList(File dir) {
        List<File> result = new LinkedList<>();
        File[] files = dir.listFiles();
        if (files != null) {
            result = Arrays.asList(files);
            Collections.sort(result, new LastModifiedComparator());
        }
        return result;
    }

    private static final class LastModifiedComparator implements Comparator<File> {

        @Override
        public int compare(File lhs, File rhs) {
            return compareLong(lhs.lastModified(), rhs.lastModified());
        }

        private int compareLong(long first, long second) {
            return (first < second) ? -1 : ((first == second) ? 0 : 1);
        }
    }

    public static boolean deleteFile(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (!f.delete())
                    return false;
            }
            return file.delete();
        } else {
            return file.delete();
        }
    }

    public static void deleteCacheFile(File file) {
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
            file.delete();
        } else {
            file.delete();
        }
    }

    public static long countTotalSize(List<File> files) {
        long totalSize = 0;
        for (File file : files)  {
            totalSize += countTotalSize(file);
        }
        return totalSize;
    }

    public static long countTotalSize(File file) {
        if (file.isDirectory()) {
            long totalSize = 0;
            for (File f : file.listFiles()) {
                totalSize += f.length();
            }
            return totalSize;
        } else {
            return file.length();
        }
    }

}

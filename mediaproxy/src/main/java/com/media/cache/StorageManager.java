package com.media.cache;

import com.android.baselib.utils.LogUtils;
import com.media.cache.utils.LocalProxyThreadUtils;
import com.media.cache.utils.LocalProxyUtils;
import com.media.cache.utils.StorageUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

public class StorageManager {

    private static StorageManager sInstance = null;

    public static StorageManager getInstance() {
        if (sInstance == null) {
            synchronized (StorageManager.class) {
                if (sInstance == null) {
                    sInstance = new StorageManager();
                }
            }
        }
        return sInstance;
    }


    //1.Update cache file's last-modified-time.
    //2.Get LRU files.
    //3.Delete the files by LRU.
    public void checkCacheFile(File saveDir, long limitCacheSize) {
        try {
            LocalProxyThreadUtils.submitCallbackTask(new CheckFileCallable(saveDir, limitCacheSize));
        } catch (Exception e) {
            LogUtils.w("VideoDownloadTask checkCacheFile " + saveDir +" failed, exception="+e);
        }
    }

    private class CheckFileCallable implements Callable<Void> {

        private File mDir;
        private long mLimitCacheSize;

        public CheckFileCallable(File dir, long cacheSize) {
            mDir = dir;
            mLimitCacheSize = cacheSize;
        }

        @Override
        public Void call() throws Exception {
            LocalProxyUtils.setLastModifiedNow(mDir);
            trimCacheFile(mDir.getParentFile(), mLimitCacheSize);
            return null;
        }
    }

    private void trimCacheFile(File dir, long cacheSize) {
        List<File> files = StorageUtils.getLruFileList(dir);
        trimCacheFile(files, cacheSize);
    }

    private void trimCacheFile(List<File> files, long limitCacheSize) {
        long totalSize = StorageUtils.countTotalSize(files);
        int totalCount = files.size();
        for (File file : files) {
            boolean shouldDeleteFile = shouldDeleteFile(totalSize, totalCount, limitCacheSize);
            if (shouldDeleteFile) {
                long fileLength = StorageUtils.countTotalSize(file);
                boolean deleted = StorageUtils.deleteFile(file);
                if (deleted) {
                    totalSize -= fileLength;
                    totalCount--;
                    LogUtils.i("trimCacheFile okay.");
                } else {
                    LogUtils.w("trimCacheFile delete file " + file.getAbsolutePath() +" failed.");
                }
            }
        }
    }

    private boolean shouldDeleteFile(long totalSize, int totalCount, long limitCacheSize) {
        if (totalCount <= 1) {
            return false;
        }
        return totalSize > limitCacheSize;
    }
}

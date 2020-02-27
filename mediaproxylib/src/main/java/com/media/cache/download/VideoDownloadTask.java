package com.media.cache.download;

import com.android.baselib.utils.LogUtils;
import com.media.cache.LocalProxyConfig;
import com.media.cache.Video;
import com.media.cache.model.VideoCacheInfo;
import com.media.cache.listener.IDownloadTaskListener;
import com.media.cache.utils.LocalProxyThreadUtils;
import com.media.cache.utils.LocalProxyUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class VideoDownloadTask {

    protected static final int MSG_TASK_PAUSED = 0x100;
    private static final int DEFAULT_SLEEP_TIME_MILLIS = 5 * 100;
    protected static final int THREAD_COUNT = 3;
    protected static final int BUFFER_SIZE = LocalProxyUtils.DEFAULT_BUFFER_SIZE;

    protected ThreadPoolExecutor mDownloadExecutor;
    protected IDownloadTaskListener mDownloadTaskListener;
    protected volatile boolean mShouldSuspendDownloadTask = false;
    protected volatile boolean mIsPlaying = false;
    protected final LocalProxyConfig mConfig;
    protected final VideoCacheInfo mInfo;
    protected final String mFinalUrl;
    protected final HashMap<String, String> mHeaders;
    protected File mSaveDir;
    protected String mSaveName;
    protected Timer mTimer;
    protected long mOldCachedSize = 0L;
    protected long mCurrentCachedSize = 0L;
    protected float mPercent = 0.0f;

    protected volatile OPERATE_TYPE mType = OPERATE_TYPE.DEFAULT;
    protected enum OPERATE_TYPE {
        DEFAULT,
        WRITED,
    }

    protected VideoDownloadTask(LocalProxyConfig config,
                                VideoCacheInfo info,
                                HashMap<String, String> headers) {
        mConfig = config;
        mInfo = info;
        mHeaders = headers;
        mFinalUrl = info.getFinalUrl();
        mSaveName = LocalProxyUtils.computeMD5(info.getUrl());
        mSaveDir = new File(mConfig.getCacheRoot(), mSaveName);
        if (!mSaveDir.exists()) {
            mSaveDir.mkdir();
        }
        info.setSaveDir(mSaveDir.getAbsolutePath());
    }

    public void suspendDownload() {
        mShouldSuspendDownloadTask = true;
    }

    public void restoreDownload() {
        mShouldSuspendDownloadTask = false;
    }

    public void suspendDownloadTaskByCondition() {
        try {
            while (mShouldSuspendDownloadTask) {
                LogUtils.w("suspendDownloadTaskByCondition");
                Thread.sleep(DEFAULT_SLEEP_TIME_MILLIS);
            }
        } catch (Exception e) {
            LogUtils.w("determineDownloadState failed, exception="+e);
        }
    }

    public boolean isDownloadTaskPaused() {
        if (mInfo != null && mInfo.getIsCompleted()) {
            return true;
        }
        if (mDownloadExecutor != null) {
            return mDownloadExecutor.isShutdown();
        }
        return true;
    }

    protected void startTimerTask() {
        if (mTimer == null) {
            mTimer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (mOldCachedSize <= mCurrentCachedSize) {
                        float speed = (mCurrentCachedSize - mOldCachedSize) * 1.0f;
                        mDownloadTaskListener.onTaskSpeedChanged(speed);
                        mOldCachedSize = mCurrentCachedSize;
                    }
                }
            };
            mTimer.schedule(task, 0, LocalProxyUtils.UPDATE_INTERVAL);
        }
    }

    protected void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    protected void notifyOnTaskPaused() {
        if (mDownloadTaskListener != null) {
            mDownloadTaskListener.onTaskPaused();
            cancelTimer();
        }
    }

    public abstract void startDownload(IDownloadTaskListener listener);

    public abstract void resumeDownload();

    public abstract void seekToDownload(float seekPercent);

    public abstract void seekToDownload(long curPosition, long totalDuration);

    public abstract void seekToDownload(float seekPercent, IDownloadTaskListener callback);

    public abstract void seekToDownload(int curDownloadTs, IDownloadTaskListener callback);

    public abstract void seekToDownload(long curLength, IDownloadTaskListener callback);

    public abstract void pauseDownload();

    public abstract void stopDownload();


    //1.Update cache file's last-modified-time.
    //2.Get LRU files.
    //3.Delete the files by LRU.
    protected void checkCacheFile(File saveDir) {
        try {
            LocalProxyThreadUtils.submitCallbackTask(new CheckFileCallable(saveDir));
        } catch (Exception e) {
            LogUtils.w("VideoDownloadTask checkCacheFile " + saveDir +" failed, exception="+e);
        }
    }

    private class CheckFileCallable implements Callable<Void> {

        private File mDir;

        public CheckFileCallable(File dir) {
            mDir = dir;
        }

        @Override
        public Void call() throws Exception {
            LocalProxyUtils.setLastModifiedNow(mDir);
            trimCacheFile(mDir.getParentFile());
            return null;
        }
    }

    private void trimCacheFile(File dir) {
        List<File> files = LocalProxyUtils.getLruFileList(dir);
        trimCacheFile(files, mConfig.getCacheSize());
    }

    private void trimCacheFile(List<File> files, long limitCacheSize) {
        long totalSize = LocalProxyUtils.countTotalSize(files);
        int totalCount = files.size();
        for (File file : files) {
            boolean shouldDeleteFile = shouldDeleteFile(totalSize, totalCount, limitCacheSize);
            if (shouldDeleteFile) {
                long fileLength = LocalProxyUtils.countTotalSize(file);
                boolean deleted = LocalProxyUtils.deleteFile(file);
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

    protected boolean isFloatEqual(float f1, float f2) {
        if (Math.abs(f1-f2) < 0.001f) {
            return true;
        }
        return false;
    }
}


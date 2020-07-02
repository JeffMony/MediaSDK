package com.media.cache.download;

import com.media.cache.LocalProxyConfig;
import com.media.cache.model.VideoCacheInfo;
import com.media.cache.listener.IDownloadTaskListener;
import com.media.cache.utils.LocalProxyUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class VideoDownloadTask {

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
    protected float mSpeed = 0.0f;

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
                        mSpeed = speed;
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
}


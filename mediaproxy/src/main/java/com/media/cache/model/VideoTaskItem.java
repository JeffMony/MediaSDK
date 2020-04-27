package com.media.cache.model;

import androidx.annotation.Nullable;

import com.android.baselib.utils.Utility;
import com.media.cache.hls.M3U8;

public class VideoTaskItem {

    private String mUrl;
    private String mProxyUrl;
    private boolean mProxyReady;
    private M3U8 mM3U8;
    private float mSpeed;
    private float mPercent;
    private long mDownloadSize;
    private int mVideoType;
    private int mTaskState;
    private int mTaskMode;
    private long mDownloadTime;
    private int mErrorCode;

    public VideoTaskItem(String url, int mode) {
        mUrl = url;
        mTaskMode = mode;
        mTaskState = VideoTaskState.DEFAULT;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        mProxyUrl = proxyUrl;
        mProxyReady = true;
    }

    public String getProxyUrl() {
        return mProxyUrl;
    }

    public boolean getProxyReady() {
        return mProxyReady;
    }

    public void setM3U8(M3U8 m3u8) {
        mM3U8 = m3u8;
    }

    public M3U8 getM3U8() {
        return mM3U8;
    }

    public void setSpeed(float speed) {
        mSpeed = speed;
    }

    public float getSpeed() {
        return mSpeed;
    }

    public String getSpeedString() {
        return Utility.getSize((long)mSpeed) + "/s";
    }

    public void setPercent(float percent) {
        mPercent = percent;
    }

    public float getPercent() {
        return mPercent;
    }

    public String getPercentString() {
        return Utility.getPercent(mPercent);
    }

    public void setDownloadSize(long size) {
        mDownloadSize = size;
    }

    public long getDownloadSize() {
        return mDownloadSize;
    }

    public String getDownloadSizeString() {
        return Utility.getSize(mDownloadSize);
    }

    public void setVideoType(int type) {
        mVideoType = type;
    }

    public int getVideoType() {
        return mVideoType;
    }

    public void setTaskState(int state) {
        mTaskState = state;
    }

    public int getTaskState() {
        return mTaskState;
    }

    public void setTaskMode(int mode) { mTaskMode = mode; }

    public int getTaskMode() { return mTaskMode; }

    public void setDownloadTime(long time) {
        mDownloadTime = time;
    }

    public long getDownloadTime() {
        return mDownloadTime;
    }

    public boolean isDownloadMode() {
        return getTaskMode() == VideoTaskMode.DOWNLOAD_MODE;
    }

    public boolean isPlayMode() {
        return getTaskMode() == VideoTaskMode.PLAY_MODE;
    }

    public boolean isRunningTask() {
        return mTaskState == VideoTaskState.DOWNLOADING || mTaskMode == VideoTaskState.PROXYREADY;
    }

    public boolean isSlientTask() {
        return mTaskState == VideoTaskState.DEFAULT || mTaskState == VideoTaskState.PAUSE || mTaskState == VideoTaskState.ERROR;
    }

    public void setErrorCode(int errorCode) {
        mErrorCode = errorCode;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj != null && obj instanceof VideoTaskItem) {
            String objUrl = ((VideoTaskItem)obj).getUrl();
            if (mUrl.equals(objUrl)) {
                return true;
            }
        }
        return false;
    }
}

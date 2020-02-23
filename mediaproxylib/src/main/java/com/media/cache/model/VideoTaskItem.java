package com.media.cache.model;

import androidx.annotation.Nullable;

import com.android.baselib.utils.Utility;
import com.media.cache.hls.M3U8;

public class VideoTaskItem {

    private String mUrl;
    private boolean mIsDownloadMode;
    private String mProxyUrl;
    private boolean mProxyReady;
    private M3U8 mM3U8;
    private float mSpeed;
    private float mPercent;
    private long mDownloadSize;
    private int mType;
    private int mTaskState;

    public VideoTaskItem(String url) {
        this(url, false);

    }

    public VideoTaskItem(String url, boolean isDownloadMode) {
        mUrl = url;
        mIsDownloadMode = isDownloadMode;
        mTaskState = VideoTaskState.DEFAULT;
    }

    public String getUrl() {
        return mUrl;
    }

    public boolean isDownloadMode() {
        return mIsDownloadMode;
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

    public void setType(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }

    public void setTaskState(int state) {
        mTaskState = state;
    }

    public int getTaskState() {
        return mTaskState;
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

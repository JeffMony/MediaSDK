package com.media.cache.model;

import androidx.annotation.Nullable;

public class VideoItem {
    private String mUrl;
    private String mProxyUrl;
    private float mProgress;
    private long mSize;
    private int mState;
    private float mSpeed;

    public VideoItem(String url) {
        mUrl = url;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        mProxyUrl = proxyUrl;
    }

    public String getProxyUrl() { return mProxyUrl; }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float mProgress) {
        this.mProgress = mProgress;
    }

    public long getSize() {
        return mSize;
    }

    public void setSize(long mSize) {
        this.mSize = mSize;
    }

    public int getState() {
        return mState;
    }

    public void setState(int mState) {
        this.mState = mState;
    }

    public float getSpeed() {
        return mSpeed;
    }

    public void setSpeed(float mSpeed) {
        this.mSpeed = mSpeed;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof VideoItem) {
            if (obj != null && ((VideoItem) obj).getUrl().equals(mUrl)) {
                return true;
            }
        }
        return false;
    }
}

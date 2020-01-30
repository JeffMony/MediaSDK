package com.media.cache.hls;

import com.media.cache.utils.LocalProxyUtils;

import java.util.Locale;

public class M3U8Ts implements Comparable<M3U8Ts> {

    private float mDuration;
    private int mIndex;
    private String mUrl;
    private String mName;
    private long mTsSize;
    private boolean mHasDiscontinuity;
    private boolean mHasKey;
    private String mMethod;
    private String mKeyUri;
    private String mKeyIV;
    private boolean mIsMessyKey;

    public M3U8Ts() {
    }

    public void initTsAttributes(String url, float duration, int index, boolean hasDiscontinuity, boolean hasKey) {
        this.mUrl = url;
        this.mName = url;
        this.mDuration = duration;
        this.mIndex = index;
        this.mHasDiscontinuity = hasDiscontinuity;
        this.mHasKey = hasKey;
        this.mTsSize = 0L;
    }

    public void setKeyConfig(String method, String keyUri, String keyIV) {
        this.mMethod = method;
        this.mKeyUri = keyUri;
        this.mKeyIV = keyIV;
    }

    public boolean hasKey() {
        return mHasKey;
    }

    public String getMethod() {
        return mMethod;
    }

    public String getKeyUri() {
        return mKeyUri;
    }

    public String getLocalKeyUri() {
        return "local.key";
    }

    public String getKeyIV() {
        return mKeyIV;
    }

    public float getDuration() {
        return mDuration;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getName() {
        return mName;
    }

    /**
     * if ts is local file, name is video_{index}.ts
     * if ts is network resource , name is starting with http or https.
     * @param name
     */
    public void setName(String name) {
        this.mName = name;
    }

    public String getIndexName() {
        return "seg_" + mIndex+".ts";
    }

    public String getProxyUrl(String host, int port, String fileName) {
        return String.format(Locale.US, "http://%s:%d/%s%s/%s/%s", host, port, LocalProxyUtils.encodeUri(mUrl), LocalProxyUtils.SPLIT_STR, fileName, getIndexName());
    }

    public void setTsSize(long tsSize) {
        this.mTsSize = tsSize;
    }

    public long getTsSize() {
        return mTsSize;
    }

    public boolean hasDiscontinuity() {
        return mHasDiscontinuity;
    }

    public void setIsMessyKey(boolean isMessyKey) {
        this.mIsMessyKey = isMessyKey;
    }

    public boolean isMessyKey() {
        return mIsMessyKey;
    }

    public String toString() {
        return "duration="+mDuration+", index="+mIndex+", name="+mName;
    }

    @Override
    public int compareTo(M3U8Ts object) {
        return mName.compareTo(object.mName);
    }
}

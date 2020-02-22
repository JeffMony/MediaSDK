package com.media.cache.model;

import java.io.Serializable;
import java.util.LinkedHashMap;

public class VideoCacheInfo implements Serializable {

    private String mUrl; // Orignal url
    private String mFinalUrl; // Final url by redirecting.
    private boolean mIsCompleted;
    private int mType;
    private long mCachedLength;
    private long mTotalLength;
    private int mCachedTs;
    private int mTotalTs;
    private String mSaveDir;
    private int mPort;
    private LinkedHashMap<Long, Long> mSegmentList; // save the video segements' info.

    public VideoCacheInfo(String videoUrl) {
        super();
        mUrl = videoUrl;
        mTotalLength = -1L;
        mType = -1;
        mSegmentList = new LinkedHashMap<>();
    }

    public void setUrl(String videoUrl) {
        mUrl = videoUrl;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setFinalUrl(String finalUrl) { this.mFinalUrl = finalUrl; }

    public String getFinalUrl() { return mFinalUrl; }

    public void setIsCompleted(boolean isCompleted) {
        this.mIsCompleted = isCompleted;
    }

    public boolean getIsCompleted() {
        return mIsCompleted;
    }

    public void setVideoType(int videoType) {
        this.mType = videoType;
    }

    public int getVideoType() {
        return mType;
    }

    public void setCachedLength(long cachedLength) {
        this.mCachedLength = cachedLength;
    }

    public long getCachedLength() {
        return mCachedLength;
    }

    public void setTotalLength(long totalLength) {
        this.mTotalLength = totalLength;
    }

    public long getTotalLength() {
        return mTotalLength;
    }

    public void setCachedTs(int cachedTs) {
        this.mCachedTs = cachedTs;
    }

    public int getCachedTs() {
        return mCachedTs;
    }

    public void setTotalTs(int totalTs) {
        this.mTotalTs = totalTs;
    }

    public int getTotalTs() {
        return mTotalTs;
    }

    public void setSaveDir(String saveDir) {
        this.mSaveDir = saveDir;
    }

    public String getSaveDir() {
        return mSaveDir;
    }

    public void setPort(int port) { mPort = port; }

    public int getPort() { return mPort; }

    public void setSegmentList(LinkedHashMap<Long, Long> list) {
        this.mSegmentList = list;
    }

    public LinkedHashMap<Long, Long> getSegmentList() {
        return mSegmentList;
    }

    public String toString() {
        return "VideoCacheInfo[url="+mUrl+", complete="+mIsCompleted+", type="+mType
                +", cachedLength="+mCachedLength+", totalLength=" +mTotalLength+", cachedTs="+mCachedTs
                +", totalTs="+mTotalTs+", saveDir="+mSaveDir+", segmentSize=" + mSegmentList.size() +"]";
    }
}

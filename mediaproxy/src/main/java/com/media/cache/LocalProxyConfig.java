package com.media.cache;

import android.content.Context;

import java.io.File;

public class LocalProxyConfig {

    private boolean mIsDebug = false;
    private Context mContext;
    private File mCacheRoot;
    private String mHost;
    private int mPort;
    private long mCacheSize;
    private int mReadTimeOut;
    private int mConnTimeOut;
    private int mSocketTimeOut;
    private boolean mRedirect;
    private boolean mFlowControlEnable;
    private long mMaxBufferSize;
    private long mMinBufferSize;
    private boolean mIgnoreAllCertErrors;
    private int mConcurrentCount;

    public LocalProxyConfig(Context context, File cacheRoot,
                                 long cacheSize, int readTimeOut,
                                 int connTimeOut, int socketTimeOut,
                                 boolean redirect, boolean ignoreAllCertErrors, int port,
                                 boolean flowControlEnable, long maxBufferSize,
                                 long minBufferSize, int count) {
        mContext = context;
        mCacheRoot = cacheRoot;
        mCacheSize = cacheSize;
        mReadTimeOut = readTimeOut;
        mConnTimeOut = connTimeOut;
        mSocketTimeOut = socketTimeOut;
        mRedirect = redirect;
        mIgnoreAllCertErrors = ignoreAllCertErrors;
        mPort = port;
        mFlowControlEnable = flowControlEnable;
        mMaxBufferSize = maxBufferSize;
        mMinBufferSize = minBufferSize;
        mConcurrentCount = count;
    }

    public Context getContext() { return mContext; }

    public int getConnTimeOut() {
        return mConnTimeOut;
    }

    public int getReadTimeOut() {
        return mReadTimeOut;
    }

    public int getSocketTimeOut() { return mSocketTimeOut; }

    public long getCacheSize() {
        return mCacheSize;
    }

    public int getPort() {
        return mPort;
    }

    public String getHost() {
        return mHost;
    }

    public void setConfig(String host, int port) {
        mHost = host;
        mPort = port;
    }

    public File getCacheRoot() {
        return mCacheRoot;
    }

    public boolean isRedirect() { return mRedirect; }

    public void setFlowControlEnable(boolean enable) {
        mFlowControlEnable = enable;
    }

    public boolean getFlowControlEnable() {
        return mFlowControlEnable;
    }

    public long getMaxBufferSize() { return mMaxBufferSize; }

    public long getMinBufferSize() { return mMinBufferSize; }

    public boolean isDebug() { return mIsDebug; }

    public void setIgnoreAllCertErrors(boolean enable) {
        mIgnoreAllCertErrors = enable;
    }

    public boolean shouldIgnoreAllCertErrors() {
        return mIgnoreAllCertErrors;
    }

    public void setConcurrentCount(int count) { mConcurrentCount = count; }

    public int getConcurrentCount() { return mConcurrentCount; }
}

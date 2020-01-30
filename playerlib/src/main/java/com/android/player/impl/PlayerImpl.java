package com.android.player.impl;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;

import com.android.player.IPlayer;
import com.android.player.PlayerAttributes;
import com.android.player.proxy.LocalProxyPlayerImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PlayerImpl implements IPlayer {

    private OnPreparedListener mOnPreparedListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnLocalProxyCacheListener mOnLocalProxyCacheListener;

    protected LocalProxyPlayerImpl mLocalProxyPlayerImpl;

    protected String mUrl;

    //Player settings
    protected boolean mUseLocalProxy = false;

    public PlayerImpl(Context context, PlayerAttributes attributes) {

        if (attributes != null) {
            mUseLocalProxy = attributes.userLocalProxy();
        }
        if (mUseLocalProxy) {
            mLocalProxyPlayerImpl = new LocalProxyPlayerImpl(this);
        }
    }

    @Override
    public void startLocalProxy(String url, HashMap<String, String> headers) {
        if (mUseLocalProxy && mLocalProxyPlayerImpl != null) {
            mLocalProxyPlayerImpl.startLocalProxy(url, headers);
        }
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {

    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {

    }

    @Override
    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {

    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {

    }

    @Override
    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {

    }

    @Override
    public void setSurface(Surface surface) {

    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        this.mOnPreparedListener = listener;
    }

    @Override
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        this.mOnVideoSizeChangedListener = listener;
    }

    @Override
    public void setOnLocalProxyCacheListener(OnLocalProxyCacheListener listener) {
        this.mOnLocalProxyCacheListener = listener;
    }

    @Override
    public void prepareAsync() throws IllegalStateException {

    }

    @Override
    public void start() throws IllegalStateException {
        if (mUseLocalProxy && mLocalProxyPlayerImpl != null) {
            mLocalProxyPlayerImpl.doStartAction();
        }
    }

    @Override
    public void pause() throws IllegalStateException {
        if (mUseLocalProxy && mLocalProxyPlayerImpl != null) {
            mLocalProxyPlayerImpl.doPauseAction();
        }
    }

    @Override
    public void stop() throws IllegalStateException {

    }

    @Override
    public void release() {
        if (mUseLocalProxy && mLocalProxyPlayerImpl != null) {
            mLocalProxyPlayerImpl.doReleaseAction();
        }
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        if (mUseLocalProxy && mLocalProxyPlayerImpl != null) {
            mLocalProxyPlayerImpl.doSeekToAction(msec);
        }
    }

    @Override
    public long getCurrentPosition() {
        return 0;
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    protected void notifyOnPrepared() {
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(this);
        }
    }

    protected void notifyOnVideoSizeChanged(int width, int height,
                                            int rotationDegree,
                                            float pixelRatio) {
        if (mOnVideoSizeChangedListener != null) {
            mOnVideoSizeChangedListener.onVideoSizeChanged(this, width, height, rotationDegree, pixelRatio);
        }
    }

    public void notifyProxyCacheReady(String proxyUrl) {
        if (mOnLocalProxyCacheListener != null) {
            mOnLocalProxyCacheListener.onCacheReady(this, proxyUrl);
        }
    }

    public void notifyProxyCacheProgress(int percent, long cachedSize) {
        if (mOnLocalProxyCacheListener != null) {
            mOnLocalProxyCacheListener.onCacheProgressChanged(this, percent, cachedSize);
        }
    }

    public void notifyProxyCacheForbidden(String url) {
        if (mOnLocalProxyCacheListener != null) {
            mOnLocalProxyCacheListener.onCacheForbidden(this, url);
        }
    }
}

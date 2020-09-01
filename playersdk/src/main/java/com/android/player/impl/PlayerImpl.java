package com.android.player.impl;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Surface;

import com.android.baselib.WeakHandler;
import com.android.player.IPlayer;
import com.android.player.PlayerAttributes;
import com.android.player.proxy.LocalProxyPlayerImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class PlayerImpl implements IPlayer {

    private OnPreparedListener mOnPreparedListener;
    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnErrorListener mOnErrorListener;
    private OnLocalProxyCacheListener mOnLocalProxyCacheListener;

    protected LocalProxyPlayerImpl mLocalProxyPlayerImpl;

    protected String mUrl;

    protected String mOriginUrl;

    //Player settings
    protected boolean mVideoCacheSwitch = false;

    public PlayerImpl(Context context, PlayerAttributes attributes) {
        applyPlayerAttr(attributes);
        if (mVideoCacheSwitch) {
            mLocalProxyPlayerImpl = new LocalProxyPlayerImpl(this);
        }
    }

    protected void applyPlayerAttr(PlayerAttributes attributes) {
        if (attributes == null) {
            return;
        }
        mVideoCacheSwitch = attributes.videoCacheSwitch();
        mUrl = attributes.getVideoUrl();
    }

    @Override
    public void startLocalProxy(String url) {
        if (mVideoCacheSwitch && mLocalProxyPlayerImpl != null) {
            mLocalProxyPlayerImpl.startLocalProxy(url);
        }
    }

    @Override
    public void setOriginUrl(String url) {
        mOriginUrl = url;
        if (mVideoCacheSwitch && mLocalProxyPlayerImpl != null) {
            mLocalProxyPlayerImpl.setCacheListener(mOriginUrl);
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
    public void setOnErrorListener(OnErrorListener listener) {
        this.mOnErrorListener = listener;
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
        if (mVideoCacheSwitch && mLocalProxyPlayerImpl != null) {
            mLocalProxyPlayerImpl.doStartAction();
        }
    }

    @Override
    public void openPlay(PlayerAttributes attributes) {
        applyPlayerAttr(attributes);
        if (TextUtils.isEmpty(mUrl)) return;
        if (mVideoCacheSwitch) {
            mLocalProxyPlayerImpl.startLocalProxy(mUrl);
        } else {
            doOpenPlay(mUrl);
        }
    }

    public abstract void doOpenPlay(String url);

    @Override
    public void pause() throws IllegalStateException {
        if (mVideoCacheSwitch && mLocalProxyPlayerImpl != null) {
            mLocalProxyPlayerImpl.doPauseAction();
        }
    }

    @Override
    public void setSpeed(float speed) {
        
    }

    @Override
    public void stop() throws IllegalStateException {

    }

    @Override
    public void release() {
        if (mVideoCacheSwitch && mLocalProxyPlayerImpl != null) {
            mLocalProxyPlayerImpl.doReleaseAction();
        }
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        if (mVideoCacheSwitch && mLocalProxyPlayerImpl != null) {
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
                                            float pixelRatio,
                                            float darRatio) {
        if (mOnVideoSizeChangedListener != null) {
            mOnVideoSizeChangedListener.onVideoSizeChanged(this, width, height, rotationDegree, pixelRatio, darRatio);
        }
    }

    protected void notifyOnError(int what, String msg) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(this, what, msg);
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

    public void notifyProxyCacheSpeed(float speed) {
        if (mOnLocalProxyCacheListener != null) {
            mOnLocalProxyCacheListener.onCacheSpeedChanged(this, speed);
        }
    }

    public void notifyProxyCacheForbidden(String url) {
        if (mOnLocalProxyCacheListener != null) {
            mOnLocalProxyCacheListener.onCacheForbidden(this, url);
        }
    }

    public void notifyProxyCacheFinished() {
        if (mOnLocalProxyCacheListener != null) {
            mOnLocalProxyCacheListener.onCacheFinished(this);
        }
    }
}

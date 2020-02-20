package com.android.player.proxy;

import com.android.baselib.utils.LogUtils;
import com.android.player.impl.PlayerImpl;
import com.media.cache.hls.M3U8;
import com.media.cache.listener.IVideoProxyCacheCallback;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class LocalProxyPlayerImpl {

    private static final int NO_PAUSED = 0;
    private static final int WIFI_PRELOAD_CONTROL = 1;
    private static final int PLAYER_PAUSE_CONTROL = 2;
    private static final int MOBILE_FLOW_CONTROL = 3;
    private static final int PROXY_CACHE_EXCEPTION = 4;
    private int mPausedReason = NO_PAUSED; // initial value or no paused state.

    private WeakReference<PlayerImpl> mPlayer;
    private boolean mUseLocalProxy = true;
    private boolean mIsCompleteCached = false; //Video has been cached completely.
    private String mUrl;
    private boolean mVideoReady = false;
    private int mCachedPercent = 0;
    private long mCachedSize = 0L;
    private M3U8 mM3U8 = null;

    public LocalProxyPlayerImpl(PlayerImpl player) {
        mPlayer = new WeakReference<>(player);
    }

    public void startLocalProxy(String url, HashMap<String, String> headers) {
        mUrl = url;
        LocalProxyCacheManager.getInstance().addCallback(url, mVideoProxyCacheCallback);
        LocalProxyCacheManager.getInstance().startEngine(url, headers);
    }

    public void doStartAction() {
        if (mUseLocalProxy) {
            if (isProxyCacheTaskPaused()) {
                resumeProxyCacheTask();
            }
        }
    }

    public void doSeekToAction(long seekPosition) {
        if (mUseLocalProxy) {
            if (mPlayer == null || mPlayer.get() == null)
                return;
            long totalDuration = mPlayer.get().getDuration();
            if (totalDuration > 0) {
                LogUtils.i("doSeekToAction seekPosition="+seekPosition);
                mPausedReason = NO_PAUSED;
                LocalProxyCacheManager.getInstance().seekToDownloadTask(seekPosition, totalDuration, mUrl);
            }
        }
    }

    public void doPauseAction() {
        if (mUseLocalProxy) {
            pauseProxyCacheTask(PLAYER_PAUSE_CONTROL);
        }
    }

    public void doReleaseAction() {
        if (mUseLocalProxy) {
            LogUtils.i("doReleaseAction player="+this);
            LocalProxyCacheManager.getInstance().stopDownloadTask(mUrl);
            LocalProxyCacheManager.getInstance().removeCallback(mUrl,
                    mVideoProxyCacheCallback);
        }
    }

    public void pauseProxyCacheTask(final int reason) {
        if (mIsCompleteCached) {
            LocalProxyCacheManager.getInstance().removeCallback(mUrl, mVideoProxyCacheCallback);
            return;
        }
        //Do pauseProxyCacheTask when state is not paused.
        if (!isProxyCacheTaskPaused()) {
            mPausedReason = reason;
            LocalProxyCacheManager.getInstance().pauseDownloadTask(mUrl);
        }
    }

    public void resumeProxyCacheTask() {
        if (mIsCompleteCached) {
            return;
        }
        if (isProxyCacheTaskPaused()) {
            LogUtils.i("resumeProxyCacheTask url="+mUrl);
            mPausedReason = NO_PAUSED;
            LocalProxyCacheManager.getInstance().addCallback(mUrl, mVideoProxyCacheCallback);
            LocalProxyCacheManager.getInstance().resumeDownloadTask(mUrl);
        }
    }

    private IVideoProxyCacheCallback mVideoProxyCacheCallback = new IVideoProxyCacheCallback() {
        @Override
        public void onCacheReady(String url, String proxyUrl) {
            if (!mVideoReady && mPlayer != null && mPlayer.get() != null) {
                mPlayer.get().notifyProxyCacheReady(proxyUrl);
                mVideoReady = true;
            }
        }

        @Override
        public void onCacheProgressChanged(String url, int percent,
                                           long cachedSize, M3U8 m3u8) {
            mCachedPercent = percent;
            mCachedSize = cachedSize;
            mM3U8 = m3u8;
            if (mPlayer != null && mPlayer.get() != null) {
                mPlayer.get().notifyProxyCacheProgress(percent, cachedSize);
            }
        }

        @Override
        public void onCacheSpeedChanged(String url, float cacheSpeed) {
            if (mPlayer != null && mPlayer.get() != null) {
                mPlayer.get().notifyProxyCacheSpeed(cacheSpeed);
            }
        }

        @Override
        public void onCacheFinished(String url) {
            LogUtils.i("onCacheFinished url="+url + ", player="+this);
            mIsCompleteCached = true;
        }

        @Override
        public void onCacheForbidden(String url) {
            LogUtils.w("onCacheForbidden url="+url+", player="+this);
            mUseLocalProxy = false;
            if (mPlayer != null && mPlayer.get() != null) {
                mPlayer.get().notifyProxyCacheForbidden(url);
            }
        }

        @Override
        public void onCacheFailed(String url, Exception e) {
            LogUtils.w("onCacheFailed , player="+this);
            pauseProxyCacheTask(PROXY_CACHE_EXCEPTION);
        }
    };

    private boolean isProxyCacheTaskPaused() {
        return mPausedReason != NO_PAUSED;
    }

}

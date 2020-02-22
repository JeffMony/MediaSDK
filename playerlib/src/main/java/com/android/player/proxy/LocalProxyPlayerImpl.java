package com.android.player.proxy;

import com.android.baselib.utils.LogUtils;
import com.android.player.impl.PlayerImpl;
import com.media.cache.VideoDownloadManager;
import com.media.cache.hls.M3U8;
import com.media.cache.listener.IDownloadListener;
import com.media.cache.model.VideoTaskItem;

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
        VideoTaskItem item = new VideoTaskItem(url);
        VideoDownloadManager.getInstance().startDownload(item, headers, mDownloadListener);
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
                VideoDownloadManager.getInstance().seekToDownloadTask(seekPosition, totalDuration, mUrl);
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
            VideoDownloadManager.getInstance().stopDownloadTask(mUrl);
        }
    }

    public void pauseProxyCacheTask(final int reason) {
        if (mIsCompleteCached) {
            VideoDownloadManager.getInstance().removeCallback(mUrl);
            return;
        }
        //Do pauseProxyCacheTask when state is not paused.
        if (!isProxyCacheTaskPaused()) {
            mPausedReason = reason;
            VideoDownloadManager.getInstance().pauseDownloadTask(mUrl);
        }
    }

    public void resumeProxyCacheTask() {
        if (mIsCompleteCached) {
            return;
        }
        if (isProxyCacheTaskPaused()) {
            LogUtils.i("resumeProxyCacheTask url="+mUrl);
            mPausedReason = NO_PAUSED;
            VideoDownloadManager.getInstance().resumeDownloadTask(mUrl, mDownloadListener);
        }
    }

    private IDownloadListener mDownloadListener = new IDownloadListener() {
        @Override
        public void onDownloadPrepare(VideoTaskItem item) {

        }

        @Override
        public void onDownloadPending(VideoTaskItem item) {

        }

        @Override
        public void onDownloadStart(VideoTaskItem item) {

        }

        @Override
        public void onDownloadProxyReady(VideoTaskItem item) {
            if (!mVideoReady && mPlayer != null && mPlayer.get() != null) {
                mPlayer.get().notifyProxyCacheReady(item.getProxyUrl());
                mVideoReady = true;
            }
        }

        @Override
        public void onDownloadProgress(VideoTaskItem item) {
            mCachedPercent = (int)item.getPercent();
            mCachedSize = item.getDownloadSize();
            mM3U8 = item.getM3U8();
            if (mPlayer != null && mPlayer.get() != null) {
                mPlayer.get().notifyProxyCacheProgress(mCachedPercent, mCachedSize);
            }
        }

        @Override
        public void onDownloadSpeed(VideoTaskItem item) {
            if (mPlayer != null && mPlayer.get() != null) {
                mPlayer.get().notifyProxyCacheSpeed(item.getSpeed());
            }
        }

        @Override
        public void onDownloadPause(VideoTaskItem item) {

        }

        @Override
        public void onDownloadError(VideoTaskItem item) {
            LogUtils.w("onDownloadError , player="+this);
            pauseProxyCacheTask(PROXY_CACHE_EXCEPTION);
        }

        @Override
        public void onDownloadProxyForbidden(VideoTaskItem item) {
            LogUtils.w("onCacheForbidden url="+item.getUrl()+", player="+this);
            mUseLocalProxy = false;
            if (mPlayer != null && mPlayer.get() != null) {
                mPlayer.get().notifyProxyCacheForbidden(item.getUrl());
            }
        }

        @Override
        public void onDownloadSuccess(VideoTaskItem item) {
            LogUtils.i("onDownloadSuccess url="+item.getUrl() + ", player="+this);
            mIsCompleteCached = true;
        }
    };

    private boolean isProxyCacheTaskPaused() {
        return mPausedReason != NO_PAUSED;
    }

}

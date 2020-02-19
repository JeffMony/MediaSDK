package com.android.player.proxy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.android.netlib.NetworkCallbackImpl;
import com.android.netlib.NetworkListener;
import com.android.netlib.utils.LogUtils;
import com.media.cache.LocalProxyConfig;
import com.media.cache.Video;
import com.media.cache.VideoCacheInfo;
import com.media.cache.VideoInfoParserManager;
import com.media.cache.download.EntireVideoDownloadTask;
import com.media.cache.download.M3U8VideoDownloadTask;
import com.media.cache.download.VideoDownloadTask;
import com.media.cache.hls.M3U8;
import com.media.cache.listener.IVideoInfoCallback;
import com.media.cache.listener.IVideoInfoParseCallback;
import com.media.cache.listener.IVideoProxyCacheCallback;
import com.media.cache.proxy.LocalProxyServer;
import com.media.cache.utils.LocalProxyUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalProxyCacheManager {

    private static final int MSG_VIDEO_READY_TO_PLAY = 0x1;
    private static final int MSG_VIDEO_CACHE_PROGRESS = 0x2;
    private static final int MSG_VIDEO_CACHE_SPEED = 0x3;
    private static final int MSG_VIDEO_CANNOT_BE_CACHED = 0x4;
    private static final int MSG_VIDEO_CACHE_FAILED = 0x5;
    private static final int MSG_VIDEO_CACHE_FINISHED = 0x6;

    private static final long VIDEO_PROXY_CACHE_SIZE = 2 * 1024 * 1024 * 1024L;
    private static final int READ_TIMEOUT = 30 * 1000;
    private static final int CONN_TIMEOUT = 30 * 1000;
    private static final int SOCKET_TIMEOUT = 60 * 1000;

    private static LocalProxyCacheManager sInstance = null;
    private LocalProxyConfig mConfig;
    private LocalProxyServer mProxyServer;
    private Handler mVideoProxyCacheHandler = new VideoProxyCacheHandler();
    private Map<String, VideoDownloadTask> mVideoDownloadTaskMap = new ConcurrentHashMap<>();
    private Map<String, IVideoProxyCacheCallback> mVideoDownloadTaskCallbackMap = new ConcurrentHashMap<>();

    public static LocalProxyCacheManager getInstance() {
        if (sInstance == null) {
            synchronized (LocalProxyCacheManager.class) {
                if (sInstance == null) {
                    sInstance = new LocalProxyCacheManager();
                }
            }
        }
        return sInstance;
    }

    private LocalProxyCacheManager() {

    }

    public void initProxyCache(Context context) {
        File file = LocalProxyUtils.getVideoCacheDir(context);
        if (!file.exists()) {
            file.mkdir();
        }
        mConfig = new LocalProxyCacheManager.Build(context)
                .setCacheRoot(file)
                .setUrlRedirect(true)
                .setIgnoreAllCertErrors(true)
                .setCacheSize(VIDEO_PROXY_CACHE_SIZE)
                .setTimeOut(READ_TIMEOUT, CONN_TIMEOUT, SOCKET_TIMEOUT)
                .buildConfig();
        mProxyServer = new LocalProxyServer(mConfig);
//        registerConnectionListener(context);
    }

    @SuppressLint("NewApi")
    @SuppressWarnings({"MissingPermission"})
    private void registerConnectionListener(Context context) {
        NetworkCallbackImpl networkCallback = new NetworkCallbackImpl(mNetworkListener);
        NetworkRequest request = new NetworkRequest.Builder().build();
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            manager.registerNetworkCallback(request, networkCallback);
        }
    }

    public String getCacheFilePath() {
        if (mConfig != null) {
            return mConfig.getCacheRoot().getAbsolutePath();
        }
        return null;
    }

    public LocalProxyCacheManager(LocalProxyConfig config) {
        mProxyServer = new LocalProxyServer(config);
        mConfig = config;
    }

    public void startEngine(String videoUrl) {
        if (TextUtils.isEmpty(videoUrl) || videoUrl.startsWith("http://127.0.0.1")) {
            return;
        }
        startEngine(videoUrl, null);
    }

    public void startEngine(String videoUrl, final HashMap<String, String> headers) {
        if (TextUtils.isEmpty(videoUrl) ||
                videoUrl.startsWith("http://127.0.0.1")) {
            return;
        }
        String saveName = LocalProxyUtils.computeMD5(videoUrl);
        VideoCacheInfo info = LocalProxyUtils.readProxyCacheInfo(new File(mConfig.getCacheRoot(), saveName));
        if (info != null) {
            LogUtils.w("startEngine info = " + info);
            if (info.getVideoType() == Video.Type.MP4_TYPE
                    || info.getVideoType() == Video.Type.WEBM_TYPE
                    || info.getVideoType() == Video.Type.QUICKTIME_TYPE
                    || info.getVideoType() == Video.Type.GP3_TYPE) {
                startBaseVideoDownloadTask(info, headers);
            } else if (info.getVideoType() == Video.Type.HLS_TYPE) {
                VideoInfoParserManager.getInstance()
                        .parseM3U8File(info, new IVideoInfoParseCallback() {

                            @Override
                            public void onM3U8FileParseSuccess(VideoCacheInfo info, M3U8 m3u8) {
                                startM3U8VideoDownloadTask(info, m3u8, headers);
                            }

                            @Override
                            public void onM3U8FileParseFailed(VideoCacheInfo info, Throwable error) {
                                parseNetworkVideoInfo(info, headers, true,
                                        "" /* default content-type*/);
                            }
                        });
            }
        } else {
            LogUtils.d("startEngine url=" + videoUrl + ", headers=" + headers);
            info = new VideoCacheInfo(videoUrl);
            parseNetworkVideoInfo(info, headers, true,
                    "" /* default content-type*/);
        }
    }

    private void startEngine(String videoUrl, final HashMap<String, String> headers, final boolean shouldRedirect, final String contentType) {
        if (TextUtils.isEmpty(videoUrl) || videoUrl.startsWith("http://127.0.0.1")) {
            return;
        }
        String saveName = LocalProxyUtils.computeMD5(videoUrl);
        VideoCacheInfo info = LocalProxyUtils.readProxyCacheInfo(new File(mConfig.getCacheRoot(), saveName));
        if (info != null) {
            LogUtils.d("startEngine info = " + info);
            if (info.getVideoType() == Video.Type.MP4_TYPE
                    || info.getVideoType() == Video.Type.WEBM_TYPE
                    || info.getVideoType() == Video.Type.QUICKTIME_TYPE
                    || info.getVideoType() == Video.Type.GP3_TYPE) {
                startBaseVideoDownloadTask(info, headers);
            } else if (info.getVideoType() == Video.Type.HLS_TYPE) {
                VideoInfoParserManager.getInstance().parseM3U8File(info, new IVideoInfoParseCallback() {
                    @Override
                    public void onM3U8FileParseSuccess(VideoCacheInfo info, M3U8 m3u8) {
                        startM3U8VideoDownloadTask(info, m3u8, headers);
                    }

                    @Override
                    public void onM3U8FileParseFailed(VideoCacheInfo info, Throwable error) {
                        parseNetworkVideoInfo(info , headers, shouldRedirect, contentType);
                    }
                });
            }
        } else {
            LogUtils.d("startEngine url="+videoUrl + ", headers="+headers);
            info = new VideoCacheInfo(videoUrl);
            parseNetworkVideoInfo(info , headers, shouldRedirect, contentType);
        }
    }

    private void parseNetworkVideoInfo(final VideoCacheInfo info, final HashMap<String, String> headers, boolean shouldRedirect, String contentType) {
        VideoInfoParserManager.getInstance().parseVideoInfo(info, new IVideoInfoCallback() {
            @Override
            public void onFinalUrl(String finalUrl) {
                //Get final url by redirecting.
            }

            @Override
            public void onBaseVideoInfoSuccess(VideoCacheInfo info) {
                startBaseVideoDownloadTask(info, headers);
            }

            @Override
            public void onBaseVideoInfoFailed(Throwable error) {
                LogUtils.w("onInfoFailed error=" +error);
                VideoInfo videoInfo = new VideoInfo(info.getVideoUrl());
                mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_CANNOT_BE_CACHED, videoInfo).sendToTarget();
            }

            @Override
            public void onM3U8InfoSuccess(VideoCacheInfo info, M3U8 m3u8) {
                startM3U8VideoDownloadTask(info, m3u8, headers);
            }

            @Override
            public void onLiveM3U8Callback(VideoCacheInfo info) {
                LogUtils.i("onLiveM3U8Callback cannot be cached.");
                VideoInfo videoInfo = new VideoInfo(info.getVideoUrl());
                mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_CANNOT_BE_CACHED, videoInfo).sendToTarget();
            }

            @Override
            public void onM3U8InfoFailed(Throwable error) {
                LogUtils.w("onM3U8InfoFailed : " + error);
                VideoInfo videoInfo = new VideoInfo(info.getVideoUrl());
                mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_CANNOT_BE_CACHED, videoInfo).sendToTarget();
            }
        }, mConfig, headers, shouldRedirect, contentType);
    }

    public void startBaseVideoDownloadTask(final VideoCacheInfo info,
                                           HashMap<String, String> headers) {
        VideoDownloadTask task = null;
        if (!mVideoDownloadTaskMap.containsKey(info.getVideoUrl())) {
            task = new EntireVideoDownloadTask(mConfig, info, headers);
            mVideoDownloadTaskMap.put(info.getVideoUrl(), task);
        } else {
            task = mVideoDownloadTaskMap.get(info.getVideoUrl());
        }

        if (task != null) {
            task.startDownload(
                    new IVideoProxyCacheCallback() {
                        @Override
                        public void onCacheReady(String url, String proxyUrl) {
                            VideoInfo videoInfo = new VideoInfo(url);
                            videoInfo.setProxyUrl(proxyUrl);
                            mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_READY_TO_PLAY, videoInfo).sendToTarget();
                        }

                        @Override
                        public void onCacheProgressChanged(
                                String url, int percent, long cachedSize, M3U8 m3u8) {
                            VideoInfo videoInfo = new VideoInfo(url);
                            videoInfo.setProgressInfo(percent, cachedSize);
                            videoInfo.setM3U8(m3u8);
                            mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_CACHE_PROGRESS, videoInfo).sendToTarget();
                        }

                        @Override
                        public void onCacheSpeedChanged(String url, float cacheSpeed) {
                            VideoInfo videoInfo = new VideoInfo(url);
                            videoInfo.setSpeed(cacheSpeed);
                            mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_CACHE_SPEED, videoInfo).sendToTarget();
                        }

                        @Override
                        public void onCacheForbidden(String url) {}

                        @Override
                        public void onCacheFailed(String url, Exception e) {
                            LogUtils.w("onCacheFailed url=" + url + ", exception=" + e.getMessage());
                            VideoInfo videoInfo = new VideoInfo(url);
                            videoInfo.setException(e);
                            mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_CACHE_FAILED, videoInfo).sendToTarget();
                        }

                        @Override
                        public void onCacheFinished(String url) {
                            VideoInfo videoInfo = new VideoInfo(url);
                            mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_CACHE_FINISHED, videoInfo).sendToTarget();
                        }
                    });
        }

    }

    public void startM3U8VideoDownloadTask(final VideoCacheInfo info,
                                           M3U8 m3u8,
                                           HashMap<String, String> headers) {
        VideoDownloadTask task = null;
        if (!mVideoDownloadTaskMap.containsKey(info.getVideoUrl())) {
            task = new M3U8VideoDownloadTask(mConfig, info, m3u8, headers);
            mVideoDownloadTaskMap.put(info.getVideoUrl(), task);

        } else {
            task = mVideoDownloadTaskMap.get(info.getVideoUrl());
        }

        if (task != null) {
            task.startDownload(
                    new IVideoProxyCacheCallback() {
                        @Override
                        public void onCacheReady(String url, String proxyUrl) {
                            LogUtils.d("onCacheReady url="+url+", proxyUrl="+proxyUrl);
                            VideoInfo videoInfo = new VideoInfo(url);
                            videoInfo.setProxyUrl(proxyUrl);
                            mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_READY_TO_PLAY, videoInfo).sendToTarget();
                        }

                        @Override
                        public void onCacheProgressChanged(
                                String url, int percent, long cachedSize, M3U8 m3u8) {
                            VideoInfo videoInfo = new VideoInfo(url);
                            videoInfo.setProgressInfo(percent, cachedSize);
                            videoInfo.setM3U8(m3u8);
                            mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_CACHE_PROGRESS, videoInfo).sendToTarget();
                        }

                        @Override
                        public void onCacheSpeedChanged(String url, float cacheSpeed) {
                            VideoInfo videoInfo = new VideoInfo(url);
                            videoInfo.setSpeed(cacheSpeed);
                            mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_CACHE_SPEED, videoInfo).sendToTarget();
                        }

                        @Override
                        public void onCacheForbidden(String url) {}

                        @Override
                        public void onCacheFailed(String url, Exception e) {
                            VideoInfo videoInfo = new VideoInfo(url);
                            videoInfo.setException(e);
                            mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_CACHE_FAILED, videoInfo).sendToTarget();
                        }

                        @Override
                        public void onCacheFinished(String url) {
                            VideoInfo videoInfo = new VideoInfo(url);
                            mVideoProxyCacheHandler.obtainMessage(MSG_VIDEO_CACHE_FINISHED, videoInfo).sendToTarget();
                        }
                    });
        }

    }

    public void seekToDownloadTask(long curPosition, long totalDuration, String url) {
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            task.seekToDownload(curPosition, totalDuration);
        }
    }

    public void seekToDownloadTask(float seekPercent, String url) {
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            task.seekToDownload(seekPercent);
        }
    }

    //You can setFlowControlEnable = true when you want to control flow.
    public void setFlowControlEnable(boolean enable) {
        mConfig.setFlowControlEnable(enable);
    }

    public void suspendDownloadTask(String url) {
        if (TextUtils.isEmpty(url))
            return;
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            task.suspendDownload();
        }
    }

    public void restoreDownloadTask(String url) {
        if (TextUtils.isEmpty(url))
            return;
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            task.restoreDownload();
        }
    }

    public void resumeDownloadTask(String url) {
        if (TextUtils.isEmpty(url))
            return;
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            task.resumeDownload();
        }
    }

    public void pauseDownloadTask(String url) {
        if (TextUtils.isEmpty(url))
            return;
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            task.pauseDownload();
        }
    }

    public boolean isDownloadTaskPaused(String url) {
        if (TextUtils.isEmpty(url))
            return true;
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            return task.isDownloadTaskPaused();
        }
        return true;
    }

    public void stopDownloadTask(String url) {
        if (TextUtils.isEmpty(url))
            return;
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            task.stopDownload();
            mVideoDownloadTaskMap.remove(url);
        }
    }

    public void addCallback(String videoUrl, IVideoProxyCacheCallback callback){
        if (TextUtils.isEmpty(videoUrl))
            return;
        mVideoDownloadTaskCallbackMap.put(videoUrl, callback);
    }

    public void removeCallback(String videoUrl, IVideoProxyCacheCallback callback){
        if (TextUtils.isEmpty(videoUrl))
            return;
        mVideoDownloadTaskCallbackMap.remove(videoUrl);
    }

    class VideoInfo {
        String mUrl;
        String mProxyUrl;
        int mPercent;
        long mCachedSize;
        M3U8 mM3U8;
        Exception mException;
        float mSpeed;

        VideoInfo(String url) {
            this.mUrl = url;
        }

        void setProxyUrl(String proxyUrl) {
            this.mProxyUrl = proxyUrl;
        }

        void setProgressInfo(int percent, long cachedSize) {
            this.mPercent = percent;
            this.mCachedSize = cachedSize;
        }

        void setM3U8(M3U8 m3u8) { this.mM3U8 = m3u8; }

        void setException(Exception e) {
            this.mException = e;
        }

        void setSpeed(float speed) { this.mSpeed = speed; }
    }

    class VideoProxyCacheHandler extends Handler {

        public VideoProxyCacheHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            onVideoStateCallback(msg.what, msg.obj);
        }

        private void onVideoStateCallback(int msg, Object obj) {
            VideoInfo videoInfo = (VideoInfo)obj;
            IVideoProxyCacheCallback callback = mVideoDownloadTaskCallbackMap.containsKey(videoInfo.mUrl) ?
                    mVideoDownloadTaskCallbackMap.get(videoInfo.mUrl) : null;
            LogUtils.d("onVideoStateCallback callback="+callback);
            if (callback != null) {
                switch (msg) {
                    case MSG_VIDEO_READY_TO_PLAY:
                        callback.onCacheReady(videoInfo.mUrl, videoInfo.mProxyUrl);
                        break;
                    case MSG_VIDEO_CACHE_PROGRESS:
                        callback.onCacheProgressChanged(
                                videoInfo.mUrl, videoInfo.mPercent,
                                videoInfo.mCachedSize, videoInfo.mM3U8);
                        break;
                    case MSG_VIDEO_CACHE_SPEED:
                        callback.onCacheSpeedChanged(videoInfo.mUrl, videoInfo.mSpeed * 1000 / LocalProxyUtils.UPDATE_INTERVAL );
                        break;
                    case MSG_VIDEO_CANNOT_BE_CACHED:
                        callback.onCacheForbidden(videoInfo.mUrl);
                        break;
                    case MSG_VIDEO_CACHE_FAILED:
                        callback.onCacheFailed(videoInfo.mUrl, videoInfo.mException);
                        break;
                    case MSG_VIDEO_CACHE_FINISHED:
                        callback.onCacheFinished(videoInfo.mUrl);
                        break;
                }
            }
        }
    }

    private NetworkListener mNetworkListener = new NetworkListener() {
        @Override
        public void onAvailable() {
            LogUtils.e("onAvailable");
        }

        @Override
        public void onWifiConnected() {
            LogUtils.e("onWifiConnected");
        }

        @Override
        public void onMobileConnected() {
            LogUtils.e("onMobileConnected");
        }

        @Override
        public void onNetworkType() {
            LogUtils.e("onNetworkType");
        }

        @Override
        public void onUnConnected() {
            LogUtils.e("onUnConnected");
        }
    };

    public static class Build {
        private Context mContext;
        private File mCacheRoot;
        private long mCacheSize = 2 * 1024 * 1024 * 1024L;  // Default 2G.
        private int mReadTimeOut = 30 * 1000;    // 30 seconds
        private int mConnTimeOut = 30 * 1000;    // 30 seconds
        private int mSocketTimeOut = 60 * 1000; // 60 seconds
        private boolean mRedirect = false;
        private boolean mIgnoreAllCertErrors = false;
        private int mPort;
        private boolean mFlowControlEnable = false; // true: control flow; false: no control
        private long mMaxBufferSize = 20 * 1024 * 1024L;  // 20M
        private long mMinBufferSize = 10 * 1024 * 1024L;  // 10M

        public Build(Context context) {
            mContext = context;
        }

        public Build setCacheRoot(File cacheRoot) {
            mCacheRoot = cacheRoot;
            return this;
        }

        public Build setCacheSize(long cacheSize) {
            mCacheSize = cacheSize;
            return this;
        }

        public Build setTimeOut(int readTimeOut, int connTimeOut, int socketTimeOut) {
            mReadTimeOut = readTimeOut;
            mConnTimeOut = connTimeOut;
            mSocketTimeOut = socketTimeOut;
            return this;
        }

        public Build setUrlRedirect(boolean redirect) {
            mRedirect = redirect;
            return this;
        }

        public Build setIgnoreAllCertErrors(boolean ignoreAllCertErrors) {
            mIgnoreAllCertErrors = ignoreAllCertErrors;
            return this;
        }

        public Build setPort(int port) {
            mPort = port;
            return this;
        }

        //You can set enable=true to control flow.
        public Build setFlowControlEnable(boolean enable) {
            mFlowControlEnable = enable;
            return this;
        }

        //You can set maxBufferSize and minBufferSize when in mobile state.
        public Build setBufferSize(long maxBufferSize, long minBufferSize) {
            mMaxBufferSize = maxBufferSize;
            mMinBufferSize = minBufferSize;
            return this;
        }

        public LocalProxyCacheManager build() {
            return new LocalProxyCacheManager(buildConfig());
        }

        private LocalProxyConfig buildConfig() {
            return new LocalProxyConfig(mContext, mCacheRoot, mCacheSize,
                    mReadTimeOut, mConnTimeOut, mSocketTimeOut,
                    mRedirect, mIgnoreAllCertErrors, mPort, mFlowControlEnable,
                    mMaxBufferSize, mMinBufferSize);
        }
    }

}

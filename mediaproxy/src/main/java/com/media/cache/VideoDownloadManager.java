package com.media.cache;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.android.baselib.MediaSDKReceiver;
import com.android.baselib.NetworkCallbackImpl;
import com.android.baselib.NetworkListener;
import com.android.baselib.utils.LogUtils;
import com.media.cache.download.BaseVideoDownloadTask;
import com.media.cache.download.M3U8VideoDownloadTask;
import com.media.cache.download.VideoDownloadTask;
import com.media.cache.hls.M3U8;
import com.media.cache.listener.IDownloadInfosCallback;
import com.media.cache.listener.IDownloadListener;
import com.media.cache.listener.IVideoInfoCallback;
import com.media.cache.listener.IVideoInfoParseCallback;
import com.media.cache.listener.IDownloadTaskListener;
import com.media.cache.model.Video;
import com.media.cache.model.VideoCacheInfo;
import com.media.cache.model.VideoTaskItem;
import com.media.cache.model.VideoTaskState;
import com.media.cache.proxy.AsyncProxyServer;
import com.media.cache.proxy.CustomProxyServer;
import com.media.cache.utils.DownloadExceptionUtils;
import com.media.cache.utils.LocalProxyUtils;
import com.media.cache.utils.StorageUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class VideoDownloadManager {

    private static final int MSG_DOWNLOAD_DEFAULT = 0;
    private static final int MSG_DOWNLOAD_PENDING = 1;
    private static final int MSG_DOWNLOAD_PREPARE = 2;
    private static final int MSG_DOWNLOAD_START = 3;
    private static final int MSG_DOWNLOAD_PROXY_READY = 4;
    private static final int MSG_DOWNLOAD_PROCESSING = 5;
    private static final int MSG_DOWNLOAD_SPEED = 6;
    private static final int MSG_DOWNLOAD_PAUSE = 7;
    private static final int MSG_DOWNLOAD_SUCCESS = 8;
    private static final int MSG_DOWNLOAD_ERROR = 9;
    private static final int MSG_DOWNLOAD_PROXY_FORBIDDEN = 10;

    private static final int MSG_DOWNLOAD_INFOS = 100;

    private static VideoDownloadManager sInstance = null;
    private LocalProxyConfig mConfig;
    private VideoDownloadQueue mVideoDownloadQueue;
    private Handler mDownloadHandler = new DownloadHandler();
    private IDownloadListener mGlobalDownloadListener;
    private List<IDownloadInfosCallback> mDownloadInfoCallbacks = new CopyOnWriteArrayList<>();
    private Map<String, VideoDownloadTask> mVideoDownloadTaskMap = new ConcurrentHashMap<>();
    private Map<String, IDownloadListener> mDownloadListenerMap = new ConcurrentHashMap<>();

    public static VideoDownloadManager getInstance() {
        if (sInstance == null) {
            synchronized (VideoDownloadManager.class) {
                if (sInstance == null) {
                    sInstance = new VideoDownloadManager();
                }
            }
        }
        return sInstance;
    }

    private VideoDownloadManager() {
        mVideoDownloadQueue = new VideoDownloadQueue();
    }

    public LocalProxyConfig downloadConfig() { return mConfig; }

    public void initConfig(LocalProxyConfig config) {
        if (config == null)
            return;
        mConfig = config;
        new CustomProxyServer(mConfig);
        VideoInfoParserManager.getInstance().initConfig(config);
        registerReceiver(mConfig.getContext());
    }

    public void registerReceiver(Context context) {
        MediaSDKReceiver receiver = new MediaSDKReceiver();
        context.registerReceiver(receiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
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

    public VideoDownloadManager(LocalProxyConfig config) {
        new AsyncProxyServer(config);
        mConfig = config;
    }

    //1.DOWNLOAD MODULE
    //-----------------------------------------------------------------------
    //-------------------------DOWNLOAD MODULE-------------------------------
    //-----------------------------------------------------------------------
    public void fetchDownloadItems(IDownloadInfosCallback callback) {
        mDownloadInfoCallbacks.add(callback);
    }

    public void removeDownloadInfosCallback(IDownloadInfosCallback callback) {
        mDownloadInfoCallbacks.remove(callback);
    }

    public void setGlobalDownloadListener(IDownloadListener listener) {
        mGlobalDownloadListener = listener;
    }

    public void startDownload(VideoTaskItem taskItem) {
        if (taskItem == null || TextUtils.isEmpty(taskItem.getUrl()) || taskItem.getUrl().startsWith("http://127.0.0.1"))
            return;

        if (mVideoDownloadQueue.contains(taskItem)) {
            taskItem = mVideoDownloadQueue.getTaskItem(taskItem.getUrl());
        } else {
            mVideoDownloadQueue.offer(taskItem);
        }
        taskItem.setTaskState(VideoTaskState.PENDING);
        mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PENDING, taskItem).sendToTarget();

        if (mVideoDownloadQueue.getDownloadingCount() < mConfig.getConcurrentCount()) {
            startDownload(taskItem, null);
        }
    }

    public void startDownload(VideoTaskItem taskItem, HashMap<String, String> headers) {
        if (taskItem == null || TextUtils.isEmpty(taskItem.getUrl()) || taskItem.getUrl().startsWith("http://127.0.0.1"))
            return;
        taskItem.setTaskState(VideoTaskState.PREPARE);
        mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PREPARE, taskItem).sendToTarget();
        parseVideoInfo(taskItem, headers);
    }

    public void removeDownloadQueue(VideoTaskItem item) {
        mVideoDownloadQueue.remove(item);
        while(mVideoDownloadQueue.getDownloadingCount() < mConfig.getConcurrentCount() ) {
            if (mVideoDownloadQueue.getDownloadingCount() == mVideoDownloadQueue.size())
                break;
            VideoTaskItem item1 = mVideoDownloadQueue.peekPendingTask();
            startDownload(item1, null);
        }

    }

    //Delete one task
    public void deleteVideoTask(VideoTaskItem taskItem) {
        String cacheFilePath = getCacheFilePath();
        if (!TextUtils.isEmpty(cacheFilePath)) {
            if (taskItem.isRunningTask()) {
                pauseDownloadTask(taskItem);
            }
            String saveName = LocalProxyUtils.computeMD5(taskItem.getUrl());
            File file = new File(cacheFilePath + File.separator + saveName);
            StorageUtils.deleteCacheFile(file);
            taskItem.setTaskState(VideoTaskState.DEFAULT);
            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_DEFAULT, taskItem).sendToTarget();
        }
    }

    public void deleteVideoTasks(VideoTaskItem[] taskItems) {
        String cacheFilePath = getCacheFilePath();
        if (!TextUtils.isEmpty(cacheFilePath)) {
            for (VideoTaskItem item : taskItems) {
                deleteVideoTask(item);
            }
        }
    }

    //Delete all files
    public void deleteAllVideoFiles(Context context) {
        try {
            StorageUtils.clearVideoCacheDir(context);
        } catch (Exception e) {
            LogUtils.w("clearVideoCacheDir failed, exception = " + e.getMessage());
        }
    }

    //Pause all download task
    public void pauseDownloadTasks(VideoTaskItem[] taskItems) {
        for (VideoTaskItem item : taskItems) {
            if (item.isRunningTask()) {
                pauseDownloadTask(item);
            }
        }
    }
    //-----------------------------------------------------------------------
    //-------------------------DOWNLOAD MODULE-------------------------------
    //-----------------------------------------------------------------------


    //2.PLAY MODULE
    //-----------------------------------------------------------------------
    //-----------------------------PLAY MODULE-------------------------------
    //-----------------------------------------------------------------------
    public void startPlayCacheTask(VideoTaskItem taskItem, IDownloadListener listener) {
        if (taskItem == null || TextUtils.isEmpty(taskItem.getUrl()) || taskItem.getUrl().startsWith("http://127.0.0.1"))
            return;

        startPlayCacheTask(taskItem, null, listener);
    }

    public void startPlayCacheTask(VideoTaskItem taskItem, HashMap<String, String> headers, IDownloadListener listener) {
        if (taskItem == null || TextUtils.isEmpty(taskItem.getUrl()) || taskItem.getUrl().startsWith("http://127.0.0.1"))
            return;
        addCallback(taskItem.getUrl(), listener);
        taskItem.setTaskState(VideoTaskState.PREPARE);
        mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PREPARE, taskItem).sendToTarget();
        parseVideoInfo(taskItem, headers);
    }
    //-----------------------------------------------------------------------
    //-----------------------------PLAY MODULE-------------------------------
    //-----------------------------------------------------------------------


    private void parseVideoInfo(VideoTaskItem taskItem, final HashMap<String, String> headers) {
        String videoUrl = taskItem.getUrl();
        String saveName = LocalProxyUtils.computeMD5(videoUrl);
        VideoCacheInfo cacheInfo = LocalProxyUtils.readProxyCacheInfo(new File(mConfig.getCacheRoot(), saveName));
        if (cacheInfo != null) {
            LogUtils.w("parseVideoInfo info = " + cacheInfo);
            if (taskItem.isDownloadMode()) {
                long createTime = cacheInfo.getDownloadTime();
                if (createTime == 0L) {
                    createTime = System.currentTimeMillis();
                    cacheInfo.setDownloadTime(createTime);
                    taskItem.setDownloadTime(createTime);
                } else {
                    taskItem.setDownloadTime(createTime);
                }
            }

            if (cacheInfo.getVideoType() == Video.Type.MP4_TYPE
                    || cacheInfo.getVideoType() == Video.Type.WEBM_TYPE
                    || cacheInfo.getVideoType() == Video.Type.QUICKTIME_TYPE
                    || cacheInfo.getVideoType() == Video.Type.GP3_TYPE) {
                startBaseVideoDownloadTask(taskItem, cacheInfo, headers);
            } else if (cacheInfo.getVideoType() == Video.Type.HLS_TYPE) {
                VideoInfoParserManager.getInstance()
                        .parseM3U8File(cacheInfo, new IVideoInfoParseCallback() {

                            @Override
                            public void onM3U8FileParseSuccess(VideoCacheInfo info, M3U8 m3u8) {
                                startM3U8VideoDownloadTask(taskItem, info, m3u8, headers);
                            }

                            @Override
                            public void onM3U8FileParseFailed(VideoCacheInfo info, Throwable error) {
                                parseVideoInfo(taskItem, info, headers);
                            }
                        });
            }
        } else {
            cacheInfo = new VideoCacheInfo(videoUrl);
            cacheInfo.setTaskMode(taskItem.getTaskMode());
            if (taskItem.isDownloadMode()) {
                long createTime = System.currentTimeMillis();
                cacheInfo.setDownloadTime(createTime);
                taskItem.setDownloadTime(createTime);
            }
            parseVideoInfo(taskItem, cacheInfo, headers);
        }
    }

    private void parseVideoInfo(VideoTaskItem taskItem, final VideoCacheInfo cacheInfo, final HashMap<String, String> headers) {
        VideoInfoParserManager.getInstance().parseVideoInfo(cacheInfo, new IVideoInfoCallback() {
            @Override
            public void onFinalUrl(String finalUrl) {
                //Get final url by redirecting.
            }

            @Override
            public void onBaseVideoInfoSuccess(VideoCacheInfo cacheInfo) {
                startBaseVideoDownloadTask(taskItem, cacheInfo, headers);
            }

            @Override
            public void onBaseVideoInfoFailed(Throwable error) {
                LogUtils.w("onInfoFailed error=" +error);
                int errorCode = DownloadExceptionUtils.getErrorCode(error);
                taskItem.setErrorCode(errorCode);
                taskItem.setTaskState(VideoTaskState.ERROR);
                mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PROXY_FORBIDDEN, taskItem).sendToTarget();
            }

            @Override
            public void onM3U8InfoSuccess(VideoCacheInfo cacheInfo, M3U8 m3u8) {
                startM3U8VideoDownloadTask(taskItem, cacheInfo, m3u8, headers);
            }

            @Override
            public void onLiveM3U8Callback(VideoCacheInfo info) {
                LogUtils.i("onLiveM3U8Callback cannot be cached.");
                taskItem.setTaskState(VideoTaskState.ERROR);
                mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PROXY_FORBIDDEN, taskItem).sendToTarget();
            }

            @Override
            public void onM3U8InfoFailed(Throwable error) {
                error.printStackTrace();
                LogUtils.w("onM3U8InfoFailed : " + error);
                int errorCode = DownloadExceptionUtils.getErrorCode(error);
                taskItem.setErrorCode(errorCode);
                taskItem.setTaskState(VideoTaskState.ERROR);
                mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PROXY_FORBIDDEN, taskItem).sendToTarget();
            }
        }, headers);
    }

    public void startBaseVideoDownloadTask(VideoTaskItem taskItem,
                                           VideoCacheInfo cacheInfo,
                                           HashMap<String, String> headers) {
        taskItem.setVideoType(cacheInfo.getVideoType());
        VideoDownloadTask downloadTask = null;
        if (!mVideoDownloadTaskMap.containsKey(cacheInfo.getUrl())) {
            downloadTask = new BaseVideoDownloadTask(mConfig, cacheInfo, headers);
            mVideoDownloadTaskMap.put(cacheInfo.getUrl(), downloadTask);
        } else {
            downloadTask = mVideoDownloadTaskMap.get(cacheInfo.getUrl());
        }

        if (downloadTask != null) {
            downloadTask.startDownload(
                    new IDownloadTaskListener() {

                        @Override
                        public void onTaskStart(String url) {
                            taskItem.setTaskState(VideoTaskState.START);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_START, taskItem).sendToTarget();
                        }

                        @Override
                        public void onLocalProxyReady(String proxyUrl) {
                            taskItem.setProxyUrl(proxyUrl);
                            taskItem.setTaskState(VideoTaskState.PROXYREADY);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PROXY_READY, taskItem).sendToTarget();
                        }

                        @Override
                        public void onTaskProgress(float percent, long cachedSize, M3U8 m3u8) {
                            if (taskItem.getTaskState() == VideoTaskState.PAUSE || taskItem.getTaskState() == VideoTaskState.SUCCESS) {
                            } else {
                                taskItem.setTaskState(VideoTaskState.DOWNLOADING);
                                taskItem.setPercent(percent);
                                taskItem.setDownloadSize(cachedSize);
                                taskItem.setM3U8(m3u8);
                                mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PROCESSING, taskItem).sendToTarget();
                            }
                        }

                        @Override
                        public void onTaskSpeedChanged(float speed) {
                            taskItem.setSpeed(speed);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_SPEED, taskItem).sendToTarget();
                        }

                        @Override
                        public void onTaskPaused() {
                            taskItem.setTaskState(VideoTaskState.PAUSE);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PAUSE, taskItem).sendToTarget();
                        }

                        @Override
                        public void onTaskFinished(long totalSize) {
                            taskItem.setTaskState(VideoTaskState.SUCCESS);
                            taskItem.setDownloadSize(totalSize);
                            taskItem.setPercent(100f);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_SUCCESS, taskItem).sendToTarget();
                        }

                        @Override
                        public void onTaskFailed(Throwable e) {
                            int errorCode = DownloadExceptionUtils.getErrorCode(e);
                            taskItem.setErrorCode(errorCode);
                            taskItem.setTaskState(VideoTaskState.ERROR);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_ERROR, taskItem).sendToTarget();
                        }
                    });
        }

    }

    public void startM3U8VideoDownloadTask(VideoTaskItem taskItem,
                                           VideoCacheInfo cacheInfo,
                                           M3U8 m3u8,
                                           HashMap<String, String> headers) {
        taskItem.setVideoType(cacheInfo.getVideoType());
        VideoDownloadTask downloadTask = null;
        if (!mVideoDownloadTaskMap.containsKey(cacheInfo.getUrl())) {
            downloadTask = new M3U8VideoDownloadTask(mConfig, cacheInfo, m3u8, headers);
            mVideoDownloadTaskMap.put(cacheInfo.getUrl(), downloadTask);
        } else {
            downloadTask = mVideoDownloadTaskMap.get(cacheInfo.getUrl());
        }

        if (downloadTask != null) {
            downloadTask.startDownload(
                    new IDownloadTaskListener() {
                        @Override
                        public void onTaskStart(String url) {
                            taskItem.setTaskState(VideoTaskState.START);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_START, taskItem).sendToTarget();
                        }

                        @Override
                        public void onLocalProxyReady(String proxyUrl) {
                            taskItem.setProxyUrl(proxyUrl);
                            taskItem.setTaskState(VideoTaskState.PROXYREADY);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PROXY_READY, taskItem).sendToTarget();
                        }

                        @Override
                        public void onTaskProgress(float percent, long cachedSize, M3U8 m3u8) {
                            if (taskItem.getTaskState() == VideoTaskState.PAUSE || taskItem.getTaskState() == VideoTaskState.SUCCESS) {

                            } else {
                                taskItem.setTaskState(VideoTaskState.DOWNLOADING);
                                taskItem.setPercent(percent);
                                taskItem.setDownloadSize(cachedSize);
                                taskItem.setM3U8(m3u8);
                                mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PROCESSING, taskItem).sendToTarget();
                            }
                        }

                        @Override
                        public void onTaskSpeedChanged(float speed) {
                            taskItem.setSpeed(speed);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_SPEED, taskItem).sendToTarget();
                        }

                        @Override
                        public void onTaskPaused() {
                            taskItem.setTaskState(VideoTaskState.PAUSE);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_PAUSE, taskItem).sendToTarget();
                        }

                        @Override
                        public void onTaskFinished(long totalSize) {
                            taskItem.setTaskState(VideoTaskState.SUCCESS);
                            taskItem.setPercent(100f);
                            taskItem.setDownloadSize(totalSize);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_SUCCESS, taskItem).sendToTarget();
                        }

                        @Override
                        public void onTaskFailed(Throwable e) {
                            int errorCode = DownloadExceptionUtils.getErrorCode(e);
                            taskItem.setErrorCode(errorCode);
                            taskItem.setTaskState(VideoTaskState.ERROR);
                            mDownloadHandler.obtainMessage(MSG_DOWNLOAD_ERROR, taskItem).sendToTarget();
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

    public void resumeDownloadTask(VideoTaskItem taskItem, IDownloadListener listener) {
        if (taskItem == null || TextUtils.isEmpty(taskItem.getUrl()))
            return;
        String url = taskItem.getUrl();
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            task.resumeDownload();
            addCallback(url, listener);
        }
    }

    public void pauseDownloadTask(VideoTaskItem taskItem) {
        if (taskItem == null || TextUtils.isEmpty(taskItem.getUrl()))
            return;
        String url = taskItem.getUrl();
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            task.pauseDownload();
        }
    }

    public void stopDownloadTask(VideoTaskItem taskItem) {
        if (taskItem == null || TextUtils.isEmpty(taskItem.getUrl()))
            return;
        String url = taskItem.getUrl();
        VideoDownloadTask task = mVideoDownloadTaskMap.get(url);
        if (task != null) {
            task.stopDownload();
            mVideoDownloadTaskMap.remove(url);
            removeCallback(url);
        }
    }

    public void addCallback(String url, IDownloadListener listener){
        if (TextUtils.isEmpty(url))
            return;
        mDownloadListenerMap.put(url, listener);
    }

    public void removeCallback(String url){
        if (TextUtils.isEmpty(url))
            return;
        mDownloadListenerMap.remove(url);
    }

    class DownloadHandler extends Handler {

        public DownloadHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_DOWNLOAD_INFOS) {
                dispatchDownloadInfos(msg.what, msg.obj);
            } else {
                dispatchVideoCacheState(msg.what, msg.obj);
            }
        }

        private void dispatchDownloadInfos(int msg, Object obj) {

        }

        private void dispatchVideoCacheState(int msg, Object obj) {
            VideoTaskItem item = (VideoTaskItem)obj;
                IDownloadListener listener = mDownloadListenerMap.containsKey(item.getUrl()) ?
                        mDownloadListenerMap.get(item.getUrl()) : null;
                handleMessage(msg, item, listener);
                handleMessage(msg, item, mGlobalDownloadListener);
        }

        private void handleMessage(int msg, VideoTaskItem item, IDownloadListener listener) {
            if (listener != null) {
                switch (msg) {
                    case MSG_DOWNLOAD_DEFAULT:
                        listener.onDownloadDefault(item);
                        break;
                    case MSG_DOWNLOAD_PENDING:
                        listener.onDownloadPending(item);
                        break;
                    case MSG_DOWNLOAD_PREPARE:
                        listener.onDownloadPrepare(item);
                        break;
                    case MSG_DOWNLOAD_START:
                        listener.onDownloadStart(item);
                        break;
                    case MSG_DOWNLOAD_PROXY_READY:
                        listener.onDownloadProxyReady(item);
                        break;
                    case MSG_DOWNLOAD_PROCESSING:
                        listener.onDownloadProgress(item);
                        break;
                    case MSG_DOWNLOAD_SPEED:
                        listener.onDownloadSpeed(item);
                        break;
                    case MSG_DOWNLOAD_PAUSE:
                        removeDownloadQueue(item);
                        listener.onDownloadPause(item);
                        break;
                    case MSG_DOWNLOAD_PROXY_FORBIDDEN:
                        removeDownloadQueue(item);
                        listener.onDownloadProxyForbidden(item);
                        break;
                    case MSG_DOWNLOAD_ERROR:
                        removeDownloadQueue(item);
                        listener.onDownloadError(item);
                        break;
                    case MSG_DOWNLOAD_SUCCESS:
                        removeDownloadQueue(item);
                        listener.onDownloadSuccess(item);
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
        private int mConcurrentCount = 3;
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

        public Build setConcurrentCount(int count) {
            mConcurrentCount = count;
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

        public VideoDownloadManager build() {
            return new VideoDownloadManager(buildConfig());
        }

        public LocalProxyConfig buildConfig() {
            return new LocalProxyConfig(mContext, mCacheRoot, mCacheSize,
                    mReadTimeOut, mConnTimeOut, mSocketTimeOut, mRedirect,
                    mIgnoreAllCertErrors, mPort, mFlowControlEnable,
                    mMaxBufferSize, mMinBufferSize, mConcurrentCount);
        }
    }

    public void setIgnoreAllCertErrors(boolean enable) {
        if (mConfig != null) {
            mConfig.setIgnoreAllCertErrors(enable);
        }
    }

    public void setConcurrentCount(int count) {
        if (mConfig != null) {
            mConfig.setConcurrentCount(count);
        }
    }

}

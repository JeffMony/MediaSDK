package com.media.cache.download;

import com.media.cache.LocalProxyConfig;
import com.media.cache.VideoCacheInfo;
import com.media.cache.listener.IVideoProxyCacheCallback;
import com.media.cache.utils.LocalProxyThreadUtils;
import com.media.cache.utils.LocalProxyUtils;
import com.media.cache.utils.LogUtils;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public abstract class VideoDownloadTask {

    private static final int DEFAULT_SLEEP_TIME_MILLIS = 5 * 100;
    protected static final int THREAD_COUNT = 3;
    protected static final int BUFFER_SIZE = LocalProxyUtils.DEFAULT_BUFFER_SIZE;

    protected volatile boolean mShouldSuspendDownloadTask = false;
    protected volatile boolean mIsPlaying = false;
    protected final LocalProxyConfig mConfig;
    protected final VideoCacheInfo mInfo;
    protected final String mFinalUrl;
    protected final HashMap<String, String> mHeaders;
    protected String mProxyAuthInfo = "";
    protected File mSaveDir;
    protected String mSaveName;
    protected ExecutorService mDownloadExecutor;
    protected IVideoProxyCacheCallback mCallback;

    protected volatile OPERATE_TYPE mType = OPERATE_TYPE.DEFAULT;
    protected enum OPERATE_TYPE {
        DEFAULT,
        WRITED,
    }

    protected VideoDownloadTask(LocalProxyConfig config,
                                VideoCacheInfo info,
                                HashMap<String, String> headers) {
        mConfig = config;
        mInfo = info;
        mHeaders = headers;
        mFinalUrl = info.getFinalUrl();
        mSaveName = LocalProxyUtils.computeMD5(info.getVideoUrl());
        mSaveDir = new File(mConfig.getCacheRoot(), mSaveName);
        if (!mSaveDir.exists()) {
            mSaveDir.mkdir();
        }
        info.setSaveDir(mSaveDir.getAbsolutePath());
    }

    public void suspendDownload() {
        mShouldSuspendDownloadTask = true;
    }

    public void restoreDownload() {
        mShouldSuspendDownloadTask = false;
    }

    public void suspendDownloadTaskByCondition() {
        try {
            while (mShouldSuspendDownloadTask) {
                LogUtils.w("suspendDownloadTaskByCondition");
                Thread.sleep(DEFAULT_SLEEP_TIME_MILLIS);
            }
        } catch (Exception e) {
            LogUtils.w("determineDownloadState failed, exception="+e);
        }
    }

    public boolean isDownloadTaskPaused() {
        if (mInfo != null && mInfo.getIsCompleted()) {
            return true;
        }
        if (mDownloadExecutor != null) {
            return mDownloadExecutor.isShutdown();
        }
        return true;
    }

    public abstract void startDownload(IVideoProxyCacheCallback callback);

    public abstract void resumeDownload();

    public abstract void seekToDownload(float seekPercent);

    public abstract void seekToDownload(long curPosition, long totalDuration);

    public abstract void seekToDownload(float seekPercent, IVideoProxyCacheCallback callback);

    public abstract void seekToDownload(int curDownloadTs, IVideoProxyCacheCallback callback);

    public abstract void seekToDownload(long curLength, IVideoProxyCacheCallback callback);

    public abstract void pauseDownload();

    public abstract void stopDownload();


    //1.Update cache file's last-modified-time.
    //2.Get LRU files.
    //3.Delete the files by LRU.
    protected void checkCacheFile(File saveDir) {
        try {
            LocalProxyThreadUtils.submitCallbackTask(new CheckFileCallable(saveDir));
        } catch (Exception e) {
            LogUtils.w("VideoDownloadTask checkCacheFile " + saveDir +" failed, exception="+e);
        }
    }

    private class CheckFileCallable implements Callable<Void> {

        private File mDir;

        public CheckFileCallable(File dir) {
            mDir = dir;
        }

        @Override
        public Void call() throws Exception {
            LocalProxyUtils.setLastModifiedNow(mDir);
            trimCacheFile(mDir.getParentFile());
            return null;
        }
    }

    private void trimCacheFile(File dir) {
        List<File> files = LocalProxyUtils.getLruFileList(dir);
        trimCacheFile(files, mConfig.getCacheSize());
    }

    private void trimCacheFile(List<File> files, long limitCacheSize) {
        long totalSize = LocalProxyUtils.countTotalSize(files);
        int totalCount = files.size();
        for (File file : files) {
            boolean shouldDeleteFile = shouldDeleteFile(totalSize, totalCount, limitCacheSize);
            if (shouldDeleteFile) {
                long fileLength = LocalProxyUtils.countTotalSize(file);
                boolean deleted = LocalProxyUtils.deleteFile(file);
                if (deleted) {
                    totalSize -= fileLength;
                    totalCount--;
                    LogUtils.i("trimCacheFile okay.");
                } else {
                    LogUtils.w("trimCacheFile delete file " + file.getAbsolutePath() +" failed.");
                }
            }
        }
    }

    private boolean shouldDeleteFile(long totalSize, int totalCount, long limitCacheSize) {
        if (totalCount <= 1) {
            return false;
        }
        return totalSize > limitCacheSize;
    }

    protected void trustAllCert(HttpsURLConnection httpsURLConnection) {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            if (sslContext != null) {
                TrustManager tm = new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        LogUtils.v( "checkClientTrusted.");
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        LogUtils.v("checkServerTrusted.");
                    }
                };
                sslContext.init(null, new TrustManager[] { tm }, null);
            }
        } catch (Exception e) {
            LogUtils.w( "SSLContext init failed");
        }
        // Cannot do ssl checkl.
        if (sslContext != null) {
            httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        }
        //Trust the cert.
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        httpsURLConnection.setHostnameVerifier(hostnameVerifier);
    }

}


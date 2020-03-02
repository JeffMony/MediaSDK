package com.media.cache.listener;

import com.media.cache.hls.M3U8;

public interface IDownloadTaskListener {

    void onTaskStart(String url);

    void onLocalProxyReady(String proxyUrl);

    void onTaskProgress(float percent, long cachedSize, M3U8 m3u8);

    void onTaskSpeedChanged(float speed);

    void onTaskPaused();

    void onTaskFinished(long totalSize);

    void onTaskFailed(Throwable e);

}

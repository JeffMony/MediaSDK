package com.media.cache.listener;

import com.media.cache.model.VideoTaskItem;

public interface IDownloadListener {

    void onDownloadDefault(VideoTaskItem item);

    void onDownloadPrepare(VideoTaskItem item);

    void onDownloadPending(VideoTaskItem item);

    void onDownloadStart(VideoTaskItem item);

    void onDownloadProxyReady(VideoTaskItem item);

    void onDownloadProgress(VideoTaskItem item);

    void onDownloadSpeed(VideoTaskItem item);

    void onDownloadPause(VideoTaskItem item);

    void onDownloadError(VideoTaskItem item);

    void onDownloadProxyForbidden(VideoTaskItem item);

    void onDownloadSuccess(VideoTaskItem item);
}

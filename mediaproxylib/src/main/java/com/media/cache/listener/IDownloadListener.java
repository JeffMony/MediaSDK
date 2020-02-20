package com.media.cache.listener;

import com.media.cache.model.VideoItem;

public interface IDownloadListener {

    void onDownloadPrepare(VideoItem item);

    void onDownloadStart(VideoItem item);

    void onDownloadProgress(VideoItem item);

    void onDownloadPending(VideoItem item);

    void onDownloadError(VideoItem item);

    void onDownloadSuccess(VideoItem item);
}

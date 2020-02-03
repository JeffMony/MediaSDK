package com.media.cache.listener;

import com.media.cache.hls.M3U8;

public interface IVideoProxyCacheCallback {

    void onCacheReady(String url, String proxyUrl);

    void onCacheProgressChanged(String url, int percent, long cachedSize,
                                M3U8 m3u8);

    void onCacheSpeedChanged(String url, float cacheSpeed);

    void onCacheFinished(String url);

    void onCacheForbidden(String url);

    void onCacheFailed(String url, Exception e);
}

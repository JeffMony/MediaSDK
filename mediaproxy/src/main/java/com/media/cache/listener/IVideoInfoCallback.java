package com.media.cache.listener;

import com.media.cache.model.VideoCacheInfo;
import com.media.cache.hls.M3U8;

public interface IVideoInfoCallback {

    void onFinalUrl(String finalUrl);

    void onBaseVideoInfoSuccess(VideoCacheInfo info);

    void onBaseVideoInfoFailed(Throwable error);

    void onM3U8InfoSuccess(VideoCacheInfo info, M3U8 m3u8);

    void onLiveM3U8Callback(VideoCacheInfo info);

    void onM3U8InfoFailed(Throwable error);
}

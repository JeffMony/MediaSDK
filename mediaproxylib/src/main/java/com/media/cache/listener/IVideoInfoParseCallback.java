package com.media.cache.listener;

import com.media.cache.model.VideoCacheInfo;
import com.media.cache.hls.M3U8;

public interface IVideoInfoParseCallback {

    void onM3U8FileParseSuccess(VideoCacheInfo info, M3U8 m3u8);

    void onM3U8FileParseFailed(VideoCacheInfo info, Throwable error);

}

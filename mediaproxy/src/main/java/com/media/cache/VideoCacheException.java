package com.media.cache;

public class VideoCacheException extends Exception {

    private String mMsg;

    public VideoCacheException(String msg) {
        mMsg = msg;
    }

    public String getMsg() {
        return mMsg;
    }
}

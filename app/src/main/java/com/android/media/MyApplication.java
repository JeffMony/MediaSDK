package com.android.media;

import android.app.Application;

import com.media.cache.VideoDownloadManager;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        VideoDownloadManager.getInstance().initProxyCache(this);
    }
}

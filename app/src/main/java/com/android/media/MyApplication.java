package com.android.media;

import android.app.Application;

import com.android.player.proxy.LocalProxyCacheManager;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LocalProxyCacheManager.getInstance().initProxyCache(this);
    }
}

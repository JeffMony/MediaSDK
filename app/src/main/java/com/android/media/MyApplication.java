package com.android.media;

import android.app.Application;

import com.media.cache.DownloadConstants;
import com.media.cache.LocalProxyConfig;
import com.media.cache.VideoDownloadManager;
import com.media.cache.utils.StorageUtils;

import java.io.File;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        File file = StorageUtils.getVideoCacheDir(this);
        if (!file.exists()) {
            file.mkdir();
        }
        LocalProxyConfig config = new VideoDownloadManager.Build(this)
                        .setCacheRoot(file)
                        .setUrlRedirect(false)
                        .setTimeOut(DownloadConstants.READ_TIMEOUT,
                                DownloadConstants.CONN_TIMEOUT,
                                DownloadConstants.SOCKET_TIMEOUT)
                        .setConcurrentCount(DownloadConstants.CONCURRENT_COUNT)
                        .setIgnoreAllCertErrors(true)
                        .buildConfig();
        VideoDownloadManager.getInstance().initConfig(config);
    }
}

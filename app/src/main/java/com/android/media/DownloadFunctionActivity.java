package com.android.media;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.baselib.utils.LogUtils;
import com.android.player.proxy.LocalProxyCacheManager;
import com.media.cache.hls.M3U8;
import com.media.cache.listener.IVideoProxyCacheCallback;
import com.media.cache.model.VideoItem;

public class DownloadFunctionActivity extends Activity {

    private ListView mDownloadListView;
    private TextView mFilePath;

    private VideoListAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_func);

        initViews();
        initDatas();

//        LocalProxyCacheManager.getInstance().startEngine(mUrl);
//        LocalProxyCacheManager.getInstance().addCallback(mUrl, mCallback);
    }

    private void initViews() {
        mDownloadListView = (ListView) findViewById(R.id.download_listview);
        mFilePath = (TextView) findViewById(R.id.file_path);

        mFilePath.setText(LocalProxyCacheManager.getInstance().getCacheFilePath());
    }

    private void initDatas() {
        VideoItem item1 = new VideoItem("https://tv.youkutv.cc/2019/10/28/6MSVuLec4zbpYFlj/playlist.m3u8");
        VideoItem item2 = new VideoItem("https://kuku.zuida-youku.com/20170616/cBIBaYMJ/index.m3u8");
        VideoItem item3 = new VideoItem("https://tv.youkutv.cc/2020/01/15/SZpLQDUmJZKF9O0D/playlist.m3u8");
        VideoItem item4 = new VideoItem("https://tv.youkutv.cc/2020/01/15/3d97sO5xQUYB5bvY/playlist.m3u8");
        VideoItem item5 = new VideoItem("http://gv.vivo.com.cn/appstore/gamecenter/upload/video/201701/2017011314414026850.mp4");
        VideoItem item6 = new VideoItem("https://ll1.zhengzhuji.com/hls/20181111/8a1f15ba7a8f0ca5418229a0cdd7bd92/1541946502/index.m3u8");
        VideoItem item7 = new VideoItem("https://tv.youkutv.cc/2019/10/28/6MSVuLec4zbpYFlj/playlist.m3u8");
        VideoItem item8 = new VideoItem("https://tv.youkutv.cc/2019/10/28/6MSVuLec4zbpYFlj/playlist.m3u8");

        VideoItem[] items = new VideoItem[8];
        items[0] = item1;
        items[1] = item2;
        items[2] = item3;
        items[3] = item4;
        items[4] = item5;
        items[5] = item6;
        items[6] = item7;
        items[7] = item8;

        mAdapter = new VideoListAdapter(this, R.layout.download_item, items);
        mDownloadListView.setAdapter(mAdapter);

    }

    private IVideoProxyCacheCallback mCallback = new IVideoProxyCacheCallback() {
        @Override
        public void onCacheReady(String url, String proxyUrl) {

        }

        @Override
        public void onCacheProgressChanged(String url, int percent, long cachedSize, M3U8 m3u8) {
            LogUtils.d("url="+url+", percent="+percent+", size="+cachedSize);
        }

        @Override
        public void onCacheSpeedChanged(String url, float cacheSpeed) {

        }

        @Override
        public void onCacheFinished(String url) {

        }

        @Override
        public void onCacheForbidden(String url) {

        }

        @Override
        public void onCacheFailed(String url, Exception e) {

        }
    };
}

package com.android.player.impl;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;

import com.android.player.PlayerAttributes;
import com.android.player.utils.LogUtils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

public class ExoPlayerImpl extends PlayerImpl {

    private Context mContext;
    private SimpleExoPlayer mPlayer;
    private MediaSource mMediaSource;

    private boolean mIsInitPlayerListener = false;

    public ExoPlayerImpl(Context context, PlayerAttributes attributes) {
        super(context, attributes);
        mContext = context.getApplicationContext();
        mPlayer = new SimpleExoPlayer.Builder(context).build();
    }

    @Override
    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        Uri uri = Uri.parse(path);
        mMediaSource = createMediaSource(uri, null);
        mUrl = uri.toString();
    }

    @Override
    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mMediaSource = createMediaSource(uri, null);
        mUrl = uri.toString();
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mMediaSource = createMediaSource(uri, null);
        mUrl = uri.toString();
    }

    @Override
    public void setSurface(Surface surface) {
        mPlayer.setVideoSurface(surface);
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        if (!mIsInitPlayerListener) {
            initPlayerListener();
        }
        mPlayer.prepare(mMediaSource);
    }

    @Override
    public void start() throws IllegalStateException {
        mPlayer.setPlayWhenReady(true);
    }

    @Override
    public void pause() throws IllegalStateException {
        mPlayer.setPlayWhenReady(false);

    }

    @Override
    public void stop() throws IllegalStateException {
        mPlayer.stop();
    }

    @Override
    public void release() {
        mPlayer.release();
    }

    @Override
    public long getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mPlayer.getDuration();
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        super.seekTo(msec);
    }

    private void initPlayerListener() {
        mPlayer.addListener(new PlayerEventListener());
        mIsInitPlayerListener = true;
    }

    private DataSource.Factory buildDataSourceFactory() {
        String userAgent = Util.getUserAgent(mContext, "ExoPlayerDemo");
        DefaultDataSourceFactory upstreamFactory =
                new DefaultDataSourceFactory(mContext, new DefaultHttpDataSourceFactory(userAgent));
        return upstreamFactory;
    }

    private MediaSource createMediaSource(Uri uri, String extension) {
        int type = Util.inferContentType(uri, extension);
        DataSource.Factory dataSourceFactory = buildDataSourceFactory();
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(uri);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    private class PlayerEventListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            LogUtils.d("onPlayerStateChanged playWhenReady="+playWhenReady+", playbackState="+playbackState);
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {

        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {

        }
    }
}

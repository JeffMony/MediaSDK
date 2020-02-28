package com.android.player.impl;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;

import com.android.baselib.utils.LogUtils;
import com.android.player.PlayerAttributes;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
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
import com.google.android.exoplayer2.video.VideoListener;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

public class ExoPlayerImpl extends PlayerImpl {

    private static final int PREPARE_NULL = 0x0;
    private static final int PREPARING_STATE = 0x1;
    private static final int PREPARED_STATE = 0x2;

    private Context mContext;
    private SimpleExoPlayer mPlayer;
    private MediaSource mMediaSource;
    private int mPrepareState = PREPARE_NULL;

    private boolean mIsInitPlayerListener = false;
    private PlayerEventListener mEventListener;
    private PlayerVideoListener mVideoListener;

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
        super.prepareAsync();
        if (!mIsInitPlayerListener) {
            initPlayerListener();
        }
        mPrepareState = PREPARING_STATE;
        mPlayer.prepare(mMediaSource);
    }

    @Override
    public void start() throws IllegalStateException {
        mPlayer.setPlayWhenReady(true);
        super.start();
    }

    @Override
    public void pause() throws IllegalStateException {
        mPlayer.setPlayWhenReady(false);
        super.pause();
    }

    @Override
    public void setSpeed(float speed) {
        PlaybackParameters parameters = new PlaybackParameters(speed);
        mPlayer.setPlaybackParameters(parameters);
    }

    @Override
    public void stop() throws IllegalStateException {
        mPlayer.stop();
        super.stop();
    }

    @Override
    public void release() {
        mPlayer.removeVideoListener(mVideoListener);
        mPlayer.removeListener(mEventListener);
        mPlayer.release();
        super.release();
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
    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        mPlayer.seekTo(msec);
        super.seekTo(msec);
    }

    private void initPlayerListener() {
        mEventListener = new PlayerEventListener();
        mVideoListener = new PlayerVideoListener();
        mPlayer.addListener(mEventListener);
        mPlayer.addVideoListener(mVideoListener);
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
            switch(playbackState) {
                case Player.STATE_BUFFERING:
                    break;
                case Player.STATE_IDLE:
                    break;
                case Player.STATE_READY:
                    if (mPrepareState == PREPARING_STATE) {
                        notifyOnPrepared();
                        mPrepareState = PREPARED_STATE;
                    }
                    break;
                case Player.STATE_ENDED:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            notifyOnError(error.type, error.getCause().getMessage());
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            LogUtils.d("onIsPlayingChanged isPlaying="+isPlaying);
        }
    }

    private class PlayerVideoListener implements VideoListener {

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            notifyOnVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
        }

        @Override
        public void onRenderedFirstFrame() {

        }

        @Override
        public void onSurfaceSizeChanged(int width, int height) {

        }
    }
}

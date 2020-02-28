package com.android.player.impl;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.view.Surface;

import com.android.player.PlayerAttributes;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IjkPlayerImpl extends PlayerImpl {

    private IjkMediaPlayer mPlayer;

    public IjkPlayerImpl(Context context, PlayerAttributes attributes) {
        super(context, attributes);

        mPlayer = new IjkMediaPlayer();

        //不用MediaCodec编解码
        mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,
            "mediacodec", 1);

        //不用opensles编解码
        mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,
            "opensles", 0);
        mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,
            "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
        mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,
                "framedrop", 1);
        mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER,
            "start-on-prepared", 0);
        mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,
            "http-detect-range-support", 0);
        mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,
            "timeout", 10000000);
        mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT,
            "reconnect", 1);
        mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC,
            "skip_loop_filter", 48);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        initPlayerListeners();
    }

    private void initPlayerListeners() {
        mPlayer.setOnPreparedListener(mOnPreparedListener);
        mPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mPlayer.setOnErrorListener(mOnErrorListener);
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mPlayer.setDataSource(path);
        mUrl = path;
    }

    @Override
    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
        mPlayer.setDataSource(fd);
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mPlayer.setDataSource(context, uri);
        mUrl = uri.toString();
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mPlayer.setDataSource(context, uri, headers);
        mUrl = uri.toString();
    }

    @Override
    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {

    }

    @Override
    public void setSurface(Surface surface) {
        mPlayer.setSurface(surface);
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        super.prepareAsync();
        mPlayer.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        mPlayer.start();
        super.start();
    }

    @Override
    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    @Override
    public void pause() throws IllegalStateException {
        mPlayer.pause();
        super.pause();
    }

    @Override
    public void setSpeed(float speed) {
        mPlayer.setSpeed(speed);
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        mPlayer.seekTo(msec);
        super.seekTo(msec);
    }

    @Override
    public void stop() throws IllegalStateException {
        mPlayer.stop();
        super.stop();
    }

    @Override
    public void release() {
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

    private IjkMediaPlayer.OnPreparedListener mOnPreparedListener = new IjkMediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(IMediaPlayer mp) {
            notifyOnPrepared();
        }

    };

    private IjkMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = new IjkMediaPlayer.OnVideoSizeChangedListener() {

        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
            float pixelRatio = sar_num * 1.0f / sar_den;
            if (Float.compare(pixelRatio, Float.NaN) == 0) {
                pixelRatio = 1.0f;
            }
            notifyOnVideoSizeChanged(width, height, 0, pixelRatio);
        }

    };

    private IjkMediaPlayer.OnErrorListener mOnErrorListener = new IjkMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            notifyOnError(what, "" + extra);
            return true;
        }
    };

}

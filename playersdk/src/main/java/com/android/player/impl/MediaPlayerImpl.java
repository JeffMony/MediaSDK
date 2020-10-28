package com.android.player.impl;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.view.Surface;

import com.android.baselib.utils.LogUtils;
import com.android.player.PlayerAttributes;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

public class MediaPlayerImpl extends PlayerImpl
        implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnVideoSizeChangedListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnCompletionListener {

    private Context mContext;
    private MediaPlayer mPlayer = null;

    private boolean mIsReleased = false;

    private static byte[] LOCK = new byte[0];
    private final Object mLock = new Object();

    public MediaPlayerImpl(Context context, PlayerAttributes attributes) {
        super(context, attributes);
        synchronized (LOCK) {
            mPlayer = new MediaPlayer();
        }
        mContext = context.getApplicationContext();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnSeekCompleteListener(this);
        mPlayer.setOnVideoSizeChangedListener(this);
        mPlayer.setOnBufferingUpdateListener(this);
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {Uri uri = Uri.parse(path);
        mUrl = uri.getPath();
        String scheme = uri.getScheme();
        if (!TextUtils.isEmpty(scheme) && scheme.equalsIgnoreCase("file")) {
            mPlayer.setDataSource(uri.getPath());
        } else {
            mPlayer.setDataSource(path);
        }
    }

    @Override
    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mPlayer.setDataSource(fd, offset, length);
    }

    @Override
    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
        mPlayer.setDataSource(fd);
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mUrl = uri.getPath();
        mPlayer.setDataSource(context, uri);
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mUrl = uri.getPath();
        mPlayer.setDataSource(context, uri, headers);
    }

    @Override
    public void setSurface(Surface surface) {
        if (!mIsReleased) {
            try {
                mPlayer.setSurface(surface);
            } catch (Exception e) {
                LogUtils.e("setSurface failed, exception = " + e.getMessage());
            }
        }
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        super.prepareAsync();
        mPlayer.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        mPlayer.start();
    }

    @Override
    public void doOpenPlay(String url) {

    }

    @Override
    public void pause() throws IllegalStateException {
        mPlayer.pause();
    }

    @Override
    public void setSpeed(float speed) {
        if (Build.VERSION.SDK_INT > 23) {
            PlaybackParams params = new PlaybackParams();
            params.setSpeed(speed);
            mPlayer.setPlaybackParams(params);
        } else {
            LogUtils.w("setSpeed is invalid.");
        }
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
    public void seekTo(long msec) throws IllegalStateException {
        int position = (int)msec;
        mPlayer.seekTo(position);
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
    public void onPrepared(MediaPlayer mp) {
        notifyOnPrepared();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        notifyOnError(what, "" + extra);
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        notifyOnVideoSizeChanged(width, height, 0, 1, 1.0f);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }
}

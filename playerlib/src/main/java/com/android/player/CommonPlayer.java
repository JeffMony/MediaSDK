package com.android.player;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;

import com.android.player.impl.ExoPlayerImpl;
import com.android.player.impl.IjkPlayerImpl;
import com.android.player.impl.MediaPlayerImpl;
import com.android.player.impl.PlayerImpl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CommonPlayer implements IPlayer {

    private PlayerImpl mPlayerImpl;
    private PlayerType mType;

    public CommonPlayer(Context context) {
        this(context, PlayerType.EXO_PLAYER);
    }

    public CommonPlayer(Context context, PlayerType type) {
        this(context, type, null);
    }

    public CommonPlayer(Context context, PlayerType type, PlayerAttributes attributes) {
        this.mType = type;

        if (type == PlayerType.MEDIA_PLAYER) {
            mPlayerImpl = new MediaPlayerImpl(context, attributes);
        } else if (type == PlayerType.EXO_PLAYER) {
            mPlayerImpl = new ExoPlayerImpl(context, attributes);
        } else if (type == PlayerType.IJK_PLAYER) {
            mPlayerImpl = new IjkPlayerImpl(context, attributes);
        }
    }

    @Override
    public void startLocalProxy(String url, HashMap<String, String> headers) {
        mPlayerImpl.startLocalProxy(url, headers);
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mPlayerImpl.setDataSource(path);
    }

    @Override
    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
        mPlayerImpl.setDataSource(fd);
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mPlayerImpl.setDataSource(context, uri);
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mPlayerImpl.setDataSource(context, uri, headers);
    }

    @Override
    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        mPlayerImpl.setDataSource(fd, offset, length);
    }

    @Override
    public void setSurface(Surface surface) {
        mPlayerImpl.setSurface(surface);
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        mPlayerImpl.setOnPreparedListener(listener);
    }

    @Override
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
        mPlayerImpl.setOnVideoSizeChangedListener(listener);
    }

    @Override
    public void setOnLocalProxyCacheListener(OnLocalProxyCacheListener listener) {
        mPlayerImpl.setOnLocalProxyCacheListener(listener);
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        mPlayerImpl.prepareAsync();
    }

    @Override
    public void start() throws IllegalStateException {
        mPlayerImpl.start();
    }

    @Override
    public void pause() throws IllegalStateException {
        mPlayerImpl.pause();
    }

    @Override
    public void setSpeed(float speed) {
        mPlayerImpl.setSpeed(speed);
    }

    @Override
    public void stop() throws IllegalStateException {
        mPlayerImpl.stop();
    }

    @Override
    public void release() {
        mPlayerImpl.release();
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        mPlayerImpl.seekTo(msec);
    }

    @Override
    public long getCurrentPosition() {
        return mPlayerImpl.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        return mPlayerImpl.getDuration();
    }

    @Override
    public boolean isPlaying() {
        return mPlayerImpl.isPlaying();
    }
}

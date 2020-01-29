package com.android.player.impl;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;

import com.google.android.exoplayer2.SimpleExoPlayer;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;

public class ExoPlayerImpl extends PlayerImpl {

    private SimpleExoPlayer mPlayer;

    public ExoPlayerImpl(Context context) {
        super(context);
    }

    @Override
    public void setDataSource(FileDescriptor fd, long offset, long length) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
    }

    @Override
    public void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException {
    }

    @Override
    public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
    }

    @Override
    public void setSurface(Surface surface) {
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
    }

    @Override
    public void start() throws IllegalStateException {
    }

    @Override
    public void pause() throws IllegalStateException {

    }

    @Override
    public void stop() throws IllegalStateException {

    }

    @Override
    public void release() {
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
}

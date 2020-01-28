package com.android.media;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;

import java.io.IOException;

public class PlayerActivity extends Activity implements View.OnClickListener {

    private TextureView mVideoView;
    private ImageButton mControlBtn;

    private MediaPlayer mPlayer;
    private Surface mSurface;
    private String mUrl = "http://gv.vivo.com.cn/appstore/gamecenter/upload/video/201701/2017011314414026850.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();
    }

    private void initViews() {
        mVideoView = (TextureView) findViewById(R.id.video_view);
        mControlBtn = (ImageButton) findViewById(R.id.video_control_btn);

        mControlBtn.setOnClickListener(this);

        mVideoView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    private void initPlayer() throws IOException {
        mPlayer = new MediaPlayer();
        Uri uri = Uri.parse(mUrl);
        mPlayer.setDataSource(this, uri);
        mPlayer.setSurface(mSurface);
        mPlayer.setOnPreparedListener(mPreparedListener);
        mPlayer.prepareAsync();
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurface = new Surface(surface);
            try {
                initPlayer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            if (mPlayer != null) {
                mPlayer.start();
            }
        }
    };

    @Override
    public void onClick(View view) {
        if(view == mControlBtn) {

        }
    }
}

package com.android.media;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.baselib.utils.LogUtils;
import com.android.baselib.utils.ScreenUtils;
import com.android.baselib.utils.Utility;
import com.android.player.CommonPlayer;
import com.android.player.IPlayer;
import com.android.player.PlayerAttributes;
import com.android.player.PlayerType;
import com.media.cache.Video;

import java.io.IOException;

public class DownloadPlayActivity extends Activity implements View.OnClickListener {

    private String mProxyUrl;
    private String mOriginUrl;

    private TextureView mVideoView;
    private ImageButton mControlBtn;
    private TextView mTimeView;
    private SeekBar mProgressView;
    private CommonPlayer mPlayer;
    private Surface mSurface;

    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mVideoWidth;
    private int mVideoHeight;
    private float mPixelRatio; //SAR
    private long mDuration = 0L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_play);

        mProxyUrl = getIntent().getStringExtra("proxy_url");
        mOriginUrl = getIntent().getStringExtra("origin_url");

        mSurfaceWidth = ScreenUtils.getScreenWidth(this);
        initViews();

        LogUtils.w("litianpeng proxyUrl = " + mProxyUrl);
    }

    private void initViews() {
        mVideoView = (TextureView) findViewById(R.id.download_video_view);
        mProgressView = (SeekBar) findViewById(R.id.download_progress_view);
        mControlBtn = (ImageButton) findViewById(R.id.download_control_btn);
        mTimeView = (TextView) findViewById(R.id.download_time_view);

        mVideoView.setSurfaceTextureListener(mSurfaceTextureListener);
        mControlBtn.setOnClickListener(this);
        mProgressView.setOnSeekBarChangeListener(mSeekBarChangeListener);
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurface = new Surface(surface);
            initPlayer();
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

    private void initPlayer() {

        PlayerAttributes attributes = new PlayerAttributes();
        attributes.setVideoCacheSwitch(true);
        attributes.setTaskMode(Video.TaskMode.DOWNLOAD_PLAY_MODE);

        mPlayer = new CommonPlayer(this, PlayerType.EXO_PLAYER, attributes);
        Uri uri = Uri.parse(mProxyUrl);
        try {
            mPlayer.setDataSource(DownloadPlayActivity.this, uri);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        mPlayer.setOriginUrl(mOriginUrl);
        mPlayer.setSurface(mSurface);
        mPlayer.setOnPreparedListener(mPreparedListener);
        mPlayer.setOnErrorListener(mErrorListener);
        mPlayer.setOnVideoSizeChangedListener(mVideoSizeChangeListener);
        mPlayer.setOnLocalProxyCacheListener(mProxyCacheListener);
        mPlayer.prepareAsync();
    }

    private IPlayer.OnPreparedListener mPreparedListener = new IPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IPlayer mp) {
            doPlayVideo();
        }
    };

    private IPlayer.OnErrorListener mErrorListener = new IPlayer.OnErrorListener() {
        @Override
        public void onError(IPlayer mp, int what, String msg) {
            Toast.makeText(DownloadPlayActivity.this, "Play Error", Toast.LENGTH_SHORT).show();
        }
    };

    private IPlayer.OnVideoSizeChangedListener mVideoSizeChangeListener = new IPlayer.OnVideoSizeChangedListener() {

        @Override
        public void onVideoSizeChanged(IPlayer mp, int width, int height, int rotationDegree, float pixelRatio, float darRatio) {

            LogUtils.d("PlayerActivity onVideoSizeChanged width="+width+", height="+height + ", pixedlRatio = " + pixelRatio);
            mVideoWidth = width;
            mVideoHeight = height;
            mPixelRatio = pixelRatio;
            mSurfaceHeight = (int)(mSurfaceWidth * mVideoHeight * 1.0f / mVideoWidth);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mSurfaceWidth, mSurfaceHeight);
            mVideoView.setLayoutParams(params);
        }
    };

    @Override
    public void onClick(View view) {
        LogUtils.e("click event");
        if(view == mControlBtn) {
            if (!mPlayer.isPlaying()) {
                mPlayer.start();
                mControlBtn.setImageResource(R.drawable.played_state);
            } else {
                mPlayer.pause();
                mControlBtn.setImageResource(R.drawable.paused_state);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPlayer != null) {
            mPlayer.pause();
            mControlBtn.setImageResource(R.drawable.paused_state);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doReleasePlayer();
    }

    private void doPlayVideo() {
        if (mPlayer != null) {
            mTimeView.setVisibility(View.VISIBLE);
            mPlayer.start();
            mDuration = mPlayer.getDuration();
            mControlBtn.setImageResource(R.drawable.played_state);
            mHandler.sendEmptyMessage(MSG_UPDATE_PROGRESS);
        }
    }

    private void doReleasePlayer() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    private static final int MSG_UPDATE_PROGRESS = 0x1;
    private static final int MAX_PROGRESS = 1000;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_PROGRESS) {
                updateProgressView();
            }
        }
    };

    private void updateProgressView() {
        if (mPlayer != null) {
            long currentPosition = mPlayer.getCurrentPosition();
            mTimeView.setText(Utility.getVideoTimeString(currentPosition) + " / " + Utility.getVideoTimeString(mDuration));
//            mProgressView.setProgress((int)(1000 *  currentPosition * 1.0f / mDuration));
//            int cacheProgress = (int)(mPercent * 1.0f / 100 * 1000);
//            mProgressView.setSecondaryProgress(cacheProgress);
        }
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 1000);
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (mPlayer != null) {
                mHandler.removeMessages(MSG_UPDATE_PROGRESS);
            }
            LogUtils.d("onStartTrackingTouch progress="+mProgressView.getProgress());
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            LogUtils.d("onStopTrackingTouch progress="+mProgressView.getProgress());

            if (mPlayer != null) {
                int progress = mProgressView.getProgress();
                int seekPosition = (int)(progress * 1.0f / MAX_PROGRESS * mDuration);
                mPlayer.seekTo(seekPosition);

                mHandler.sendEmptyMessage(MSG_UPDATE_PROGRESS);
            }
        }
    };

    private IPlayer.OnLocalProxyCacheListener mProxyCacheListener = new IPlayer.OnLocalProxyCacheListener() {
        @Override
        public void onCacheReady(IPlayer mp, String proxyUrl) {

        }

        @Override
        public void onCacheProgressChanged(IPlayer mp, int percent, long cachedSize) {

        }

        @Override
        public void onCacheSpeedChanged(IPlayer mp, float speed) {

        }

        @Override
        public void onCacheForbidden(IPlayer mp, String url) {

        }

        @Override
        public void onCacheFinished(IPlayer mp) {

        }
    };
}

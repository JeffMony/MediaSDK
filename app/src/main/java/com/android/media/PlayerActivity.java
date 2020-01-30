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

import com.android.media.utils.ScreenUtils;
import com.android.player.CommonPlayer;
import com.android.player.IPlayer;
import com.android.player.PlayerAttributes;
import com.android.player.PlayerType;
import com.android.player.utils.LogUtils;
import com.android.player.utils.TimeUtils;

import java.io.IOException;

public class PlayerActivity extends Activity implements View.OnClickListener {

    private TextureView mVideoView;
    private ImageButton mControlBtn;
    private TextView mTimeView;
    private SeekBar mProgressView;

    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mVideoWidth;
    private int mVideoHeight;
    private long mDuration = 0L;

    private CommonPlayer mPlayer;
    private Surface mSurface;
    private String mUrl = "https://ll1.zhengzhuji.com/hls/20181111/8a1f15ba7a8f0ca5418229a0cdd7bd92/1541946502/index.m3u8";
    private int mPlayerType = -1;
    private boolean mUseLocalProxy = false;
    private int mPercent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        mUrl = getIntent().getStringExtra("url");
        mPlayerType = getIntent().getIntExtra("playerType", -1);
        if (mPlayerType == -1) {
            mPlayerType = 1;
        }
        mUseLocalProxy = getIntent().getBooleanExtra("useLocalProxy", false);
        mSurfaceWidth = ScreenUtils.getScreenWidth(this);
        initViews();
    }

    private void initViews() {
        mVideoView = (TextureView) findViewById(R.id.video_view);
        mTimeView = (TextView) findViewById(R.id.video_time_view);
        mProgressView = (SeekBar) findViewById(R.id.video_progress_view);
        mControlBtn = (ImageButton) findViewById(R.id.video_control_btn);


        mControlBtn.setOnClickListener(this);
        mVideoView.setSurfaceTextureListener(mSurfaceTextureListener);
        mProgressView.setOnSeekBarChangeListener(mSeekBarChangeListener);
    }

    private void initPlayer() {

        PlayerAttributes attributes = new PlayerAttributes();
        attributes.setUseLocalProxy(mUseLocalProxy);

        if (mPlayerType == 1) {
            mPlayer = new CommonPlayer(this, PlayerType.IJK_PLAYER, attributes);
        } else if (mPlayerType == 2) {
            mPlayer = new CommonPlayer(this, PlayerType.EXO_PLAYER, attributes);
        } else if (mPlayerType == 3) {
            mPlayer = new CommonPlayer(this, PlayerType.MEDIA_PLAYER, attributes);
        }

        if (mUseLocalProxy) {
            mPlayer.setOnLocalProxyCacheListener(mOnLocalProxyCacheListener);
            mPlayer.startLocalProxy(mUrl, null);
        } else {
            Uri uri = Uri.parse(mUrl);
            try {
                mPlayer.setDataSource(PlayerActivity.this, uri);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            mPlayer.setSurface(mSurface);
            mPlayer.setOnPreparedListener(mPreparedListener);
            mPlayer.setOnVideoSizeChangedListener(mVideoSizeChangeListener);
            mPlayer.prepareAsync();
        }
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

    @Override
    protected void onPause() {
        super.onPause();
        if (mPlayer != null) {
            mPlayer.pause();
            mControlBtn.setImageResource(R.drawable.paused_state);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doReleasePlayer();
    }

    private IPlayer.OnPreparedListener mPreparedListener = new IPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IPlayer mp) {
            doPlayVideo();
        }
    };

    private IPlayer.OnVideoSizeChangedListener mVideoSizeChangeListener = new IPlayer.OnVideoSizeChangedListener() {

        @Override
        public void onVideoSizeChanged(IPlayer mp, int width, int height, int rotationDegree, float pixelRatio) {

            LogUtils.d("PlayerActivity onVideoSizeChanged width="+width+", height="+height);
            mVideoWidth = width;
            mVideoHeight = height;
            mSurfaceHeight = (int)(mSurfaceWidth * mVideoHeight * 1.0f / mVideoWidth);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mSurfaceWidth, mSurfaceHeight);
            mVideoView.setLayoutParams(params);
        }
    };

    private IPlayer.OnLocalProxyCacheListener mOnLocalProxyCacheListener = new IPlayer.OnLocalProxyCacheListener() {
        @Override
        public void onCacheReady(IPlayer mp, String proxyUrl) {
            LogUtils.w("onCacheReady proxyUrl = " + proxyUrl);
            Uri uri = Uri.parse(proxyUrl);
            try {
                mPlayer.setDataSource(PlayerActivity.this, uri);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            mPlayer.setSurface(mSurface);
            mPlayer.setOnPreparedListener(mPreparedListener);
            mPlayer.setOnVideoSizeChangedListener(mVideoSizeChangeListener);
            mPlayer.prepareAsync();
        }

        @Override
        public void onCacheProgressChanged(IPlayer mp, int percent, long cachedSize) {
            LogUtils.w("onCacheProgressChanged percent = " + percent);
            mPercent = percent;
        }

        @Override
        public void onCacheForbidden(IPlayer mp, String url) {
            LogUtils.w("onCacheForbidden url = " + url);
        }
    };

    private static final int MSG_UPDATE_PROGRESS = 0x1;
    private static final int MSG_UPDATE_CACHE_PROGRESS = 0x2;
    private static final int MAX_PROGRESS = 1000;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_PROGRESS) {
                updateProgressView();
            } else if (msg.what == MSG_UPDATE_CACHE_PROGRESS) {

            }
        }
    };

    @Override
    public void onClick(View view) {
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

    private void doPlayVideo() {
        if (mPlayer != null) {
            mPlayer.start();
            mControlBtn.setImageResource(R.drawable.played_state);
            mDuration = mPlayer.getDuration();
            LogUtils.d("total duration ="+mDuration +", timeString="+ TimeUtils.getVideoTimeString(mDuration));
            mHandler.sendEmptyMessage(MSG_UPDATE_PROGRESS);
        }
    }

    private void updateProgressView() {
        if (mPlayer != null) {
            long currentPosition = mPlayer.getCurrentPosition();
            mTimeView.setText(TimeUtils.getVideoTimeString(currentPosition) + " / " + TimeUtils.getVideoTimeString(mDuration));

            mProgressView.setProgress((int)(1000 *  currentPosition * 1.0f / mDuration));
            mProgressView.setSecondaryProgress((int)(mPercent * 1.0f / 100 * 1000));
        }
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 1000);
    }

    private void doReleasePlayer() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }
}

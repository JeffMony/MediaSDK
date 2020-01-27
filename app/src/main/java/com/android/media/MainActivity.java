package com.android.media;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;

public class MainActivity extends Activity implements View.OnClickListener {

    private TextureView mVideoView;
    private ImageButton mControlBtn;

    private MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initPlayer();
    }

    private void initViews() {
        mVideoView = (TextureView) findViewById(R.id.video_view);
        mControlBtn = (ImageButton) findViewById(R.id.video_control_btn);

        mControlBtn.setOnClickListener(this);
    }

    private void initPlayer() {

    }

    @Override
    public void onClick(View view) {
        if(view == mControlBtn) {

        }
    }
}

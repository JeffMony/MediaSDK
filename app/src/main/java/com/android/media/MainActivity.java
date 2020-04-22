package com.android.media;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mPlayBtn;
    private Button mDownloadBtn;
    private Button mScanBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayBtn = (Button) findViewById(R.id.play_btn);
        mDownloadBtn = (Button) findViewById(R.id.download_btn);
        mScanBtn = (Button) findViewById(R.id.scan_btn);
        mPlayBtn.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);
        mScanBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mPlayBtn) {
            Intent intent = new Intent(this, PlayFeatureActivity.class);
            startActivity(intent);
        } else if (v == mDownloadBtn) {
            Intent intent = new Intent(this, DownloadFeatureActivity.class);
            startActivity(intent);
        } else if (v == mScanBtn) {
            Intent intent = new Intent(this, MediaScannerActivity.class);
            startActivity(intent);
        }
    }
}

package com.android.media;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class DownloadFeatureActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mDownloadConfigBtn;
    private Button mDownloadBaseBtn;
    private Button mDownloadOrcodeBtn;
    private Button mCurrentDownloadBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_feature);

        initViews();
    }

    private void initViews() {
        mDownloadConfigBtn = (Button) findViewById(R.id.download_config_btn);
        mDownloadBaseBtn = (Button) findViewById(R.id.download_base_btn);
        mDownloadOrcodeBtn = (Button) findViewById(R.id.download_orcode_btn);
        mCurrentDownloadBtn = (Button) findViewById(R.id.current_download_btn);

        mDownloadConfigBtn.setOnClickListener(this);
        mDownloadBaseBtn.setOnClickListener(this);
        mDownloadOrcodeBtn.setOnClickListener(this);
        mCurrentDownloadBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mDownloadConfigBtn) {

        } else if (v == mDownloadBaseBtn) {
            Intent intent = new Intent(DownloadFeatureActivity.this, DownloadBaseListActivity.class);
            startActivity(intent);
        } else if (v == mDownloadOrcodeBtn) {
            Intent intent = new Intent(DownloadFeatureActivity.this, DownloadOrcodeActivity.class);
            startActivity(intent);
        } else if (v == mCurrentDownloadBtn) {

        }
    }
}

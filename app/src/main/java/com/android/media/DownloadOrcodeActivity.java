package com.android.media;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

public class DownloadOrcodeActivity extends Activity implements View.OnClickListener {

    private EditText mDownloadUrlText;
    private Button mSingleDownloadBtn;
    private Button mOrcodeScannerBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orcode);

        initViews();
    }

    private void initViews() {
        mDownloadUrlText = (EditText) findViewById(R.id.download_url_text);
        mSingleDownloadBtn = (Button) findViewById(R.id.download_single_btn);
        mOrcodeScannerBtn = (Button) findViewById(R.id.orcode_scanner_btn);

        mSingleDownloadBtn.setOnClickListener(this);
        mOrcodeScannerBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mSingleDownloadBtn) {

        } else if (v == mOrcodeScannerBtn) {

        }
    }
}

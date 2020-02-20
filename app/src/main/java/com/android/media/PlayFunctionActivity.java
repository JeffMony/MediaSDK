package com.android.media;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.JsonToken;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.baselib.utils.LogUtils;
import com.android.player.proxy.CacheManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PlayFunctionActivity extends Activity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener, CompoundButton.OnCheckedChangeListener {

    private static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final int REQUEST_PERMISSION_OK = 0x1;

    private EditText mVideoUrlView;
    private Button mPlayBtn;
    private ListView mVideoListView;

    private RadioGroup mPlayerBtnGroup;
    private RadioButton mIjkPlayerBtn;
    private RadioButton mExoPlayerBtn;
    private RadioButton mMediaPlayerBtn;

    private CheckBox mVideoCacheBox;
    private TextView mCachedLocationView;
    private LinearLayout mCacheLayout;
    private TextView mCacheSizeView;
    private TextView mClearCacheView;

    private List<HashMap<String, String>> mVideoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_func);

        initViews();
        initViewListData();
    }

    private void initViews() {
        mVideoUrlView = (EditText) findViewById(R.id.video_url_view);
        mPlayBtn = (Button) findViewById(R.id.play_btn);
        mVideoListView = (ListView) findViewById(R.id.video_list);
        mPlayerBtnGroup = (RadioGroup) findViewById(R.id.play_btn_group);
        mIjkPlayerBtn = (RadioButton) findViewById(R.id.ijkplayer_btn);
        mExoPlayerBtn = (RadioButton) findViewById(R.id.exoplayer_btn);
        mMediaPlayerBtn = (RadioButton) findViewById(R.id.mediaplayer_btn);
        mVideoCacheBox = (CheckBox) findViewById(R.id.local_proxy_box);
        mCachedLocationView = (TextView) findViewById(R.id.cached_location_view);
        mCacheLayout = (LinearLayout) findViewById(R.id.cache_layout);
        mCacheSizeView = (TextView) findViewById(R.id.cache_size_view);
        mClearCacheView = (TextView) findViewById(R.id.clear_cache_view);

        mIjkPlayerBtn.setChecked(true);

        mPlayBtn.setOnClickListener(this);
        mClearCacheView.setOnClickListener(this);

        mPlayerBtnGroup.setOnCheckedChangeListener(this);
        mVideoCacheBox.setOnCheckedChangeListener(this);

        mCachedLocationView.setText(CacheManager.getCachePath());
    }

    private void initViewListData() {
        mVideoList = new ArrayList<>();
        try {
            InputStream is = null;
            try {
                String PATH = Environment.getExternalStorageDirectory() + "/list.json";
                File file = new File(PATH);

                if (file.exists()) {
                    is = new FileInputStream(PATH);
                } else {
                    is = getAssets().open("list.json");
                }
                JsonReader reader = new JsonReader(new InputStreamReader(is));
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.beginObject();
                    HashMap<String, String> item = new HashMap<>();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if (name.equals("name")) {
                            String videoName = reader.nextString();
                            item.put("name", videoName);
                        } else if (name.equals("age") || reader.peek() != JsonToken.NULL) { // 当前获取的字段是否为：null
                            String videoUrl = reader.nextString();
                            item.put("url", videoUrl);
                        }
                    }
                    reader.endObject();
                    mVideoList.add(item);
                }
                reader.endArray();
            } finally {
                if (null != is) {
                    is.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, mVideoList, R.layout.video_item, new String[]{"name"}, new int[]{R.id.video_name});
        mVideoListView.setAdapter(adapter);
        mVideoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = mVideoList.get(position).get("url");
                mVideoUrlView.setText(url);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();

        mCacheSizeView.setText(CacheManager.getCachedSize());
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { WRITE_EXTERNAL_STORAGE }, REQUEST_PERMISSION_OK);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_OK) {
            if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,  "存储权限已开通", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,  "存储权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mPlayBtn) {
            doPlayVideo();
        } else if (v == mClearCacheView) {
            clearVideoCache();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        LogUtils.d("onCheckedChanged checkedId = " + checkedId);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            mCachedLocationView.setVisibility(View.VISIBLE);
            mCacheLayout.setVisibility(View.VISIBLE);
        } else{
            mCachedLocationView.setVisibility(View.GONE);
            mCacheLayout.setVisibility(View.GONE);
        }
    }

    private void doPlayVideo() {
        String url = mVideoUrlView.getText().toString();
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this,  "输入的url为空", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("url", url);

            int playerType = -1;
            if (mIjkPlayerBtn.isChecked()) {
                playerType = 1;
            } else if (mExoPlayerBtn.isChecked()) {
                playerType = 2;
            } else if (mMediaPlayerBtn.isChecked()) {
                playerType = 3;
            }
            intent.putExtra("playerType", playerType);
            boolean videoCached = mVideoCacheBox.isChecked();
            intent.putExtra("videoCached", videoCached);

            startActivity(intent);
        }
    }

    private void clearVideoCache() {
        CacheManager.deleteCacheFile();
        mCacheSizeView.setText("0 MB");
    }
}

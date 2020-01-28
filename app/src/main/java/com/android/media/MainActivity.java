package com.android.media;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.JsonReader;
import android.util.JsonToken;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

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

public class MainActivity extends Activity {

    private static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final int REQUEST_PERMISSION_OK = 0x1;

    private ListView mVideoListView;
    private List<HashMap<String, String>> mVideoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initViewListData();
    }

    private void initViews() {
        mVideoListView = (ListView) findViewById(R.id.video_list);
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

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();

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
            if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,  "存储权限已开通", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,  "存储权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

}

package com.android.media;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.cache.model.VideoTaskItem;
import com.media.cache.model.VideoTaskState;

public class VideoListAdapter extends ArrayAdapter<VideoTaskItem> {

    public VideoListAdapter(Context context, int resource, VideoTaskItem[] items) {
        super(context, resource, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.download_item, null);
        VideoTaskItem item = getItem(position);
        TextView urlTextView = (TextView) view.findViewById(R.id.url_text);
        urlTextView.setText(item.getUrl());
        TextView stateTextView = (TextView) view.findViewById(R.id.status_txt);
        setStateText(stateTextView, item);
        TextView infoTextView = (TextView) view.findViewById(R.id.download_txt);
        setDownloadInfoText(infoTextView, item);
        return view;
    }

    private void setStateText(TextView stateView, VideoTaskItem item) {
        switch (item.getTaskState()) {
            case VideoTaskState.PENDING:
                stateView.setText("等待中");
                break;
            case VideoTaskState.PREPARE:
                stateView.setText("准备好");
                break;
            case VideoTaskState.START:
                stateView.setText("开始下载");
                break;
            case VideoTaskState.DOWNLOADING:
                if (item.getProxyReady()) {
                    stateView.setText("下载中...(可播放)");
                } else {
                    stateView.setText("下载中...");
                }
                break;
            case VideoTaskState.PAUSE:
                stateView.setText("下载暂停");
                break;
            case VideoTaskState.SUCCESS:
                stateView.setText("下载完成");
                break;
            case VideoTaskState.ERROR:
                stateView.setText("下载错误");
                break;
            default:
                stateView.setText("未下载");
                break;

        }
    }

    private void setDownloadInfoText(TextView infoView, VideoTaskItem item) {
        switch (item.getTaskState()) {
            case VideoTaskState.DOWNLOADING:
                infoView.setText("进度:" + item.getPercentString() + ", 速度:" + item.getSpeedString());
                break;
            case VideoTaskState.SUCCESS:
                infoView.setText("进度:" + item.getPercentString());
                break;
            case VideoTaskState.PAUSE:
                infoView.setText("进度:" + item.getPercentString());
                break;
            default:
                break;
        }
    }

    public void notifyChanged(VideoTaskItem[] items, VideoTaskItem item) {
        for (int index = 0; index < getCount(); index++) {
            if (getItem(index).equals(item)) {
                items[index] = item;
                notifyDataSetChanged();
            }
        }
    }
}

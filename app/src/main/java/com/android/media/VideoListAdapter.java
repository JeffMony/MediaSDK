package com.android.media;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.cache.model.VideoItem;

public class VideoListAdapter extends ArrayAdapter<VideoItem> {

    public VideoListAdapter(Context context, int resource, VideoItem[] items) {
        super(context, resource, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.download_item, null);
        VideoItem item = getItem(position);
        TextView urlTextView = (TextView) view.findViewById(R.id.url_text);
        urlTextView.setText(item.getUrl());
        return view;
    }
}

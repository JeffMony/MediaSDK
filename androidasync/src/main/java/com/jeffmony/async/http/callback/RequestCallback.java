package com.jeffmony.async.http.callback;

import com.jeffmony.async.callback.ResultCallback;
import com.jeffmony.async.http.AsyncHttpResponse;

public interface RequestCallback<T> extends ResultCallback<AsyncHttpResponse, T> {
    void onConnect(AsyncHttpResponse response);
    void onProgress(AsyncHttpResponse response, long downloaded, long total);
}

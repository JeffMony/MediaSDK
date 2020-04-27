package com.jeffmony.async.http.callback;

import com.jeffmony.async.http.AsyncHttpResponse;

public interface HttpConnectCallback {
    void onConnectCompleted(Exception ex, AsyncHttpResponse response);
}

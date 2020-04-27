package com.jeffmony.async.http.server;


public interface HttpServerRequestCallback {
    void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response);
}

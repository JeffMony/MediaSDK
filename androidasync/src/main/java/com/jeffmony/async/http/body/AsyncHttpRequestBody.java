package com.jeffmony.async.http.body;

import com.jeffmony.async.DataEmitter;
import com.jeffmony.async.DataSink;
import com.jeffmony.async.callback.CompletedCallback;
import com.jeffmony.async.http.AsyncHttpRequest;

public interface AsyncHttpRequestBody<T> {
    void write(AsyncHttpRequest request, DataSink sink, CompletedCallback completed);
    void parse(DataEmitter emitter, CompletedCallback completed);
    String getContentType();
    boolean readFullyOnRequest();
    int length();
    T get();
}

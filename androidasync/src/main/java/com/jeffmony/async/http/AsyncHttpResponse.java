package com.jeffmony.async.http;

import com.jeffmony.async.AsyncSocket;
import com.jeffmony.async.DataEmitter;

public interface AsyncHttpResponse extends DataEmitter {
    String protocol();
    String message();
    int code();
    Headers headers();
    AsyncSocket detachSocket();
    AsyncHttpRequest getRequest();
}

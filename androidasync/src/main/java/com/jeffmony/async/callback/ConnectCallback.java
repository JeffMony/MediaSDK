package com.jeffmony.async.callback;

import com.jeffmony.async.AsyncSocket;

public interface ConnectCallback {
    void onConnectCompleted(Exception ex, AsyncSocket socket);
}

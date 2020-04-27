package com.jeffmony.async.callback;

import com.jeffmony.async.AsyncServerSocket;
import com.jeffmony.async.AsyncSocket;

public interface ListenCallback extends CompletedCallback {
    void onAccepted(AsyncSocket socket);
    void onListening(AsyncServerSocket socket);
}

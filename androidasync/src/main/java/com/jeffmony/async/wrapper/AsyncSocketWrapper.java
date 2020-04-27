package com.jeffmony.async.wrapper;

import com.jeffmony.async.AsyncSocket;

public interface AsyncSocketWrapper extends AsyncSocket, DataEmitterWrapper {
    AsyncSocket getSocket();
}

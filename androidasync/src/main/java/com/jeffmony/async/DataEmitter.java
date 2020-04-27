package com.jeffmony.async;

import com.jeffmony.async.callback.CompletedCallback;
import com.jeffmony.async.callback.DataCallback;

public interface DataEmitter {
    void setDataCallback(DataCallback callback);
    DataCallback getDataCallback();
    boolean isChunked();
    void pause();
    void resume();
    void close();
    boolean isPaused();
    void setEndCallback(CompletedCallback callback);
    CompletedCallback getEndCallback();
    AsyncServer getServer();
    String charset();
}

package com.jeffmony.async.callback;

/**
 * Created by koush on 7/5/16.
 */
public interface ValueCallback<T> {
    void onResult(T value);
}

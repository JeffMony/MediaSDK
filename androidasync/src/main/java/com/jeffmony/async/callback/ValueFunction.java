package com.jeffmony.async.callback;

public interface ValueFunction<T> {
    T getValue() throws Exception;
}

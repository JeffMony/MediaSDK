package com.jeffmony.async.future;

public interface SuccessCallback<T> {
    void success(T value) throws Exception;
}

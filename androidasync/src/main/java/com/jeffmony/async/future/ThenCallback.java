package com.jeffmony.async.future;

public interface ThenCallback<T, F> {
    /**
     * Callback that is invoked when Future.then completes,
     * and converts a value F to value T.
     * @param from
     * @return
     * @throws Exception
     */
    T then(F from) throws Exception;
}

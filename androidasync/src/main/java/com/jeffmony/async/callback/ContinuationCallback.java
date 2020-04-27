package com.jeffmony.async.callback;

import com.jeffmony.async.future.Continuation;

public interface ContinuationCallback {
    void onContinue(Continuation continuation, CompletedCallback next) throws Exception;
}

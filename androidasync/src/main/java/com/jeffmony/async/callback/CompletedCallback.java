package com.jeffmony.async.callback;

public interface CompletedCallback {
    class NullCompletedCallback implements CompletedCallback {
        @Override
        public void onCompleted(Exception ex) {

        }
    }

    public void onCompleted(Exception ex);
}

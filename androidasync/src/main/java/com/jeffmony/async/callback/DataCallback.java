package com.jeffmony.async.callback;

import com.jeffmony.async.ByteBufferList;
import com.jeffmony.async.DataEmitter;

public interface DataCallback {
    class NullDataCallback implements DataCallback {
        @Override
        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            bb.recycle();
        }
    }

    void onDataAvailable(DataEmitter emitter, ByteBufferList bb);
}

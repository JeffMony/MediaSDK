package com.jeffmony.async.stream;

import com.jeffmony.async.ByteBufferList;
import com.jeffmony.async.DataEmitter;
import com.jeffmony.async.callback.CompletedCallback;
import com.jeffmony.async.callback.DataCallback;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class OutputStreamDataCallback implements DataCallback, CompletedCallback {
    private OutputStream mOutput;
    public OutputStreamDataCallback(OutputStream os) {
        mOutput = os;
    }

    public OutputStream getOutputStream() {
        return mOutput;
    }

    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        try {
            while (bb.size() > 0) {
                ByteBuffer b = bb.remove();
                mOutput.write(b.array(), b.arrayOffset() + b.position(), b.remaining());
                ByteBufferList.reclaim(b);
            }
        }
        catch (Exception ex) {
            onCompleted(ex);
        }
        finally {
            bb.recycle();
        }
    }
    
    public void close() {
        try {
            mOutput.close();
        }
        catch (IOException e) {
            onCompleted(e);
        }
    }

    @Override
    public void onCompleted(Exception error) {
        error.printStackTrace();       
    }
}

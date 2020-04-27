package com.jeffmony.async.http.body;

import com.jeffmony.async.DataSink;
import com.jeffmony.async.Util;
import com.jeffmony.async.callback.CompletedCallback;
import com.jeffmony.async.http.NameValuePair;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public abstract class StreamPart extends Part {
    public StreamPart(String name, long length, List<NameValuePair> contentDisposition) {
        super(name, length, contentDisposition);
    }
    
    @Override
    public void write(DataSink sink, CompletedCallback callback) {
        try {
            InputStream is = getInputStream();
            Util.pump(is, sink, callback);
        }
        catch (Exception e) {
            callback.onCompleted(e);
        }
    }
    
    protected abstract InputStream getInputStream() throws IOException;
}

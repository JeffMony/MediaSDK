package com.jeffmony.async.http.body;

import com.jeffmony.async.DataEmitter;
import com.jeffmony.async.DataSink;
import com.jeffmony.async.Util;
import com.jeffmony.async.callback.CompletedCallback;
import com.jeffmony.async.future.FutureCallback;
import com.jeffmony.async.http.AsyncHttpRequest;
import com.jeffmony.async.parser.JSONObjectParser;

import org.json.JSONObject;

public class JSONObjectBody implements AsyncHttpRequestBody<JSONObject> {
    public JSONObjectBody() {
    }
    
    byte[] mBodyBytes;
    JSONObject json;
    public JSONObjectBody(JSONObject json) {
        this();
        this.json = json;
    }

    @Override
    public void parse(DataEmitter emitter, final CompletedCallback completed) {
        new JSONObjectParser().parse(emitter).setCallback(new FutureCallback<JSONObject>() {
            @Override
            public void onCompleted(Exception e, JSONObject result) {
                json = result;
                completed.onCompleted(e);
            }
        });
    }

    @Override
    public void write(AsyncHttpRequest request, DataSink sink, final CompletedCallback completed) {
        Util.writeAll(sink, mBodyBytes, completed);
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public boolean readFullyOnRequest() {
        return true;
    }

    @Override
    public int length() {
        mBodyBytes = json.toString().getBytes();
        return mBodyBytes.length;
    }

    public static final String CONTENT_TYPE = "application/json";

    @Override
    public JSONObject get() {
        return json;
    }
}


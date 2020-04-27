package com.jeffmony.async.http.body;

import com.jeffmony.async.DataEmitter;
import com.jeffmony.async.DataSink;
import com.jeffmony.async.Util;
import com.jeffmony.async.callback.CompletedCallback;
import com.jeffmony.async.future.FutureCallback;
import com.jeffmony.async.http.AsyncHttpRequest;
import com.jeffmony.async.parser.JSONArrayParser;

import org.json.JSONArray;

public class JSONArrayBody implements AsyncHttpRequestBody<JSONArray> {
    public JSONArrayBody() {
    }

    byte[] mBodyBytes;
    JSONArray json;
    public JSONArrayBody(JSONArray json) {
        this();
        this.json = json;
    }

    @Override
    public void parse(DataEmitter emitter, final CompletedCallback completed) {
        new JSONArrayParser().parse(emitter).setCallback(new FutureCallback<JSONArray>() {
            @Override
            public void onCompleted(Exception e, JSONArray result) {
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
        return "application/json";
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
    public JSONArray get() {
        return json;
    }
}


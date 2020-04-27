package com.jeffmony.async.http.server;

import com.jeffmony.async.AsyncSocket;
import com.jeffmony.async.DataEmitter;
import com.jeffmony.async.http.Headers;
import com.jeffmony.async.http.Multimap;
import com.jeffmony.async.http.body.AsyncHttpRequestBody;

import java.util.Map;
import java.util.regex.Matcher;

public interface AsyncHttpServerRequest extends DataEmitter {
    Headers getHeaders();
    Matcher getMatcher();
    void setMatcher(Matcher matcher);
    <T extends AsyncHttpRequestBody> T getBody();
    AsyncSocket getSocket();
    String getPath();
    Multimap getQuery();
    String getMethod();
    String getUrl();

    String get(String name);
    Map<String, Object> getState();
}

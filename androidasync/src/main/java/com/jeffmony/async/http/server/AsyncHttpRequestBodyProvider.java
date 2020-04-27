package com.jeffmony.async.http.server;

import com.jeffmony.async.http.Headers;
import com.jeffmony.async.http.body.AsyncHttpRequestBody;

public interface AsyncHttpRequestBodyProvider {
    AsyncHttpRequestBody getBody(Headers headers);
}

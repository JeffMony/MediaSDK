package com.jeffmony.async.parser;

import com.jeffmony.async.DataEmitter;
import com.jeffmony.async.DataSink;
import com.jeffmony.async.callback.CompletedCallback;
import com.jeffmony.async.future.Future;

import java.lang.reflect.Type;

/**
 * Created by koush on 5/27/13.
 */
public interface AsyncParser<T> {
    Future<T> parse(DataEmitter emitter);
    void write(DataSink sink, T value, CompletedCallback completed);
    Type getType();
    String getMime();
}

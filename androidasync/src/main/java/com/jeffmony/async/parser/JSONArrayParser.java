package com.jeffmony.async.parser;

import com.jeffmony.async.DataEmitter;
import com.jeffmony.async.DataSink;
import com.jeffmony.async.callback.CompletedCallback;
import com.jeffmony.async.future.Future;

import org.json.JSONArray;

import java.lang.reflect.Type;

/**
 * Created by koush on 5/27/13.
 */
public class JSONArrayParser implements AsyncParser<JSONArray> {
    @Override
    public Future<JSONArray> parse(DataEmitter emitter) {
        return new StringParser().parse(emitter)
        .thenConvert(JSONArray::new);
    }

    @Override
    public void write(DataSink sink, JSONArray value, CompletedCallback completed) {
        new StringParser().write(sink, value.toString(), completed);
    }

    @Override
    public Type getType() {
        return JSONArray.class;
    }

    @Override
    public String getMime() {
        return "application/json";
    }
}

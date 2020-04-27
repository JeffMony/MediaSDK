package com.jeffmony.async.parser;

import com.jeffmony.async.ByteBufferList;
import com.jeffmony.async.DataEmitter;
import com.jeffmony.async.DataSink;
import com.jeffmony.async.callback.CompletedCallback;
import com.jeffmony.async.future.Future;

import java.lang.reflect.Type;
import java.nio.charset.Charset;

/**
 * Created by koush on 5/27/13.
 */
public class StringParser implements AsyncParser<String> {
    Charset forcedCharset;

    public StringParser() {
    }

    public StringParser(Charset charset) {
        this.forcedCharset = charset;
    }

    @Override
    public Future<String> parse(DataEmitter emitter) {
        final String charset = emitter.charset();
        return new ByteBufferListParser().parse(emitter)
        .thenConvert(from -> {
            Charset charsetToUse = forcedCharset;
            if (charsetToUse == null && charset != null)
                charsetToUse = Charset.forName(charset);
            return from.readString(charsetToUse);
        });
    }

    @Override
    public void write(DataSink sink, String value, CompletedCallback completed) {
        new ByteBufferListParser().write(sink, new ByteBufferList(value.getBytes()), completed);
    }

    @Override
    public Type getType() {
        return String.class;
    }

    @Override
    public String getMime() {
        return null;
    }
}

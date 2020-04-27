package com.jeffmony.async.parser;

import com.jeffmony.async.DataEmitter;
import com.jeffmony.async.DataSink;
import com.jeffmony.async.callback.CompletedCallback;
import com.jeffmony.async.future.Future;
import com.jeffmony.async.http.body.DocumentBody;
import com.jeffmony.async.stream.ByteBufferListInputStream;

import org.w3c.dom.Document;

import java.lang.reflect.Type;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by koush on 8/3/13.
 */
public class DocumentParser implements AsyncParser<Document> {
    @Override
    public Future<Document> parse(DataEmitter emitter) {
        return new ByteBufferListParser().parse(emitter)
        .thenConvert(from -> DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteBufferListInputStream(from)));
    }

    @Override
    public void write(DataSink sink, Document value, CompletedCallback completed) {
        new DocumentBody(value).write(null, sink, completed);
    }

    @Override
    public Type getType() {
        return Document.class;
    }

    @Override
    public String getMime() {
        return "text/xml";
    }
}

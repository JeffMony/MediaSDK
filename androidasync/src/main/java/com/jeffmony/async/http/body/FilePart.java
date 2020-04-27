package com.jeffmony.async.http.body;

import com.jeffmony.async.http.BasicNameValuePair;
import com.jeffmony.async.http.NameValuePair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class FilePart extends StreamPart {
    File file;
    public FilePart(String name, final File file) {
        super(name, (int)file.length(), new ArrayList<NameValuePair>() {
            {
                add(new BasicNameValuePair("filename", file.getName()));
            }
        });

//        getRawHeaders().set("Content-Type", "application/xml");

        this.file = file;
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public String toString() {
        return getName();
    }
}

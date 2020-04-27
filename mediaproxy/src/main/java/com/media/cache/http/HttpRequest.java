package com.media.cache.http;

import com.media.cache.utils.LocalProxyUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.net.ssl.SSLException;

public class HttpRequest {
    private final BufferedInputStream mInputStream;
    private final String mRemoteIP;
    private final String mRemoteHostname;
    private final HashMap<String, String> mHeaders;
    private HashMap<String, String> mParams;
    private Method mMethod;
    private String mUri;
    private String mProtocolVersion;
    private boolean mKeepAlive;
    private String mQueryParameter;

    public HttpRequest(InputStream inputStream, InetAddress inetAddress) {
        this.mInputStream = new BufferedInputStream(inputStream);

        //isLoopbackAddress() : local address; 127.0.0.0 ~ 127.255.255.255
        //isAnyLocalAddress() : normal address ?
        this.mRemoteIP = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "127.0.0.1" : inetAddress.getHostAddress();
        this.mRemoteHostname = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress() ? "localhost" : inetAddress.getHostName();
        this.mHeaders = new HashMap<String, String>();
    }

    public void parseRequest() throws Exception {
        byte[] buf = new byte[LocalProxyUtils.DEFAULT_BUFFER_SIZE];
        int splitByteIndex = 0;
        int readLength = 0;

        int read = -1;
        this.mInputStream.mark(LocalProxyUtils.DEFAULT_BUFFER_SIZE);
        try {
            read = this.mInputStream.read(buf, 0, LocalProxyUtils.DEFAULT_BUFFER_SIZE);
        } catch (SSLException e) {
            throw e;
        } catch (IOException e) {
            LocalProxyUtils.close(this.mInputStream);
            throw new SocketException("Socket Shutdown");
        }
        if (read == -1) {
            LocalProxyUtils.close(this.mInputStream);
            throw new SocketException("Can't read inputStream");
        }
        while (read > 0) {
            readLength += read;
            splitByteIndex = findResponseHeaderEnd(buf, readLength);
            if (splitByteIndex > 0) {
                break;
            }
            read = this.mInputStream.read(buf, readLength, LocalProxyUtils.DEFAULT_BUFFER_SIZE - readLength);
        }

        if (splitByteIndex < readLength) {
            this.mInputStream.reset();
            this.mInputStream.skip(splitByteIndex);
        }

        this.mParams = new HashMap<String, String>();
        this.mHeaders.clear();

        // Create a BufferedReader for parsing the header.
        BufferedReader headerReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, readLength)));

        // Decode the header into params and header java properties
        Map<String, String> extraInfo = new HashMap<String, String>();
        decodeHeader(headerReader, extraInfo, this.mParams, this.mHeaders);

        if (null != this.mRemoteIP) {
            this.mHeaders.put("remote-addr", this.mRemoteIP);
            this.mHeaders.put("http-client-ip", this.mRemoteIP);
        }

        this.mMethod = Method.lookup(extraInfo.get("method"));
        if (this.mMethod == null) {
            throw new Exception("BAD REQUEST: Syntax error. HTTP verb " + extraInfo.get("method") + " unhandled.");
        }

        this.mUri = extraInfo.get("uri");

        String connection = this.mHeaders.get("connection");
        mKeepAlive = "HTTP/1.1".equals(mProtocolVersion) &&
                (connection == null || !connection.matches("(?i).*close.*"));
    }

    //GET / HTTP/1.1\r\nHost: www.sina.com.cn\r\nConnection: close\r\n\r\n
    //'\r\n\r\n'
    private int findResponseHeaderEnd(final byte[] buf, int readLength) {
        int splitByteIndex = 0;
        while (splitByteIndex + 1 < readLength) {

            // RFC2616
            if (buf[splitByteIndex] == '\r'
                    && buf[splitByteIndex + 1] == '\n'
                    && splitByteIndex + 3 < readLength
                    && buf[splitByteIndex + 2] == '\r'
                    && buf[splitByteIndex + 3] == '\n') {
                return splitByteIndex + 4;
            }

            // tolerance
            if (buf[splitByteIndex] == '\n' && buf[splitByteIndex + 1] == '\n') {
                return splitByteIndex + 2;
            }
            splitByteIndex++;
        }
        return 0;
    }

    private void decodeHeader(BufferedReader headerReader,
                              Map<String, String> extraInfo,
                              Map<String, String> params,
                              Map<String, String> headers) throws Exception {
        try {
            // Read the request line
            String readLine = headerReader.readLine();
            if (readLine == null) {
                return;
            }

            StringTokenizer st = new StringTokenizer(readLine);
            if (!st.hasMoreTokens()) {
                throw new Exception("Bad request, syntax error, correct format: GET /example/file.html");
            }

            extraInfo.put("method", st.nextToken());

            if (!st.hasMoreTokens()) {
                throw new Exception("Bad request, syntax error, correct format: GET /example/file.html");
            }

            String uri = st.nextToken();

            // Decode parameters from the URI
            int questionMaskIndex = uri.indexOf('?');
            if (questionMaskIndex >= 0 && questionMaskIndex < uri.length()) {
                decodeParams(uri.substring(questionMaskIndex + 1), params);
                uri = LocalProxyUtils.decodeUri(uri.substring(0, questionMaskIndex));
            } else {
                uri = LocalProxyUtils.decodeUri(uri);
            }

            // If there's another token, its protocol version,
            // followed by HTTP headers.
            // NOTE: this now forces header names lower case since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens()) {
                mProtocolVersion = st.nextToken();
            } else {
                //default protocol version
                mProtocolVersion = "HTTP/1.1";
            }

            //parse headers:
            String line = headerReader.readLine();
            while (line != null && !line.trim().isEmpty()) {
                int index = line.indexOf(':');
                if (index >= 0 && index < line.length()) {
                    headers.put(line.substring(0, index).trim().
                            toLowerCase(Locale.US), line.substring(index + 1).trim());
                }
                line = headerReader.readLine();
            }

            extraInfo.put("uri", uri);
        } catch (IOException e) {
            throw new Exception( "Parsing Header Exception: " + e.getMessage(), e);
        }
    }

    private void decodeParams(String params, Map<String, String> paramsMap) {
        if (params == null) {
            this.mQueryParameter = "";
            return;
        }

        this.mQueryParameter = params;
        StringTokenizer st = new StringTokenizer(params, "&");
        while (st.hasMoreTokens()) {
            String item = st.nextToken();
            int index = item.indexOf('=');
            if (index >= 0 && index < item.length()) {
                paramsMap.put(LocalProxyUtils.decodeUri(item.substring(0, index)).trim(),
                        LocalProxyUtils.decodeUri(item.substring(index + 1)));
            } else {
                paramsMap.put(LocalProxyUtils.decodeUri(item).trim(), "");
            }
        }
    }

    public String getMimeType() {
        return "video/mpeg";
    }

    public String getProtocolVersion() {
        return mProtocolVersion;
    }

    public String getUri(){
        return String.valueOf(mUri);
    }

    public boolean keepAlive() {
        return mKeepAlive;
    }

    public Method requestMethod() {
        return mMethod;
    }
}

package com.media.cache.utils;

import android.net.Uri;
import android.text.TextUtils;

import com.media.cache.LocalProxyConfig;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HttpUtils {

    public static int MAX_REDIRECT = 5;
    public static final int RESPONSE_OK = 200;

    public static boolean matchHttpSchema(String url) {
        if (TextUtils.isEmpty(url))
            return false;
        Uri uri = Uri.parse(url);
        String schema = uri.getScheme();
        return "http".equals(schema) || "https".equals(schema);
    }

    public static String getMimeType(LocalProxyConfig config, String videoUrl, HashMap<String, String> headers) throws IOException {
        String mimeType = null;
        URL url = null;
        try {
            url = new URL(videoUrl);
        } catch (MalformedURLException e) {
            LogUtils.w("VideoUrl(" + videoUrl +") packages error, exception = " + e.getMessage());
            throw new MalformedURLException("URL parse error.");
        }
        HttpURLConnection connection = null;
        if (url != null) {
            try {
                connection = makeConnection(config, url, headers);
            } catch (IOException e) {
                LogUtils.w("Unable to connect videoUrl(" + videoUrl + "), exception = " + e.getMessage());
                closeConnection(connection);
                throw new IOException("getMimeType connect failed.");
            }
            int responseCode = 0;
            if (connection != null) {
                try {
                    responseCode = connection.getResponseCode();
                }catch (IOException e) {
                    LogUtils.w("Unable to Get reponseCode videoUrl(" + videoUrl + "), exception = " + e.getMessage());
                    closeConnection(connection);
                    throw new IOException("getMimeType get responseCode failed.");
                }
                if (responseCode == RESPONSE_OK) {
                    String contentType = connection.getContentType();
                    LogUtils.i("contentType = " + contentType);
                    return contentType;
                }

            }
        }
        return mimeType;
    }

    public static String getFinalUrl(LocalProxyConfig config, String videoUrl, HashMap<String, String> headers) throws IOException {
        URL url = null;
        try {
            url = new URL(videoUrl);
        } catch (MalformedURLException e) {
            LogUtils.w("VideoUrl(" + videoUrl +") packages error, exception = " + e.getMessage());
            throw new MalformedURLException("URL parse error.");
        }
        url = handleRedirectRequest(config, url, headers);
        return url.toString();
    }

    public static URL handleRedirectRequest(LocalProxyConfig config, URL url, HashMap<String, String> headers) throws IOException {
        int redirectCount = 0;
        while (redirectCount++ < MAX_REDIRECT) {
            HttpURLConnection connection = makeConnection(config, url, headers);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MULT_CHOICE
                    || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                    && (responseCode == 307 /* HTTP_TEMP_REDIRECT */
                    || responseCode == 308 /* HTTP_PERM_REDIRECT */)) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                url = handleRedirect(url, location);
                return handleRedirectRequest(config, url, headers);
            } else {
                return url;
            }
        }
        throw new NoRouteToHostException("Too many redirects: " + redirectCount);
    }

    private static HttpURLConnection makeConnection(LocalProxyConfig config, URL url, HashMap<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        connection = (HttpURLConnection)url.openConnection();
        connection.setConnectTimeout(config.getConnTimeOut());
        connection.setReadTimeout(config.getReadTimeOut());
        if (headers != null) {
            for (Map.Entry<String, String> item : headers.entrySet()) {
                connection.setRequestProperty(item.getKey(), item.getValue());
            }
        }
        connection.connect();
        return connection;
    }

    private static URL handleRedirect(URL originalUrl, String location) throws IOException {
        if (location == null) {
            throw new ProtocolException("Null location redirect");
        }
        URL url = new URL(originalUrl, location);
        String protocol = url.getProtocol();
        if (!"https".equals(protocol) && !"http".equals(protocol)) {
            throw new ProtocolException("Unsupported protocol redirect: " + protocol);
        }
        return url;
    }

    private static void closeConnection(HttpURLConnection connection) {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }
}


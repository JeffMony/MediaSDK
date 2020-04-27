package com.media.cache.proxy;

import android.text.TextUtils;

import com.android.baselib.utils.LogUtils;
import com.jeffmony.async.AsyncServer;
import com.jeffmony.async.AsyncServerSocket;
import com.jeffmony.async.http.server.AsyncHttpServer;
import com.jeffmony.async.http.server.AsyncHttpServerRequest;
import com.jeffmony.async.http.server.AsyncHttpServerResponse;
import com.media.cache.LocalProxyConfig;
import com.media.cache.utils.HttpUtils;
import com.media.cache.utils.LocalProxyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

public class AsyncProxyServer {
  private final AsyncHttpServer mHttpServer;
  private final LocalProxyConfig mConfig;
  private static final String PROXY_HOST = "127.0.0.1";
  private static final int REDIRECTED_COUNT = 3;

  public AsyncProxyServer(LocalProxyConfig config) {
    mConfig = config;
    mHttpServer = new AsyncHttpServer();

    mHttpServer.setErrorCallback(e -> LogUtils.w("AsyncProxyServer.ErrorCallback.exception=" + e));

    mHttpServer.get("/.*", (request, response) -> {
      try {
        sendData(request, response);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    AsyncServerSocket socket = mHttpServer.listen(0);
    mConfig.setConfig(PROXY_HOST, socket.getLocalPort());
  }

  private void sendData(AsyncHttpServerRequest request, AsyncHttpServerResponse response) throws Exception {
    String resultUrl = LocalProxyUtils.decodeUri(request.getPath());
    if (TextUtils.isEmpty(resultUrl)) {
      LogUtils.e("ProxyServer failed, sendData request url is null.");
      return;
    }
    resultUrl = resultUrl.substring(1);
    if (resultUrl.startsWith("http://") || resultUrl.startsWith("https://")) {
      if (resultUrl.contains(LocalProxyUtils.SPLIT_STR)) {
        String[] arr = resultUrl.split(LocalProxyUtils.SPLIT_STR);
        String videoUrl = arr[0];
        String fileName = arr[1].substring(1);
        File file = new File(mConfig.getCacheRoot(), fileName);
        if (file.exists()) {
          response.sendFile(file);
        } else {
          sendDataByNetwork(videoUrl, file, response);
        }
      }
    } else {
      File file = new File(mConfig.getCacheRoot(), resultUrl);
      if (file.exists()) {
        response.sendFile(file);
      } else {
        LogUtils.e("sendData failed, "+ file.getAbsolutePath() + " not found.");
      }
    }
  }

  public void sendDataByNetwork(String url, File file, AsyncHttpServerResponse response) {
    HttpURLConnection connection = null;
    InputStream inputStream;
    try {
      connection = openConnection(url);
      int responseCode = connection.getResponseCode();
      int contentLength = connection.getContentLength();
      if (responseCode == HttpUtils.RESPONSE_OK) {
        inputStream = connection.getInputStream();
        response.sendStream(inputStream, contentLength);
        saveFile(inputStream, file);
      }
    } catch (Exception e) {
      LogUtils.w("sendDataByNetwork failed, exception="+e.getMessage());
    } finally {
      if (connection != null)
        connection.disconnect();
    }
  }

  private HttpURLConnection openConnection(String videoUrl)
          throws Exception {
    HttpURLConnection connection;
    boolean redirected;
    int redirectedCount = 0;
    do {
      URL url = new URL(videoUrl);
      connection = (HttpURLConnection)url.openConnection();
      if (mConfig.shouldIgnoreAllCertErrors() && connection instanceof HttpsURLConnection) {
        HttpUtils.trustAllCert((HttpsURLConnection)(connection));
      }
      connection.setConnectTimeout(mConfig.getConnTimeOut());
      connection.setReadTimeout(mConfig.getReadTimeOut());
      int code = connection.getResponseCode();
      redirected = code == HTTP_MOVED_PERM || code == HTTP_MOVED_TEMP ||
              code == HTTP_SEE_OTHER;
      if (redirected) {
        redirectedCount++;
        connection.disconnect();
      }
      if (redirectedCount > REDIRECTED_COUNT) {
        throw new Exception("Too many redirects: " + redirectedCount);
      }
    } while (redirected);
    return connection;
  }

  private void saveFile(InputStream inputStream, File file) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      int len = 0;
      byte[] buf = new byte[LocalProxyUtils.DEFAULT_BUFFER_SIZE];
      while ((len = inputStream.read(buf)) != -1) {
        fos.write(buf, 0, len);
      }
    } catch (Exception e) {
      LogUtils.w(file.getAbsolutePath() + " saveFile failed, exception="+e);
      if (file.exists()) {
        file.delete();
      }
    } finally {
      LocalProxyUtils.close(inputStream);
      LocalProxyUtils.close(fos);
    }
  }

  private void shutdown() {
    mHttpServer.stop();
    AsyncServer.getDefault().stop();
  }
}

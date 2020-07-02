package com.media.cache.http;

import com.android.baselib.utils.LogUtils;
import com.media.cache.LocalProxyConfig;
import com.media.cache.utils.LocalProxyUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketProcessorTask implements Runnable {

    private final LocalProxyConfig mConfig;
    private final Socket mSocket;

    public SocketProcessorTask(Socket socket, LocalProxyConfig config) {
        this.mConfig = config;
        this.mSocket = socket;
    }

    @Override
    public void run() {
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = mSocket.getOutputStream();
            inputStream = mSocket.getInputStream();

            HttpRequest request = new HttpRequest(inputStream, mSocket.getInetAddress());
            while (!mSocket.isClosed()) {
                request.parseRequest();
                HttpResponse response = new HttpResponse(request, mConfig);
                response.send(outputStream);
            }
        } catch (Exception e) {
            LogUtils.w("socket request failed, exception=" + e);
        } finally {
            LocalProxyUtils.close(outputStream);
            LocalProxyUtils.close(inputStream);
            LocalProxyUtils.close(mSocket);
        }
    }
}

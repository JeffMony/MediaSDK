package com.media.cache.proxy;

import com.android.baselib.utils.LogUtils;
import com.media.cache.LocalProxyConfig;
import com.media.cache.http.SocketProcessorTask;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomProxyServer {

    private final ExecutorService mSocketPool = Executors.newFixedThreadPool(8);
    private final LocalProxyConfig mConfig;
    private static final String PROXY_HOST = "127.0.0.1";

    private Thread mRequestThread;
    private ServerSocket mServerSocket;
    private int mPort;

    public CustomProxyServer(LocalProxyConfig config) {
        mConfig = config;
        try {
            InetAddress address = InetAddress.getByName(PROXY_HOST);
            this.mServerSocket = new ServerSocket(0, 8, address);
            this.mPort = mServerSocket.getLocalPort();
            mConfig.setConfig(PROXY_HOST, mPort);
            CountDownLatch startSignal = new CountDownLatch(1);
            WaitSocketRequestsTask task = new WaitSocketRequestsTask(startSignal);
            mRequestThread = new Thread(task);
            mRequestThread.setName("VideoProxyCacheThread");
            mRequestThread.start();
            startSignal.await();
        } catch (Exception e) {
            shutdown();
            LogUtils.w("Cannot create serverSocket, exception=" + e);
        }

    }

    private class WaitSocketRequestsTask implements Runnable {

        private CountDownLatch mLatch;

        public WaitSocketRequestsTask(CountDownLatch latch) { mLatch = latch; }

        @Override
        public void run() {
            mLatch.countDown();
            initSocketProcessor();
        }
    }

    private void initSocketProcessor() {
        do {
            try {
                Socket socket = mServerSocket.accept();
                if (mConfig.getConnTimeOut() > 0)
                    socket.setSoTimeout(mConfig.getConnTimeOut());
                mSocketPool.submit(new SocketProcessorTask(socket, mConfig));
            } catch (Exception e) {
                LogUtils.w(
                        "WaitRequestsRun ServerSocket accept failed, exception=" + e);
            }
        } while (!mServerSocket.isClosed());
    }

    private void shutdown() {
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (Exception e) {
                LogUtils.w( "ServerSocket close failed, exception="+e);
            }finally {
                mSocketPool.shutdown();
                if (mRequestThread != null && mRequestThread.isAlive()) {
                    mRequestThread.interrupt();
                }
            }
        }
    }
}

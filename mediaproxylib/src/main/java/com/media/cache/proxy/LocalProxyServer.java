package com.media.cache.proxy;

import com.media.cache.LocalProxyConfig;
import com.media.cache.utils.LogUtils;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalProxyServer {

    private final ExecutorService mSocketPool = Executors.newFixedThreadPool(8);
    private final LocalProxyConfig mConfig;
    private static final String PROXY_HOST = "127.0.0.1";

    private ServerSocket mServerSocket;
    private int mPort;

    public LocalProxyServer(LocalProxyConfig config) {
        mConfig = config;
        try {
            this.mServerSocket = new ServerSocket();
            this.mPort = config.getPort();
            mConfig.setConfig(PROXY_HOST, mPort);
            mServerSocket.setReuseAddress(true);
            WaitSocketRequestsTask task = new WaitSocketRequestsTask();
            Thread thread = new Thread(task);
            thread.setName("VideoProxyCacheThread");
            thread.start();
            while (!task.mSocketBinded && task.mSocketBindException == null) {
                try {
                    Thread.sleep(10L);
                } catch (Exception e) {
                    LogUtils.w("VideoProxyCacheServer sleep failed, exception="+e);
                }
            }
            if (task.mSocketBindException != null)
                throw task.mSocketBindException;
        } catch (Exception e) {
            shutdown();
            LogUtils.w("Cannot create serverSocket, exception=" + e);
        }

    }

    private class WaitSocketRequestsTask implements Runnable {

        private boolean mSocketBinded = false;
        private Exception mSocketBindException;


        public WaitSocketRequestsTask() {
        }

        @Override
        public void run() {
            try {
                LogUtils.i( "WaitSocketRequestsTask run : " + mConfig.getHost() +":"+mConfig.getPort());
                mServerSocket.bind(mConfig.getHost() != null ? new InetSocketAddress(mConfig.getHost(),mConfig.getPort()) : new InetSocketAddress(mConfig.getPort()));
                mSocketBinded = true;
            } catch (Exception e) {
                LogUtils.w("WaitRequestsRun ServerSocket bind failed, exception="+e);
                mSocketBindException = e;
                return;
            }

            do{
                try {
                    Socket socket = mServerSocket.accept();
                    if (mConfig.getConnTimeOut() > 0)
                        socket.setSoTimeout(mConfig.getConnTimeOut());
                    mSocketPool.submit(new SocketProcessorTask(socket, mConfig));
                } catch (Exception e) {
                    LogUtils.w("WaitRequestsRun ServerSocket accept failed, exception="+e);
                }
            }while (!mServerSocket.isClosed());

        }
    }

    private void shutdown() {
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (Exception e) {
                LogUtils.w( "ServerSocket close failed, exception="+e);
            }finally {
                mSocketPool.shutdown();
            }
        }
    }
}

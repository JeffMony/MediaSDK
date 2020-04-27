package com.media.cache.http;

import android.text.TextUtils;

import com.android.baselib.utils.LogUtils;
import com.media.cache.LocalProxyConfig;
import com.media.cache.utils.HttpUtils;
import com.media.cache.utils.LocalProxyUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

public class HttpResponse {

    private static String CONTENT_TYPE = "Content-Type";
    private static String DATE = "Date";
    private static String CONNECTION = "Connection";
    private static String TRANSFER_ENCODING = "Transfer-Encoding";
    private static String GMT_PATTERN = "E, d MMM yyyy HH:mm:ss 'GMT'";

    private final HttpRequest mRequest;
    private final LocalProxyConfig mConfig;
    private final File mCacheRoot;
    private final String mMimeType;
    private final String mProtocolVersion;
    private IState mResponseState;
    private InputStream mInputStream;

    public HttpResponse(HttpRequest request, LocalProxyConfig config) throws Exception {
        this.mRequest = request;
        this.mConfig = config;
        this.mCacheRoot = config.getCacheRoot();
        this.mMimeType = mRequest.getMimeType();
        this.mProtocolVersion = mRequest.getProtocolVersion();
        String resultUrl = mRequest.getUri();
        if (resultUrl.startsWith("/http://") || resultUrl.startsWith("/https://")) {
            resultUrl = resultUrl.substring(1);
            if (resultUrl.contains(LocalProxyUtils.SPLIT_STR)) {
                String[] arr = resultUrl.split(LocalProxyUtils.SPLIT_STR);
                String url = arr[0];
                String fileName = arr[1];

                File file = new File(mCacheRoot, fileName);
                if (file.exists()) {
                    try {
                        mInputStream = new FileInputStream(file);
                        this.mResponseState = ResponseState.OK;
                    } catch (Exception e) {
                        throw new Exception("No files found to the request:" + file.getAbsolutePath(), e);
                    }
                } else {
                    try {
                        mInputStream = downloadFile(url, file);
                        this.mResponseState = ResponseState.OK;
                    } catch (Exception e) {
                        throw new Exception("HttpResponse download file failed:"+e);
                    }
                }
            }

        } else {
            File file = new File(mCacheRoot, mRequest.getUri());
            LogUtils.w("jeffmony HttpResponse file exist="+file.exists());
            if (file.exists()) {
                try {
                    mInputStream = new FileInputStream(file);
                    this.mResponseState = ResponseState.OK;
                } catch (Exception e) {
                    throw new Exception("No files found to the request:" + file.getAbsolutePath(), e);
                }
            } else {
                mResponseState = ResponseState.INTERNAL_ERROR;
                throw new Exception("No files found to the request:" + file.getAbsolutePath());
            }
        }
    }

    private static final int REDIRECTED_COUNT = 3;

    public InputStream downloadFile(String url, File file) throws Exception {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = openConnection(url);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpUtils.RESPONSE_OK) {
                inputStream = connection.getInputStream();
                saveFile(inputStream, file);
                return inputStream;
            }
        }catch (Exception e) {
            throw e;
        }finally {
            if (connection != null)
                connection.disconnect();
        }
        return null;
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
                throw new Exception("Too many redirects: " +
                        redirectedCount);
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

    public void send(OutputStream outputStream) throws Exception {
        SimpleDateFormat gmtFormat= new SimpleDateFormat(GMT_PATTERN, Locale.US);
        gmtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            if (mResponseState == null) {
                throw new Exception("sendResponse(): Status can't be null.");
            }
            PrintWriter pw = new PrintWriter(new BufferedWriter(new
                    OutputStreamWriter(outputStream, new ContentType(mMimeType).getEncoding())), false);
            if (TextUtils.isEmpty(mProtocolVersion)) {
                pw.append("HTTP/1.1 ");
            } else {
                pw.append(mProtocolVersion + " ");
            }
            pw.append(mResponseState.getDescription()).append(" \r\n");
            if (!TextUtils.isEmpty(mMimeType)) {
                appendHeader(pw, CONTENT_TYPE, mMimeType);
            }
            appendHeader(pw, DATE, gmtFormat.format(new Date()));
            appendHeader(pw, CONNECTION, ( mRequest.keepAlive() ? "keep-alive" : "close"));
            if (mRequest.requestMethod() != Method.HEAD ) {
                appendHeader(pw, TRANSFER_ENCODING, "chunked");
            }
            pw.append("\r\n");
            pw.flush();
            sendBodyWithCorrectTransferAndEncoding(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            throw new Exception("send response failed: ", e);
        } finally {
            LocalProxyUtils.close(this.mInputStream);
        }
    }

    protected void appendHeader(PrintWriter pw, String key, String value) {
//        LogUtils.i("HttpResponse--[printHeader] key="+key+" value="+value);
        pw.append(key).append(": ").append(value).append("\r\n");
    }

    private void sendBodyWithCorrectTransferAndEncoding(OutputStream outputStream) throws IOException {
        ChunkedOutputStream chunkedOutputStream = new ChunkedOutputStream(outputStream);
        sendBody(chunkedOutputStream, -1);
        chunkedOutputStream.finish();
    }

    private void sendBody(OutputStream outputStream, long pending) throws IOException {
        long buffer_size = LocalProxyUtils.DEFAULT_BUFFER_SIZE;
        byte[] buff = new byte[(int) buffer_size];
        boolean sendEverything = pending == -1;
        while (pending > 0 || sendEverything) {
            long bytesToRead = sendEverything ? buffer_size : Math.min(pending, buffer_size);
            if (this.mInputStream == null) {
                break;
            }
            int read = this.mInputStream.read(buff, 0, (int) bytesToRead);
            if (read <= 0) {
                break;
            }
            outputStream.write(buff, 0, read);
            if (!sendEverything) {
                pending -= read;
            }
        }
    }
}


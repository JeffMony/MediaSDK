package com.media.cache.download;

import com.android.baselib.utils.LogUtils;
import com.media.cache.LocalProxyConfig;
import com.media.cache.StorageManager;
import com.media.cache.VideoCacheException;
import com.media.cache.model.VideoCacheInfo;
import com.media.cache.listener.IDownloadTaskListener;
import com.media.cache.utils.DownloadExceptionUtils;
import com.media.cache.utils.HttpUtils;
import com.media.cache.utils.LocalProxyThreadUtils;
import com.media.cache.utils.LocalProxyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.Nullable;

public class BaseVideoDownloadTask extends VideoDownloadTask {

    private static final String VIDEO_SUFFIX = ".video";

    private final LinkedHashMap<Long, Long> mSegmentList;
    private LinkedHashMap<Long, VideoRange> mVideoRangeMap;
    private VideoRange mCurDownloadRange;
    private long mTotalLength = -1L;

    class VideoRange {

        long start; //segment start.
        long end;   //segment end.

        VideoRange(long start, long end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            VideoRange range = (VideoRange)obj;
            return (start == range.start) && (end == range.end);
        }

        public String toString() {
            return "VideoRange[start="+start+", end="+end+"]";
        }
    }

    public BaseVideoDownloadTask(LocalProxyConfig config,
                                 VideoCacheInfo info,
                                 HashMap<String, String> headers) {
        super(config, info, headers);
        this.mTotalLength = info.getTotalLength();
        this.mSegmentList = mInfo.getSegmentList();
        this.mVideoRangeMap = new LinkedHashMap<>();
        mCurDownloadRange = new VideoRange(Long.MIN_VALUE, Long.MAX_VALUE);
        initSegements();
    }

    private void initSegements() {
        Iterator iterator = mSegmentList.entrySet().iterator();
        LogUtils.i( "initSegments size="+mSegmentList.size());
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> item = (Map.Entry<Long, Long>)iterator.next();
            long start = item.getKey();
            long end = item.getValue();
            mVideoRangeMap.put(start, new VideoRange(start, end));
        }
        printVideoRange();
    }

    private synchronized VideoRange getVideoRequestRange(long curSeekPosition) {
        long rangeStart = 0;
        long rangeEnd = Long.MAX_VALUE;
        printVideoRange();
        Iterator iterator = mVideoRangeMap.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<Long, VideoRange> item = (Map.Entry<Long, VideoRange>)iterator.next();
            VideoRange range = item.getValue();
            if (range.start > curSeekPosition) {
                rangeEnd = range.start;
                break;
            }
            if (range.start <= curSeekPosition && range.end >= curSeekPosition) {
                rangeStart = range.end;
                continue;
            }
            if (curSeekPosition > range.end + BUFFER_SIZE) {
                rangeStart = curSeekPosition;
                continue;
            } else {
                rangeStart = range.end;
                continue;
            }
        }
        VideoRange range = new VideoRange(rangeStart, rangeEnd);
        return range;
    }

    @Override
    public void startDownload(IDownloadTaskListener listener) {
        mDownloadTaskListener = listener;
        if (listener != null) {
            listener.onTaskStart(mInfo.getUrl());
        }
        mIsPlaying = false;
        seekToDownload(0L, listener);
    }

    @Override
    public void resumeDownload() {
        LogUtils.i("BaseVideoDownloadTask resumeDownload current position="+mCurrentCachedSize);
        mShouldSuspendDownloadTask = false;
        seekToDownload(mCurrentCachedSize, mDownloadTaskListener);
    }

    @Override
    public void seekToDownload(float seekPercent) {
        seekToDownload(seekPercent, mDownloadTaskListener);
    }

    @Override
    public void seekToDownload(long curPosition, long totalDuration) {
        pauseDownload();
        long curSeekPosition = (long)(curPosition * 1.0f / totalDuration * mTotalLength);
        LogUtils.i("BaseVideoDownloadTask seekToDownload seekToDownload="+curSeekPosition);
        mShouldSuspendDownloadTask = false;
        seekToDownload(curSeekPosition, mDownloadTaskListener);
    }

    @Override
    public void seekToDownload(float seekPercent, IDownloadTaskListener callback) {
        pauseDownload();
        long curSeekPosition = (long)(seekPercent * 1.0f / 100 * mTotalLength);
        LogUtils.i("BaseVideoDownloadTask seekToDownload seekToDownload="+curSeekPosition);
        mShouldSuspendDownloadTask = false;
        seekToDownload(curSeekPosition, callback);
    }

    //Just for M3U8VideoDownloadTask.
    @Override
    public void seekToDownload(int curDownloadTs, IDownloadTaskListener callback) {

    }

    @Override
    public void seekToDownload(long curSeekPosition, IDownloadTaskListener listener) {
        if (mInfo.getIsCompleted()) {
            LogUtils.i("BaseVideoDownloadTask local file.");
            notifyVideoReady();
            notifyCacheProgress();
            return;
        }
        startTimerTask();
        mDownloadExecutor = new ThreadPoolExecutor(
                THREAD_COUNT, THREAD_COUNT, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.DiscardOldestPolicy());
        mDownloadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mCurDownloadRange = getVideoRequestRange(curSeekPosition);
                LogUtils.i("seekToDownload ### mCurDownloadRange="+mCurDownloadRange);
                if (mTotalLength == -1L) {
                    mTotalLength = getContentLength(mFinalUrl);
                    LogUtils.i("file length = " + mTotalLength);
                    if (mTotalLength <= 0) {
                        LogUtils.w("BaseVideoDownloadTask file length cannot be fetched.");
                        notifyFailed(new VideoCacheException(DownloadExceptionUtils.FILE_LENGTH_FETCHED_ERROR_STRING));
                        return;
                    }
                    mInfo.setTotalLength(mTotalLength);
                }
                File videoFile;
                try {
                    videoFile = new File(mSaveDir, mSaveName + VIDEO_SUFFIX);
                    if (!videoFile.exists()) {
                        videoFile.createNewFile();
                    }
                } catch (Exception e) {
                    LogUtils.w("BaseDownloadTask createNewFile failed, exception="+e.getMessage());
                    return;
                }

                if (videoFile != null && videoFile.exists() &&
                        videoFile.length() > BUFFER_SIZE) {
                    notifyVideoReady();
                }

                if (mCurDownloadRange.start == Long.MIN_VALUE) {
                    mCurDownloadRange.start = 0;
                }
                if (mCurDownloadRange.end == Long.MAX_VALUE) {
                    mCurDownloadRange.end = mTotalLength;
                }

                InputStream inputStream = null;
                RandomAccessFile randomAccessFile = null;
                try {
                    LogUtils.i("seekToDownload start request video range:" + mCurDownloadRange);
                    LogUtils.i("begin request");
                    long rangeEnd = mCurDownloadRange.end;
                    long rangeStart = mCurDownloadRange.start;
                    mCurrentCachedSize = rangeStart;

                    inputStream = getResponseBody(mFinalUrl, rangeStart, rangeEnd);
                    byte[] buf = new byte[BUFFER_SIZE];

                    LogUtils.i("begin response");

                    //Read http stream body.

                    randomAccessFile = new RandomAccessFile(videoFile.getAbsolutePath(), "rw");
                    randomAccessFile.seek(rangeStart);
                    int readLength = 0;
                    while ((readLength = inputStream.read(buf)) != -1) {
                        if (mCurrentCachedSize >= rangeEnd) {
                            mCurrentCachedSize = rangeEnd;
                        }
                        if (mCurrentCachedSize + readLength > rangeEnd) {
                            randomAccessFile.write(buf, 0, (int)(rangeEnd - mCurrentCachedSize));
                            mCurrentCachedSize = rangeEnd;
                        } else {
                            randomAccessFile.write(buf, 0, readLength);
                            mCurrentCachedSize += readLength;
                        }
                        notifyCacheProgress();
                        if (mCurrentCachedSize >= BUFFER_SIZE + rangeStart) {
                            notifyVideoReady();
                        }
                        if (mCurrentCachedSize >= rangeEnd) {
                            LogUtils.w("BaseVideoDownloadTask innerThread segment download finished.");
                            notifyNextVideoSegment(rangeEnd);
                        }
                    }
                } catch (IOException e) {
                    LogUtils.w( "BaseVideo Download file failed, exception: " + e);

                    StorageManager.getInstance().checkCacheFile(mSaveDir, mConfig.getCacheSize());

                    //InterruptedIOException is just interrupted by external operation.
                    if (e instanceof InterruptedIOException) {
                        return;
                    }

                    notifyFailed(e);
                    return;
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (randomAccessFile != null) {
                            randomAccessFile.close();
                        }
                    } catch (IOException e) {
                        LogUtils.w( "Close stream failed, exception: " + e.getMessage());
                    }
                }

            }
        });

        //mCurDownloadRange download finished. Please download next range.
        //1.pauseDownload;
        //2.seekToDownload next range;
    }

    private void notifyFailed(Throwable e) {
        if (mDownloadTaskListener != null){
            mDownloadTaskListener.onTaskFailed(e);
        }
    }

    @Override
    public void pauseDownload() {
        if (mDownloadExecutor != null && !mDownloadExecutor.isShutdown()) {
            mDownloadExecutor.shutdownNow();
            mShouldSuspendDownloadTask = true;
            notifyOnTaskPaused();
        }
        updateProxyCacheInfo();
        writeProxyCacheInfo();
        StorageManager.getInstance().checkCacheFile(mSaveDir, mConfig.getCacheSize());
    }

    @Override
    public void stopDownload() {
        if (mDownloadExecutor != null && !mDownloadExecutor.isShutdown()) {
            mDownloadExecutor.shutdownNow();
            mShouldSuspendDownloadTask = true;
            notifyOnTaskPaused();
        }
        updateProxyCacheInfo();
        writeProxyCacheInfo();
        StorageManager.getInstance().checkCacheFile(mSaveDir, mConfig.getCacheSize());
    }

    private synchronized void updateProxyCacheInfo() {
        LogUtils.i( "BaseVideoDownloadTask updateProxyCacheInfo");
        if (!isCompleted()) {
            if (mCurrentCachedSize > mTotalLength)
                mCurDownloadRange.end = mTotalLength;
            else
                mCurDownloadRange.end = mCurrentCachedSize;
            mergeVideoRange();
            mInfo.setCachedLength(mCurDownloadRange.end);
            mInfo.setIsCompleted(isCompleted());
        } else {
            mInfo.setIsCompleted(true);
        }
        if (mInfo.getIsCompleted()) {
            notifyCacheFinished();
        }
    }

    private void writeProxyCacheInfo() {
        if (mType == OPERATE_TYPE.WRITED) {
            return;
        }
        LocalProxyThreadUtils.submitRunnableTask(new Runnable() {
            @Override
            public void run() {
                mInfo.setPort(mConfig.getPort());
                LogUtils.i("writeProxyCacheInfo : " + mInfo);
                LocalProxyUtils.writeProxyCacheInfo(mInfo, mSaveDir);
            }
        });
        if (mType == OPERATE_TYPE.DEFAULT && mInfo.getIsCompleted()) {
            mType = OPERATE_TYPE.WRITED;
        }
    }

    private synchronized void mergeVideoRange() {
        //merge  mCurDownloadRange in  mVideoRangeMap.
        if (mVideoRangeMap.size() < 1) {
            LogUtils.i("mergeVideoRange mCurDownloadRange="+mCurDownloadRange);
            if (mCurDownloadRange.start != Long.MIN_VALUE &&
                    mCurDownloadRange.end != Long.MAX_VALUE &&
                    mCurDownloadRange.start < mCurDownloadRange.end) {
                mVideoRangeMap.put(mCurDownloadRange.start, mCurDownloadRange);
            } else {
                LogUtils.i( "mergeVideoRange Cannot merge video range.");
            }
        } else if (!mVideoRangeMap.containsValue(mCurDownloadRange)) {
            LogUtils.i("mergeVideoRange rangeLength>1, mCurDownloadRange="+mCurDownloadRange);

            if (mCurDownloadRange.start == Long.MIN_VALUE
                    || mCurDownloadRange.end == Long.MAX_VALUE
                    || mCurDownloadRange.start >= mCurDownloadRange.end
                    || mCurrentCachedSize <= mCurDownloadRange.start) {
                return;
            }

            //1.Convert mCurDownloadRange into FinalRange.
            VideoRange finalRange = new VideoRange(Long.MIN_VALUE, Long.MAX_VALUE);
            Iterator iterator = mVideoRangeMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, VideoRange> item = (Map.Entry<Long, VideoRange>) iterator.next();
                VideoRange range = item.getValue();
                LogUtils.i("mergeVideoRange  item range="+range);
                if (range.start > mCurDownloadRange.end) {
                    finalRange.end = mCurDownloadRange.end;
                    break;
                }
                if (range.start <= mCurDownloadRange.end && range.end >= mCurDownloadRange.end) {
                    finalRange.end = range.end;
                    break;
                }
                if (range.end >= mCurDownloadRange.start && range.start <= mCurDownloadRange.start) {
                    finalRange.start = range.start;
                    continue;
                }
                if (range.end < mCurDownloadRange.start) {
                    finalRange.start = mCurDownloadRange.start;
                    continue;
                }
            }

            //2.Generate FinalRange.
            if (finalRange.start == Long.MIN_VALUE) {
                finalRange.start = mCurDownloadRange.start;
            }
            if (finalRange.end == Long.MAX_VALUE) {
                finalRange.end = mCurDownloadRange.end;
            }
            LogUtils.i("finalRange = " + finalRange);
            //3.Put FinalRange into mVideoRangeMap container.
            mVideoRangeMap.put(finalRange.start, finalRange);
            //4.Remove redundancy range from mVideoRangeMap.
            iterator = mVideoRangeMap.entrySet().iterator();
            LinkedHashMap<Long, VideoRange> tempVideoRangeMap = new LinkedHashMap<>();
            while(iterator.hasNext()) {
                Map.Entry<Long, VideoRange> item = (Map.Entry<Long, VideoRange>) iterator.next();
                VideoRange range = item.getValue();
                if (!containRange(finalRange, range)) {
                    tempVideoRangeMap.put(range.start, range);
                }
            }
            mVideoRangeMap.clear();
            mVideoRangeMap.putAll(tempVideoRangeMap);
        }
        LinkedHashMap<Long, Long> tempSegmentList = new LinkedHashMap<>();
        Iterator iterator = mVideoRangeMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, VideoRange> item = (Map.Entry<Long, VideoRange>) iterator.next();
            VideoRange range = item.getValue();
            tempSegmentList.put(range.start, range.end);
        }
        mSegmentList.clear();
        mSegmentList.putAll(tempSegmentList);
        mInfo.setSegmentList(mSegmentList);
    }

    //5.Determine video cache is complete?
    private synchronized boolean isCompleted() {
        if (mVideoRangeMap.size() != 1) {
            return false;
        }
        //The key is 0L, not 0; remember it.
        VideoRange range = mVideoRangeMap.get(0L);
        if (range != null && range.end == mTotalLength) {
            return true;
        }
        return false;
    }

    private boolean containRange(VideoRange range1, VideoRange range2) {
        return range1.start < range2.start && range1.end >= range2.end;
    }

    private synchronized void printVideoRange() {
        Iterator iterator = mVideoRangeMap.entrySet().iterator();
        LogUtils.i("printVideoRange size="+mVideoRangeMap.size());
        while (iterator.hasNext()) {
            Map.Entry<Long, VideoRange> item = (Map.Entry<Long, VideoRange>) iterator.next();
            VideoRange range = item.getValue();
            LogUtils.i( "printVideoRange range="+range);
        }
    }

    private synchronized void notifyVideoReady() {
        if (mDownloadTaskListener != null && !mIsPlaying) {
            String proxyUrl = String.format(Locale.US, "http://%s:%d/%s/%s", mConfig.getHost(), mConfig.getPort(), mSaveName, mSaveName + VIDEO_SUFFIX);
            mDownloadTaskListener.onLocalProxyReady(proxyUrl);//Uri.fromFile(mM3u8Help.getFile()).toString());
            mIsPlaying = true;
        }
    }

    private void notifyCacheProgress() {
        if (mDownloadTaskListener != null) {
            if (mInfo.getIsCompleted()) {
                if (!LocalProxyUtils.isFloatEqual(100.0f, mPercent)) {
                    mDownloadTaskListener.onTaskProgress(100,
                            mTotalLength, null);
                }
                mPercent = 100.0f;
                notifyCacheFinished();
            } else {
                mInfo.setCachedLength(mCurrentCachedSize);
                float percent = mCurrentCachedSize * 1.0f * 100 / mTotalLength;
                if (!LocalProxyUtils.isFloatEqual(percent, mPercent)) {
                    mDownloadTaskListener.onTaskProgress(percent,
                            mCurrentCachedSize, null);
                    mPercent = percent;
                }
            }
        }
    }

    //1.current segment has been downloaded.
    //2.start to download next video's segment.
    private void notifyNextVideoSegment(long rangeStart) {
        pauseDownload();
        if (rangeStart < mTotalLength) {
            seekToDownload(rangeStart, mDownloadTaskListener);
        }
    }

    private void notifyCacheFinished() {
        if (mDownloadTaskListener != null) {
            writeProxyCacheInfo();
            mDownloadTaskListener.onTaskFinished(mTotalLength);
            StorageManager.getInstance().checkCacheFile(mSaveDir, mConfig.getCacheSize());
        }
    }

    private InputStream getResponseBody(String url, long start, long end) throws IOException {
        HttpURLConnection connection = openConnection(url);
        connection.setRequestProperty("Range",
                "bytes=" + start + "-" +
                        end);
        return connection.getInputStream();
    }

    private long getContentLength(String videoUrl) {
        long length = 0;
        HttpURLConnection connection = null;
        try {
            connection = openConnection(videoUrl);
            length = connection.getContentLength();
        } catch (Exception e) {
            LogUtils.w( "BaseDownloadTask failed, exception="+e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
                connection = null;
            }
        }
        return length;
    }

    private HttpURLConnection openConnection(String videoUrl)
            throws IOException {
        HttpURLConnection connection;
        URL url = new URL(videoUrl);
        connection = (HttpURLConnection)url.openConnection();
        if (mConfig.shouldIgnoreAllCertErrors() && connection instanceof HttpsURLConnection) {
            HttpUtils.trustAllCert((HttpsURLConnection)(connection));
        }
        connection.setConnectTimeout(mConfig.getReadTimeOut());
        connection.setReadTimeout(mConfig.getConnTimeOut());
        if (mHeaders != null) {
            for (Map.Entry<String, String> item : mHeaders.entrySet()) {
                connection.setRequestProperty(item.getKey(), item.getValue());
            }
        }
        return connection;
    }
}

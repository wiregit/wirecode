package com.limegroup.mozilla;

import java.io.File;

import com.limegroup.gnutella.Downloader.DownloadStatus;

public interface MozillaDownloadListener {

    long getDownloadId();

    long getAmountPending();

    long getAmountDownloaded();

    long getContentLength();

    File getSaveFile();

    boolean isCompleted();

    boolean isInactive();

    boolean isPaused();

    float getAverageBandwidth();

    float getMeasuredBandwidth();

    void measureBandwidth();

    DownloadStatus getDownloadStatus();

    void cancelDownload();

    void removeDownload();

    void pauseDownload();

    void resumeDownload();

}

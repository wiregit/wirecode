package com.limegroup.mozilla;

import java.io.File;

import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.Downloader.DownloadStatus;

/**
 * Interface to allow access into the state of the nsIDownloadListener.
 * 
 */
public interface MozillaDownloadListener extends BandwidthTracker {

    /**
     * Returns the download id this listener is tracking.
     */
    long getDownloadId();

    /**
     * Returns the pending amount of bytes to be downloaded.
     */
    long getAmountPending();

    /**
     * Returns the amount downloaded so far.
     */
    long getAmountDownloaded();

    /**
     * Returns the total length of the download.
     */
    long getContentLength();

    /**
     * Returns the target save file for the download.
     */
    File getSaveFile();

    /**
     * Indicator if the download is complete or not.
     */
    boolean isCompleted();

    /**
     * Indicator if the downloader is currently active.
     */
    boolean isInactive();

    /**
     * Indicator if the downloader is in a paused state.
     */
    boolean isPaused();

    @Override
    float getAverageBandwidth();

    @Override
    float getMeasuredBandwidth();

    @Override
    void measureBandwidth();

    /**
     * Returns the download status for this download.
     */
    DownloadStatus getDownloadStatus();

    /**
     * Cancels the current download.
     */
    void cancelDownload();

    /**
     * Removes the current download.
     */
    void removeDownload();

    /**
     * Pauses the current download.
     */
    void pauseDownload();

    /**
     * Resumes the current download.
     */
    void resumeDownload();

}

package org.limewire.core.impl.mozilla;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.limewire.util.Objects;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDOMDocument;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIDownloadProgressListener;
import org.mozilla.interfaces.nsIRequest;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIWebProgress;
import org.mozilla.xpcom.Mozilla;

import com.limegroup.bittorrent.SimpleBandwidthTracker;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.mozilla.MozillaDownloadListener;

/**
 * This class listens to a specific Mozilla download and tracks some statistics
 * for us.
 */
public class LimeMozillaDownloadProgressListener implements nsIDownloadProgressListener,
        MozillaDownloadListener {
    private final long downloadId;

    private final AtomicInteger state;

    private final AtomicLong totalProgress;

    private final SimpleBandwidthTracker down;

    private final File saveFile;

    private final AtomicLong contentLength;

    public LimeMozillaDownloadProgressListener(nsIDownload download, short state) {
        Objects.nonNull(download, "download");
        this.downloadId = download.getId();
        this.state = new AtomicInteger(state);
        this.totalProgress = new AtomicLong();
        this.down = new SimpleBandwidthTracker();
        this.saveFile = new File(download.getTarget().getPath());
        this.contentLength = new AtomicLong(download.getSize());
    }

    @Override
    public void onDownloadStateChange(short state, nsIDownload download) {
        if (downloadId == download.getId()) {
            this.state.set(state);
        }
    }

    @Override
    public void onProgressChange(nsIWebProgress webProgress, nsIRequest request,
            long curSelfProgress, long maxSelfProgress, long curTotalProgress,
            long maxTotalProgress, nsIDownload download) {

        if (this.downloadId == download.getId()) {
            // this is my download
            long diff = curTotalProgress - totalProgress.longValue();
            down.count(diff);
            totalProgress.set(curTotalProgress);
            contentLength.set(download.getSize());

            this.state.set(download.getState());

            if (maxTotalProgress == curTotalProgress) {
                System.out.println("done!");
            }
        }
    }

    @Override
    public void onSecurityChange(nsIWebProgress webProgress, nsIRequest request, long state,
            nsIDownload download) {
        // don't care about this event.
    }

    @Override
    public void onStateChange(nsIWebProgress webProgress, nsIRequest request, long stateFlags,
            long status, nsIDownload download) {
        if (this.downloadId == download.getId()) {
            // this is my download
            this.state.set(download.getState());
            if (this.state.get() == nsIDownloadManager.DOWNLOAD_FINISHED) {
                this.contentLength.set(download.getSize());
                this.totalProgress.set(this.contentLength.get());
            }

        }

    }

    @Override
    public void setDocument(nsIDOMDocument document) {
        // no mozilla window to use
    }

    @Override
    public nsIDOMDocument getDocument() {
        // no mozilla window to use
        return null;
    }

    @Override
    public nsISupports queryInterface(String uuid) {
        return Mozilla.queryInterface(this, uuid);
    }

    public long getDownloadId() {
        return downloadId;
    }

    public float getAverageBandwidth() {
        return down.getAverageBandwidth();
    }

    public float getMeasuredBandwidth() {
        try {
            down.measureBandwidth();
            // TODO i shouldn't have to do the above measure bandwidth call.
            return down.getMeasuredBandwidth();
        } catch (InsufficientDataException e) {
            return 0;
        }
    }

    public void measureBandwidth() {
        down.measureBandwidth();
    }

    public boolean isCompleted() {
        return state.get() == nsIDownloadManager.DOWNLOAD_FINISHED;
    }

    public boolean isPaused() {
        int myState = state.get();
        boolean paused = myState == nsIDownloadManager.DOWNLOAD_PAUSED
                || myState == nsIDownloadManager.DOWNLOAD_NOTSTARTED;
        return paused;
    }

    public DownloadStatus getDownloadStatus() {
        if (isCompleted()) {
            return DownloadStatus.COMPLETE;
        }

        switch (state.get()) {
        case nsIDownloadManager.DOWNLOAD_SCANNING:
            return DownloadStatus.RESUMING;
        case nsIDownloadManager.DOWNLOAD_DOWNLOADING:
            return DownloadStatus.DOWNLOADING;
        case nsIDownloadManager.DOWNLOAD_FINISHED:
            return DownloadStatus.COMPLETE;
        case nsIDownloadManager.DOWNLOAD_QUEUED:
            return DownloadStatus.QUEUED;
        case nsIDownloadManager.DOWNLOAD_PAUSED:
            return DownloadStatus.PAUSED;
        case nsIDownloadManager.DOWNLOAD_NOTSTARTED:
            return DownloadStatus.PAUSED;
        case nsIDownloadManager.DOWNLOAD_CANCELED:
            return DownloadStatus.ABORTED;
        case nsIDownloadManager.DOWNLOAD_BLOCKED_PARENTAL:
            return DownloadStatus.INVALID;
        case nsIDownloadManager.DOWNLOAD_BLOCKED_POLICY:
            return DownloadStatus.INVALID;
        case nsIDownloadManager.DOWNLOAD_DIRTY:
            return DownloadStatus.INVALID;
        case nsIDownloadManager.DOWNLOAD_FAILED:
            return DownloadStatus.INVALID;
        }

        throw new IllegalStateException("unknown mozilla state");
    }

    public File getSaveFile() {
        return saveFile;
    }

    public boolean isInactive() {
        boolean inactive = state.get() != nsIDownloadManager.DOWNLOAD_DOWNLOADING;
        return inactive;
    }

    public long getAmountDownloaded() {
        return totalProgress.get();
    }

    public long getAmountPending() {
        return getContentLength() - getAmountDownloaded();
    }

    public long getContentLength() {
        return contentLength.get();
    }

    @Override
    public void cancelDownload() {
        getDownloadManager().cancelDownload(downloadId);
    }

    @Override
    public void pauseDownload() {
        getDownloadManager().pauseDownload(downloadId);
    }

    @Override
    public void removeDownload() {
        getDownloadManager().removeDownload(downloadId);
    }

    @Override
    public void resumeDownload() {
        getDownloadManager().resumeDownload(downloadId);
    }

    private nsIDownloadManager getDownloadManager() {
        nsIDownloadManager downloadManager = XPCOMUtils.getServiceProxy(
                "@mozilla.org/download-manager;1", nsIDownloadManager.class);
        return downloadManager;
    }
}
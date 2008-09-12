package org.limewire.core.impl.mozilla;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.limewire.core.api.mozilla.LimeMozillaDownloadManagerListener;
import org.limewire.core.api.mozilla.LimeMozillaDownloadProgressListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.Objects;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDOMDocument;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsIDownloadProgressListener;
import org.mozilla.interfaces.nsIRequest;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIWebProgress;
import org.mozilla.xpcom.Mozilla;
import org.mozilla.xpcom.XPCOMException;

import com.limegroup.bittorrent.SimpleBandwidthTracker;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.mozilla.MozillaDownloadListener;

/**
 * This class listens to a specific Mozilla download and tracks some statistics
 * for us.
 */
public class LimeMozillaDownloadProgressListenerImpl implements nsIDownloadProgressListener,
        MozillaDownloadListener, LimeMozillaDownloadProgressListener {

    private static final Log LOG = LogFactory.getLog(LimeMozillaDownloadProgressListenerImpl.class);

    private final long downloadId;

    private final AtomicInteger state;

    private final AtomicLong totalProgress;

    private final SimpleBandwidthTracker down;

    private final File saveFile;

    private final AtomicLong contentLength;
    
    private final LimeMozillaDownloadManagerListener manager;

    public LimeMozillaDownloadProgressListenerImpl(LimeMozillaDownloadManagerListener manager, nsIDownload download, short state) {
        this.manager = Objects.nonNull(manager, "manager");
        Objects.nonNull(download, "download");
        this.downloadId = download.getId();
        this.state = new AtomicInteger(state);
        this.totalProgress = new AtomicLong();
        this.down = new SimpleBandwidthTracker();
        this.saveFile = new File(download.getTarget().getPath());
        this.contentLength = new AtomicLong(download.getSize());
    }

    @Override
    public synchronized void onDownloadStateChange(short state, nsIDownload download) {
        if (downloadId == download.getId()) {
            this.state.set(state);
        }
    }

    @Override
    public synchronized void onProgressChange(nsIWebProgress webProgress, nsIRequest request,
            long curSelfProgress, long maxSelfProgress, long curTotalProgress,
            long maxTotalProgress, nsIDownload download) {

        if (this.downloadId == download.getId()) {
            // this is my download
            int diff = (int) (curTotalProgress - totalProgress.longValue());
            down.count(diff);
            if (!isPaused()) {
                // this event might come in after pausing, so we don't want to
                // change the state in that event
                int state = download.getState();
                this.state.set(state);
            }
            totalProgress.set(curTotalProgress);
            contentLength.set(download.getSize());
        }
    }

    @Override
    public synchronized void onSecurityChange(nsIWebProgress webProgress, nsIRequest request,
            long state, nsIDownload download) {
        // don't care about this event.
    }

    @Override
    public synchronized void onStateChange(nsIWebProgress webProgress, nsIRequest request,
            long stateFlags, long status, nsIDownload download) {
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

    @Override
    public synchronized float getAverageBandwidth() {
        return down.getAverageBandwidth();
    }

    @Override
    public synchronized float getMeasuredBandwidth() {
        try {
            return down.getMeasuredBandwidth();
        } catch (InsufficientDataException e) {
            return 0;
        }
    }

    @Override
    public synchronized void measureBandwidth() {
        down.measureBandwidth();
    }

    @Override
    public synchronized boolean isCompleted() {
        return state.get() == nsIDownloadManager.DOWNLOAD_FINISHED;
    }

    @Override
    public synchronized boolean isPaused() {
        int myState = state.get();
        boolean paused = myState == nsIDownloadManager.DOWNLOAD_PAUSED;
        return paused;
    }

    @Override
    public synchronized DownloadStatus getDownloadStatus() {
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

    @Override
    public File getSaveFile() {
        return saveFile;
    }

    @Override
    public synchronized boolean isInactive() {
        boolean inactive = state.get() != nsIDownloadManager.DOWNLOAD_DOWNLOADING;
        return inactive;
    }

    @Override
    public synchronized long getAmountDownloaded() {
        return totalProgress.get();
    }

    @Override
    public synchronized long getAmountPending() {
        return getContentLength() - getAmountDownloaded();
    }

    @Override
    public synchronized long getContentLength() {
        return contentLength.get();
    }

    @Override
    public void cancelDownload() {
        MozillaExecutor.mozSyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (LimeMozillaDownloadProgressListenerImpl.this) {
                        getDownloadManager().cancelDownload(downloadId);
                    }
                } catch (XPCOMException e) {
                    LOG.debug(e.getMessage(), e);
                }
            }
        });

    }

    @Override
    public void pauseDownload() {
        MozillaExecutor.mozSyncExec(new Runnable() {
            @Override
            public void run() {
                synchronized (LimeMozillaDownloadProgressListenerImpl.this) {
                    try {
                        getDownloadManager().pauseDownload(downloadId);
                    } catch (XPCOMException e) {
                        LOG.debug(e.getMessage(), e);
                    }
                    state.set(nsIDownloadManager.DOWNLOAD_PAUSED);
                }
            }
        });
    }

    @Override
    public void removeDownload() {
        MozillaExecutor.mozSyncExec(new Runnable() {
            @Override
            public void run() {
                synchronized (LimeMozillaDownloadProgressListenerImpl.this) {
                    nsIDownloadManager downloadManager = getDownloadManager();
                    try {
                        downloadManager.removeDownload(downloadId);
                    } catch (XPCOMException e) {
                        LOG.debug(e.getMessage(), e);
                    }
                    try {
                        downloadManager.removeListener(LimeMozillaDownloadProgressListenerImpl.this);
                    } catch (XPCOMException e) {
                        LOG.debug(e.getMessage(), e);
                    }
                    manager.remove(LimeMozillaDownloadProgressListenerImpl.this);
                }
            }
        });
    }

    @Override
    public void resumeDownload() {
        MozillaExecutor.mozSyncExec(new Runnable() {
            @Override
            public void run() {
                synchronized (LimeMozillaDownloadProgressListenerImpl.this) {
                    try {
                        getDownloadManager().resumeDownload(downloadId);
                    } catch (XPCOMException e) {
                        LOG.debug(e.getMessage(), e);
                    }
                    state.set(nsIDownloadManager.DOWNLOAD_QUEUED);
                }
            }
        });
    }

    private nsIDownloadManager getDownloadManager() {
        nsIDownloadManager downloadManager = XPCOMUtils.getServiceProxy(
                "@mozilla.org/download-manager;1", nsIDownloadManager.class);
        return downloadManager;
    }
}
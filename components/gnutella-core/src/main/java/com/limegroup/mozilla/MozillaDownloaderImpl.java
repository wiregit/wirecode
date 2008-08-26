package com.limegroup.mozilla;

import java.io.File;

import org.limewire.io.InvalidDataException;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.browser.MozillaDownloadProgressListener;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractCoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;

public class MozillaDownloaderImpl extends AbstractCoreDownloader {

    public static final String NS_IDOWNLOADMANAGER_CID = "@mozilla.org/download-manager;1";

    private long downloadId;

    private final MozillaDownloadProgressListener listener;

    public MozillaDownloaderImpl(SaveLocationManager saveLocationManager,
            MozillaDownloadProgressListener listener) {
        super(saveLocationManager);
        this.listener = listener;
        this.downloadId = listener.getDownloadId();
    }

    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.MOZILLA;
    }

    @Override
    public GUID getQueryGUID() {
        return null;
    }

    @Override
    public void handleInactivity() {
        // nothing happens when we're inactive
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public boolean isQueuable() {
        return !isResumable();
    }

    private nsIDownloadManager getDownloadManager() {
        nsIDownloadManager downloadManager = XPCOMUtils.getServiceProxy(NS_IDOWNLOADMANAGER_CID,
                nsIDownloadManager.class);
        return downloadManager;
    }

    @Override
    public void discardCorruptDownload(boolean delete) {
        nsIDownloadManager downloadManager = getDownloadManager();
        downloadManager.cancelDownload(downloadId);
        downloadManager.removeDownload(downloadId);
    }

    @Override
    public long getAmountLost() {
        return 0;
    }

    @Override
    public int getAmountPending() {
        nsIDownload download = getDownload();
        return (int) (download.getSize() - getAmountDownloaded());

    }

    @Override
    public long getAmountRead() {
        return getAmountDownloaded();
    }

    private long getAmountDownloaded() {
        nsIDownload download = getDownload();
        long amount = download.getPercentComplete() * download.getAmountTransferred();
        return amount;
    }

    @Override
    public long getAmountVerified() {
        return getAmountDownloaded();
    }

    @Override
    public RemoteFileDesc getBrowseEnabledHost() {
        return null;
    }

    @Override
    public int getBusyHostCount() {
        return 0;
    }

    @Override
    public Endpoint getChatEnabledHost() {
        return null;
    }

    @Override
    public int getChunkSize() {
        return 0;
    }

    @Override
    public long getContentLength() {
        nsIDownload download = getDownload();
        return download.getSize();
    }

    @Override
    public String getCustomIconDescriptor() {
        return "";
    }

    @Override
    public File getDownloadFragment() {
        return getFile();
    }

    @Override
    public File getFile() {
        return getSaveFile();
    }

    @Override
    public int getNumHosts() {
        return 1;
    }

    @Override
    public int getNumberOfAlternateLocations() {
        return 0;
    }

    @Override
    public int getNumberOfInvalidAlternateLocations() {
        return 0;
    }

    @Override
    public int getPossibleHostCount() {
        return 1;
    }

    @Override
    public int getQueuePosition() {
        return 1;
    }

    @Override
    public int getQueuedHostCount() {
        return 0;
    }

    @Override
    public int getRemainingStateTime() {
        return 0;
    }

    @Override
    public File getSaveFile() {
        nsIDownload download = getDownload();
        File file = new File(download.getTarget().getPath());
        return file;
    }

    @Override
    public URN getSha1Urn() {
        return null;
    }

    @Override
    public DownloadStatus getState() {
        if (isCompleted()) {
            return DownloadStatus.COMPLETE;
        }

        nsIDownload download = getDownload();

        switch (download.getState()) {
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
    public int getTriedHostCount() {
        return 1;
    }

    @Override
    public String getVendor() {
        return "";
    }

    @Override
    public boolean hasBrowseEnabledHost() {
        return false;
    }

    @Override
    public boolean hasChatEnabledHost() {
        return false;
    }

    @Override
    public boolean isCompleted() {
        nsIDownload download = getDownload();
        return download.getState() == nsIDownloadManager.DOWNLOAD_FINISHED;
    }

    @Override
    public boolean isInactive() {
        nsIDownload download = getDownload();
        boolean inactive = download.getState() != nsIDownloadManager.DOWNLOAD_DOWNLOADING;
        return inactive;
    }

    @Override
    public boolean isLaunchable() {
        return false;
    }

    @Override
    public boolean isPausable() {
        return !isPaused();
    }

    @Override
    public boolean isPaused() {
        nsIDownload download = getDownload();
        return isPaused(download);
    }

    @Override
    public boolean isRelocatable() {
        return !isCompleted();
    }

    @Override
    public boolean isResumable() {
        return isPaused();
    }

    private boolean isPaused(nsIDownload download) {
        boolean paused = download.getState() == nsIDownloadManager.DOWNLOAD_PAUSED
                || download.getState() == nsIDownloadManager.DOWNLOAD_NOTSTARTED;
        return paused;
    }

    private nsIDownload getDownload() {
        nsIDownloadManager downloadManager = getDownloadManager();
        nsIDownload download = downloadManager.getDownload(downloadId);
        return download;
    }

    @Override
    public void pause() {
        if (!isPaused()) {
            getDownloadManager().pauseDownload(downloadId);
        }
    }

    @Override
    public boolean resume() {
        boolean resumed = true;
        if (isPaused()) {
            getDownloadManager().resumeDownload(downloadId);
            resumed = true;
        }
        return resumed;
    }

    @Override
    public void stop() {
        pause();
    }

    @Override
    protected File getDefaultSaveFile() {
        return null;
    }

    @Override
    public boolean conflicts(URN urn, long fileSize, File... files) {
        return false;
    }

    @Override
    public boolean conflictsWithIncompleteFile(File incomplete) {
        return false;
    }

    @Override
    public void finish() {
        getDownloadManager().cancelDownload(downloadId);
        getDownloadManager().removeDownload(downloadId);
    }

    @Override
    public void addListener(EventListener<DownloadStatusEvent> listener) {
    }

    @Override
    public boolean removeListener(EventListener<DownloadStatusEvent> listener) {
        return false;
    }

    @Override
    public void initialize() {
    }

    @Override
    public boolean shouldBeRemoved() {
        return false;
    }

    @Override
    public boolean shouldBeRestarted() {
        return false;
    }

    @Override
    public void startDownload() {
        // this is done automatically by mozilla.
    }

    @Override
    public float getAverageBandwidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float getMeasuredBandwidth() throws InsufficientDataException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void measureBandwidth() {
        // TODO Auto-generated method stub

    }

    @Override
    protected DownloadMemento createMemento() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        // TODO Auto-generated method stub

    }
}

package com.limegroup.mozilla;

import java.io.File;

import org.limewire.io.InvalidDataException;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.browser.download.LimeMozillaDownloadProgressListener;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsIDownloadManager;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.NoOpSaveLocationManager;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractCoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.MozillaDownloadMementoImpl;

/**
 * Downloader listening to events from the Mozilla download process.
 */
public class MozillaDownloaderImpl extends AbstractCoreDownloader {

    private static final String NS_IDOWNLOADMANAGER_CID = "@mozilla.org/download-manager;1";

    private long downloadId;

    private final LimeMozillaDownloadProgressListener listener;

    public MozillaDownloaderImpl(LimeMozillaDownloadProgressListener listener) {
        super(new NoOpSaveLocationManager());
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
        return (int) listener.getAmountPending();
    }

    @Override
    public long getAmountRead() {
        return getAmountDownloaded();
    }

    private long getAmountDownloaded() {
        return listener.getAmountDownloaded();
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
        return listener.getContentLength();
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
        return listener.getSaveFile();
    }

    @Override
    public URN getSha1Urn() {
        return null;
    }

    @Override
    public DownloadStatus getState() {
        return listener.getDownloadStatus();
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
        return listener.isCompleted();
    }

    @Override
    public boolean isInactive() {
        return listener.isInactive();
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
        return listener.isPaused();
    }

    @Override
    public boolean isRelocatable() {
        return !isCompleted();
    }

    @Override
    public boolean isResumable() {
        return isPaused();
    }

    @Override
    public void pause() {
        if (!isPaused() && !isInactive() && !isCompleted()) {
            getDownloadManager().pauseDownload(downloadId);
        }
    }

    @Override
    public boolean resume() {
        if (isPaused()) {
            getDownloadManager().resumeDownload(downloadId);
        }
        return true;
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
        try {
            getDownloadManager().cancelDownload(downloadId);
        } catch (Exception ignored) {
            // yum
        }
        try {
            getDownloadManager().removeDownload(downloadId);
        } catch (Exception ignored) {
            // yum
        }
    }

    @Override
    public void addListener(EventListener<DownloadStatusEvent> listener) {
        //TODO implement
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
        return listener.getAverageBandwidth();
    }

    @Override
    public float getMeasuredBandwidth() throws InsufficientDataException {
        return listener.getMeasuredBandwidth();
    }

    @Override
    public void measureBandwidth() {
        listener.measureBandwidth();
    }

    @Override
    protected DownloadMemento createMemento() {
        return new MozillaDownloadMementoImpl();
    }

    @Override
    public void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        // nothing we do not want to init from memento
        // TODO would need to stop error message in factory initing the
        // downloader
    }
}

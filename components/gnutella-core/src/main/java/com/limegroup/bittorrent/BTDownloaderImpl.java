package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.api.download.SaveLocationException.LocationCode;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.Address;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.libtorrent.LibTorrentState;
import org.limewire.libtorrent.LibTorrentStatus;
import org.limewire.libtorrent.Torrent;
import org.limewire.libtorrent.TorrentEvent;
import org.limewire.libtorrent.TorrentSHA1ConversionUtils;
import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractCoreDownloader;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.downloader.serial.BTDownloadMemento;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.LibTorrentBTDownloadMemento;
import com.limegroup.gnutella.downloader.serial.LibTorrentBTDownloadMementoImpl;

/**
 * This class enables the rest of LimeWire to treat a BitTorrent as a regular
 * download.
 */
public class BTDownloaderImpl extends AbstractCoreDownloader implements BTDownloader {

    private final DownloadManager downloadManager;

    private final Torrent torrent;

    private final BTUploaderFactory btUploaderFactory;

    private volatile AtomicBoolean complete = new AtomicBoolean(false);

    /**
     * Torrent info hash based URN used as a cache for getSha1Urn().
     */
    private URN urn = null;

    @Inject
    BTDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            BTUploaderFactory btUploaderFactory, Provider<Torrent> torrentProvider) {
        super(saveLocationManager);

        this.downloadManager = downloadManager;
        this.btUploaderFactory = btUploaderFactory;
        this.torrent = torrentProvider.get();

        torrent.addListener(new EventListener<TorrentEvent>() {
            public void handleEvent(TorrentEvent event) {
                if (TorrentEvent.COMPLETED == event) {
                    FileUtils.deleteRecursive(torrent.getCompleteFile());
                    File completeDir = getSaveFile().getParentFile();
                    torrent.moveTorrent(completeDir);
                    BTDownloaderImpl.this.downloadManager.remove(BTDownloaderImpl.this, true);
                    complete.set(true);
                } else if (TorrentEvent.STOPPED == event) {
                    BTDownloaderImpl.this.downloadManager.remove(BTDownloaderImpl.this, true);
                }
            };
        });
    }

    @Override
    public void init(File torrentFile) throws IOException {
        torrent.init(torrentFile, SharingSettings.getSaveDirectory());
        saveFile = torrent.getCompleteFile();
    }

    /**
     * Stops a torrent download. If the torrent is in seeding state, it does
     * nothing. (To stop a seeding torrent it must be stopped from the uploads
     * pane)
     */
    @Override
    public void stop() {
        if (!isInactive() && !torrent.isFinished()) {
            torrent.stop();
            downloadManager.remove(this, true);
        } else {
            downloadManager.remove(this, true);
        }
    }

    @Override
    public void pause() {
        torrent.pause();
    }

    @Override
    public boolean isPaused() {
        return torrent.isPaused();
    }

    @Override
    public boolean isPausable() {
        return !isPaused();
    }

    @Override
    public boolean isInactive() {
        return isResumable() || getState() == DownloadState.QUEUED;
    }

    @Override
    public boolean isLaunchable() {
        return torrent.isSingleFileTorrent();
        // TODO old logic would check last verified offest, but logic seems
        // wrong, wince the pieces download randomly, there is no guarentee that
        // the begginning of the file is ok to preview.
    }

    @Override
    public boolean isResumable() {
        return isPaused();
    }

    @Override
    public boolean resume() {
        torrent.resume();
        return true;
    }

    @Override
    public File getFile() {
        if (torrent.isFinished()) {
            return getSaveFile();
        } else {
            return getIncompleteFile();
        }
    }

    @Override
    public File getIncompleteFile() {
        return torrent.getIncompleteFile();
    }

    @Override
    public File getDownloadFragment() {
        if (!isLaunchable()) {
            return null;
        }

        if (isCompleted()) {
            return getSaveFile();
        }

        if (torrent.isMultiFileTorrent()) {
            return null;
        }

        File file = new File(getIncompleteFile().getParent(), IncompleteFileManager.PREVIEW_PREFIX
                + getIncompleteFile().getName());

        // TODO come up with correct size for preview, look at old code checking
        // last verified offsets etc. old code looks wrong though, since the
        // file downloads randomly, the last verified offset does not tell us
        // much.
        long size = Math.min(getIncompleteFile().length(), 2 * 1024 * 1024);
        if (FileUtils.copy(getIncompleteFile(), size, file) <= 0) {
            return null;
        }
        return file;

    }

    @Override
    public DownloadState getState() {

        if (torrent.isCancelled()) {
            return DownloadState.ABORTED;
        }

        if (torrent.isFinished()) {
            return DownloadState.COMPLETE;
        }

        LibTorrentStatus status = torrent.getStatus();
        if (status == null) {
            return DownloadState.QUEUED;
        }
        LibTorrentState state = LibTorrentState.forId(status.state);
        return convertState(state);
    }

    private DownloadState convertState(LibTorrentState state) {
        // TODO support error states

        switch (state) {
        case DOWNLOADING:
            if (isPaused()) {
                return DownloadState.PAUSED;
            } else {
                return DownloadState.DOWNLOADING;
            }
        case QUEUED_FOR_CHECKING:
            return DownloadState.RESUMING;
        case CHECKING_FILES:
            return DownloadState.RESUMING;
        case SEEDING:
            return DownloadState.COMPLETE;
        case FINISHED:
            return DownloadState.COMPLETE;
        case ALLOCATING:
            return DownloadState.CONNECTING;
        case DOWNLOADING_METADATA:
            return DownloadState.INITIALIZING;
        default:
            throw new IllegalStateException("Unknown libtorrent state: " + state);
        }
    }

    @Override
    public int getRemainingStateTime() {
        // Unused
        return 0;
    }

    @Override
    public long getContentLength() {
        return torrent.getTotalSize();
    }

    @Override
    public long getAmountRead() {
        return torrent.getTotalDownloaded();
    }

    @Override
    public String getVendor() {
        return BITTORRENT_DOWNLOAD;
    }

    @Override
    public void discardCorruptDownload(boolean delete) {
        // we never give up because of corruption (because this can never be
        // called)
    }

    @Override
    public RemoteFileDesc getBrowseEnabledHost() {
        return null;
    }

    @Override
    public boolean hasBrowseEnabledHost() {
        return false;
    }

    @Override
    public int getQueuePosition() {
        return 1;
    }

    @Override
    public int getNumberOfAlternateLocations() {
        return getPossibleHostCount();
    }

    @Override
    public int getNumberOfInvalidAlternateLocations() {
        return 0; // not applicable to torrents
    }

    @Override
    public int getPossibleHostCount() {
        return torrent.getNumPeers();
    }

    @Override
    public int getBusyHostCount() {
        return 0;
    }

    @Override
    public int getQueuedHostCount() {
        return 0;
    }

    @Override
    public GUID getQueryGUID() {
        // Unused for torrents
        return null;
    }

    @Override
    public boolean isCompleted() {
        return complete.get();
    }

    @Override
    public boolean shouldBeRemoved() {
        // TODO validate
        switch (getState()) {
        case DISK_PROBLEM:
        case COMPLETE:
            return true;
        }
        return false;
    }

    @Override
    public long getAmountVerified() {
        return torrent.getTotalDownloaded();
    }

    @Override
    public void measureBandwidth() {
        // Unused, we are using the bandwidth reported by libtorrent
    }

    @Override
    public float getMeasuredBandwidth() throws InsufficientDataException {
        return (torrent.getDownloadRate() / 1024);
    }

    @Override
    public float getAverageBandwidth() {
        // Unused by anything
        return (torrent.getDownloadRate() / 1024);
    }

    @Override
    public boolean isRelocatable() {
        return !isCompleted();
    }

    @Override
    public void setSaveFile(File saveDirectory, String fileName, boolean overwrite)
            throws SaveLocationException {
        super.setSaveFile(saveDirectory, fileName, overwrite);
        // // if this didn't throw target is ok.
        // torrentFileSystem.setCompleteFile(new File(saveDirectory, fileName));

        // TODO support this method in future when we allow picking a new
        // savepath for a torrent
    }

    @Override
    protected File getDefaultSaveFile() {
        return new File(SharingSettings.getSaveDirectory(), torrent.getName());
    }

    @Override
    public URN getSha1Urn() {
        if (urn == null) {
            synchronized (this) {
                if (urn == null) {
                    String sha1 = torrent.getSha1();
                    if (sha1 != null) {
                        try {
                            urn = URN.createSHA1UrnFromBytes(TorrentSHA1ConversionUtils
                                    .fromHexString(sha1));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return urn;
    }

    @Override
    public int getNumHosts() {
        return torrent.getNumPeers();
    }

    @Override
    public List<Address> getSourcesAsAddresses() {

        // TODO: use torrent.getPeers() ... currently a strange internal
        // libtorrent error retrieving peers.

        // torrent.getPeers();

        List<Address> list = new LinkedList<Address>();

        try {
            list.add(new ConnectableImpl("test.com", false));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return list;
    }

    @Override
    public void initialize() {
        // torrentManager.get().addEventListener(this);
        // incompleteFileManager.addTorrentEntry(urn);
        // TODO what do we do with the incomplete file manager
    }

    @Override
    public void startDownload() {
        btUploaderFactory.createBTUploader(torrent);
        torrent.start();
    }

    @Override
    public void handleInactivity() {
        // nothing happens when we're inactive
    }

    @Override
    public boolean shouldBeRestarted() {
        return getState() == DownloadState.QUEUED;
    }

    @Override
    public boolean isAlive() {
        return false; // doesn't apply to torrents
    }

    @Override
    public boolean isQueuable() {
        return !isPaused();
    }

    @Override
    public synchronized void finish() {
        // TODO cleanup any memory needed
    }

    @Override
    public String getCustomIconDescriptor() {
        return BITTORRENT_DOWNLOAD;
    }

    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.BTDOWNLOADER;
    }

    @Override
    protected DownloadMemento createMemento() {
        return new LibTorrentBTDownloadMementoImpl();
    }

    @Override
    protected void fillInMemento(DownloadMemento memento) {
        super.fillInMemento(memento);

        LibTorrentBTDownloadMemento btMemento = (LibTorrentBTDownloadMemento) memento;

        btMemento.setName(torrent.getName());
        btMemento.setSha1Urn(getSha1Urn());
        btMemento.setContentLength(getContentLength());
        btMemento.setTrackerURL(torrent.getTrackerURL());
        btMemento.setPaths(torrent.getPaths());
        btMemento.setFastResumePath(torrent.getFastResumePath());
    }

    public void initFromCurrentMemento(LibTorrentBTDownloadMemento memento)
            throws InvalidDataException {
        urn = memento.getSha1Urn();

        if (!urn.isSHA1()) {
            throw new InvalidDataException(
                    "Non SHA1 URN retrieved from LibTorrent torrent momento.");
        }

        torrent.init(memento.getName(), TorrentSHA1ConversionUtils.toHexString(urn.getBytes()),
                memento.getContentLength(), memento.getTrackerURL(), memento.getPaths(), memento
                        .getSaveFile(), memento.getFastResumePath());
    }

    public void initFromOldMemento(BTDownloadMemento memento) throws InvalidDataException {
        BTMetaInfoMemento btmetainfo = memento.getBtMetaInfoMemento();

        List<String> paths = new ArrayList<String>();

        for (TorrentFile torrentFile : btmetainfo.getFileSystem().getFiles()) {
            paths.add(torrentFile.getTorrentPath());
        }

        URI[] trackers = btmetainfo.getTrackers();
        URI tracker1 = trackers[0];

        String name = btmetainfo.getFileSystem().getName();

        long totalSize = btmetainfo.getFileSystem().getTotalSize();

        byte[] infoHash = btmetainfo.getInfoHash();

        String sha1 = TorrentSHA1ConversionUtils.toHexString(infoHash);

        torrent
                .init(name, sha1, totalSize, tracker1.toString(), paths, memento.getSaveFile(),
                        null);
    }

    @Override
    public synchronized void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        super.initFromMemento(memento);
        if (BTDownloadMemento.class.isInstance(memento)) {
            initFromOldMemento((BTDownloadMemento) memento);
        } else if (LibTorrentBTDownloadMemento.class.isInstance(memento)) {
            initFromCurrentMemento((LibTorrentBTDownloadMemento) memento);
        }

    }

    @Override
    public void addListener(EventListener<DownloadStateEvent> listener) {
        // TODO implement
    }

    @Override
    public boolean removeListener(EventListener<DownloadStateEvent> listener) {
        // TODO implement
        return false;
    }

    @Override
    public void deleteIncompleteFiles() {
        // TODO assert that complete or aborted?
        FileUtils.deleteRecursive(getIncompleteFile());
    }

    @Override
    public List<File> getCompleteFiles() {
        return torrent.getCompleteFiles();
    }

    @Override
    public List<File> getIncompleteFiles() {
        return torrent.getIncompleteFiles();
    }

    @Override
    public boolean conflicts(URN urn, long fileSize, File... file) {
        if (getSha1Urn().equals(urn)) {
            return true;
        }

        for (File f : file) {
            if (conflictsSaveFile(f)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean conflictsSaveFile(File complete) {
        return complete.equals(getSaveFile());
    }

    @Override
    public boolean conflictsWithIncompleteFile(File incomplete) {
        return incomplete.equals(getIncompleteFile());
    }

    /**
     * Ensures the eventual download location is not already taken
     * 
     * @throws SaveLocationException
     */
    @Override
    public void checkTargetLocation() throws SaveLocationException {

        if (torrent.getCompleteFile().exists()) {
            throw new SaveLocationException(LocationCode.FILE_ALREADY_EXISTS, torrent
                    .getCompleteFile());
        }

    }

    /**
     * Ensures the eventual download location is not already taken by the files
     * of any other download.
     * 
     * @throws SaveLocationException
     */
    @Override
    public void checkActiveAndWaiting() throws SaveLocationException {

        for (CoreDownloader current : downloadManager.getAllDownloaders()) {
            if (getSha1Urn().equals(current.getSha1Urn())) {
                throw new SaveLocationException(LocationCode.FILE_ALREADY_DOWNLOADING, torrent
                        .getCompleteFile());
            }

            if (current.conflictsSaveFile(torrent.getCompleteFile())) {
                throw new SaveLocationException(LocationCode.FILE_IS_ALREADY_DOWNLOADED_TO, torrent
                        .getCompleteFile());
            }

            if (current.conflictsSaveFile(torrent.getIncompleteFile())) {
                throw new SaveLocationException(LocationCode.FILE_ALREADY_DOWNLOADING, torrent
                        .getCompleteFile());
            }
        }
    }

    /**
     * No longer relevant in any Downloader.
     */
    @Override
    public int getChunkSize() {
        throw new UnsupportedOperationException("BTDownloaderImpl.getChunkSize() not implemented");
    }

    /**
     * No longer relevant in any Downloader.
     */
    @Override
    public long getAmountLost() {
        throw new UnsupportedOperationException("BTDownloaderImpl.getAmountLost() not implemented");
    }

    /**
     * No longer relevant in any Downloader.
     */
    @Override
    public int getAmountPending() {
        throw new UnsupportedOperationException(
                "BTDownloaderImpl.getAmountPending() not implemented");
    }

    /**
     * No longer relevant in any Downloader.
     */
    @Override
    public int getTriedHostCount() {
        throw new UnsupportedOperationException(
                "BTDownloaderImpl.getTriedHostCount() not implemented");
    }
}

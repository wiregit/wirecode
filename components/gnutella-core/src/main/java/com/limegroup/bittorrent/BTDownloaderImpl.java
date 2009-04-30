package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.api.download.SaveLocationException.LocationCode;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.libtorrent.LibTorrentBTDownloadMemento;
import org.limewire.libtorrent.LibTorrentState;
import org.limewire.libtorrent.LibTorrentStatus;
import org.limewire.libtorrent.Torrent;
import org.limewire.libtorrent.TorrentEvent;
import org.limewire.libtorrent.TorrentManager;
import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
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

/**
 * This class enables the rest of LimeWire to treat a BitTorrent as a regular
 * download.
 */
public class BTDownloaderImpl extends AbstractCoreDownloader implements BTDownloader {

    private final DownloadManager downloadManager;

    private final TorrentManager torrentManager;

    private final Torrent torrent;

    private final BTUploaderFactory btUploaderFactory;

    private final AtomicBoolean complete = new AtomicBoolean(false);

    private URN urn = null;

    @Inject
    BTDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            TorrentManager torrentManager, BTUploaderFactory btUploaderFactory) {
        super(saveLocationManager);
        this.downloadManager = downloadManager;
        this.torrentManager = torrentManager;
        this.btUploaderFactory = btUploaderFactory;
        this.torrent = new Torrent(torrentManager);
        torrent.addListener(new EventListener<TorrentEvent>() {
            public void handleEvent(TorrentEvent event) {
                if (torrent.isFinished() && !complete.getAndSet(true)) {
                    FileUtils.deleteRecursive(torrent.getCompleteFile());
                    File completeDir = getCompleteFile().getParentFile();
                    torrent.moveTorrent(completeDir);
                }
            };
        });
    }

    @Override
    public void init(File torrentFile) throws IOException {
        torrent.init(torrentFile, SharingSettings.getSaveDirectory());
    }

    /**
     * Stops a torrent download. If the torrent is in seeding state, it does
     * nothing. (To stop a seeding torrent it must be stopped from the uploads
     * pane)
     */
    @Override
    public void stop() {
        // TODO add back in seeding logic
        finish();
        downloadManager.remove(this, true);
    }

    @Override
    public void pause() {
        torrentManager.pauseTorrent(torrent.getSha1());
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
        torrentManager.resumeTorrent(torrent.getSha1());
        return true;
    }

    @Override
    public File getFile() {
        if (torrent.isFinished()) {
            return getCompleteFile();
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
            return getCompleteFile();
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
        case downloading:
            if (isPaused()) {
                return DownloadState.PAUSED;
            } else {
                return DownloadState.DOWNLOADING;
            }
        case queued_for_checking:
            return DownloadState.RESUMING;
        case checking_files:
            return DownloadState.RESUMING;
        case seeding:
            return DownloadState.COMPLETE;
        case finished:
            return DownloadState.COMPLETE;
        case allocating:
            return DownloadState.CONNECTING;
        case downloading_metadata:
            return DownloadState.CONNECTING;
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
        // we never give up because of corruption
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
        return torrent.isFinished();
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

    public long getAmountVerified() {
        return torrent.getTotalDownloaded();
    }

    @Override
    public int getChunkSize() {
        throw new UnsupportedOperationException("BTDownloaderImpl.getChunkSize() not implemented");
    }

    @Override
    public long getAmountLost() {
        // Unused by anything
        return 0;
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
    public File getSaveFile() {
        return getCompleteFile();
    }

    @Override
    protected File getDefaultSaveFile() {
        return null;
    }

    @Override
    public URN getSha1Urn() {
        if (urn == null) {
            synchronized (this) {
                if (urn == null) {
                    try {
                        urn = URN.createSHA1UrnFromBytes(torrent.getInfoHash());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return urn;
    }

    @Override
    public int getAmountPending() {
        // Unused
        return 0;
    }

    @Override
    public int getNumHosts() {
        return torrent.getNumPeers();
    }

    @Override
    public List<Address> getSourcesAsAddresses() {
        
        return Collections.emptyList();
        
        // TODO
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
    public boolean conflicts(URN urn, long fileSize, File... file) {
        // if (this.urn.equals(urn))
        // return true;
        // for (File f : file) {
        // if (conflictsSaveFile(f))
        // return true;
        // }

        // TODO TODO TODO
        return false;
    }

    @Override
    public boolean conflictsSaveFile(File candidate) {
        // return torrentFileSystem.conflicts(candidate);
        // TODO
        return false;
    }

    @Override
    public boolean conflictsWithIncompleteFile(File incomplete) {
        // return torrentFileSystem.conflictsIncomplete(incomplete);
        return false;
        // TODO
    }

    @Override
    public synchronized void finish() {
        torrent.stop();
    }

    @Override
    public int getTriedHostCount() {
        return 0;
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
    public boolean isMementoSupported() {
        return true;
    }

    @Override
    protected DownloadMemento createMemento() {
        if (torrent.isFinished()) {
            throw new IllegalStateException("creating memento for finished torrent!");
        }

        // TODO create a new memento type
        return null;
    }

    @Override
    protected void fillInMemento(DownloadMemento memento) {
        super.fillInMemento(memento);
        // TODO fill in additional memento details
    }

    public void initFromCurrentMemento(LibTorrentBTDownloadMemento memento) {

    }
    
    public void initFromOldMemento(BTDownloadMemento memento) {
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

        String sha1 = toHexString(infoHash);
        torrent.init(name, sha1, totalSize, tracker1.toString(),  paths, SharingSettings.getSaveDirectory(), null);
    }

    @Override
    public synchronized void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        super.initFromMemento(memento);
        if (BTDownloadMemento.class.isInstance(memento)) {
            initFromOldMemento((BTDownloadMemento) memento);
        } 
        else if (LibTorrentBTDownloadMemento.class.isInstance(memento)) {
            initFromCurrentMemento((LibTorrentBTDownloadMemento) memento);
        }

    }

    private String toHexString(byte[] block) {
        StringBuffer hexString = new StringBuffer(block.length * 2);
        for ( byte b : block ) {
            hexString.append(Integer.toHexString(b));
        }
        return hexString.toString();
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
    public File getCompleteFile() {
        return torrent.getCompleteFile();
    }

    @Override
    public List<File> getCompleteFiles() {
        return torrent.getCompleteFiles();
    }

    @Override
    public List<File> getIncompleteFiles() {
        return torrent.getIncompleteFiles();
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
            if (urn.equals(current.getSha1Urn())) {
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

}

package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.io.InvalidDataException;
import org.limewire.libtorrent.Torrent;
import org.limewire.libtorrent.TorrentManager;
import org.limewire.listener.EventListener;

import com.google.inject.Inject;
import com.limegroup.bittorrent.BTData.BTFileData;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractCoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.BTDownloadMementoImpl;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;

/**
 * This class enables the rest of LimeWire to treat a BitTorrent as a regular
 * download.
 */
public class BTDownloaderImpl extends AbstractCoreDownloader implements BTDownloader {

    private final DownloadManager downloadManager;

    private final TorrentManager libTorrentManager;

    private final Torrent torrent;

    @Inject
    BTDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            TorrentManager libTorrentManager) {
        super(saveLocationManager);
        this.downloadManager = downloadManager;
        this.libTorrentManager = libTorrentManager;
        this.torrent = new Torrent(libTorrentManager);
    }

    @Override
    public void init(File torrentFile) throws IOException {
        torrent.init(torrentFile);
    }

    /**
     * Stops a torrent download. If the torrent is in seeding state, it does
     * nothing. (To stop a seeding torrent it must be stopped from the uploads
     * pane)
     */
    @Override
    public void stop() {
        // TODO, put back in logic
        finish();
    }

    @Override
    public void pause() {
        libTorrentManager.pauseTorrent(torrent.getSha1());
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
        return isResumable() || torrent.getState() == DownloadState.QUEUED;
    }

    @Override
    public boolean isLaunchable() {
        // TODO add back in
        return false;
    }

    @Override
    public boolean isResumable() {
        return isPaused();
    }

    @Override
    public boolean resume() {
        libTorrentManager.resumeTorrent(torrent.getSha1());
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

        // TODO add in preview capability for single file torrents.
        // File file = new File(getIncompleteFile().getParent(),
        // IncompleteFileManager.PREVIEW_PREFIX
        // + torrentFileSystem.getIncompleteFile().getName());
        // // Copy first block, returning if nothing was copied.
        // if (FileUtils.copy(torrentFileSystem.getIncompleteFile(), size, file)
        // <= 0)
        // return null;
        // return file;

        return null;
    }

    @Override
    public DownloadState getState() {
        return torrent.getState();
    }

    @Override
    public int getRemainingStateTime() {
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
        return null;
    }

    @Override
    public boolean isCompleted() {
        return torrent.isFinished();
    }

    @Override
    public boolean shouldBeRemoved() {
        // switch (torrent.getState()) {
        // case DISK_PROBLEM:
        // case SEEDING:
        // return true;
        // }
        // return false;
        return false;
        // TODO
    }

    public long getAmountVerified() {
        return torrent.getTotalDownloaded();
    }

    @Override
    public int getChunkSize() {
        return torrent.getPieceLength();
    }

    @Override
    public long getAmountLost() {
        // Unused
        return 0;
    }

    @Override
    public void measureBandwidth() {
        // Unused
    }

    @Override
    public float getMeasuredBandwidth() throws InsufficientDataException {
        return (torrent.getDownloadRate() / 1024);
    }

    @Override
    public float getAverageBandwidth() {
        // Unused
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

        // TODO
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
        String sha1String = torrent.getSha1();
        try {
            return URN.createSHA1Urn(sha1String);
        } catch (IOException e) {
            // TODO handle
            return null;
        }
    }

    @Override
    public int getAmountPending() {
        // TODO ???
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
        // TODO
    }

    @Override
    public void startDownload() {
        // btUploaderFactory.createBTUploader((ManagedTorrent) torrent,
        // btMetaInfo);
        // torrent.start();

        torrent.start();

        // TODO moving to complete folder when complete and starting to seed
        // again
    }

    @Override
    public void handleInactivity() {
        // nothing happens when we're inactive
    }

    @Override
    public boolean shouldBeRestarted() {
        // return getState() == DownloadState.QUEUED &&
        // torrentManager.get().allowNewTorrent();
        return getState() == DownloadState.QUEUED;
        // TODO
    }

    @Override
    public boolean isAlive() {
        return false; // doesn't apply to torrents
    }

    @Override
    public boolean isQueuable() {
        return !isResumable();
        // TODO ??
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
        libTorrentManager.removeTorrent(torrent.getSha1());
        // TODO cleanup things
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
    protected DownloadMemento createMemento() {
        if (torrent.isFinished()) {
            throw new IllegalStateException("creating memento for finished torrent!");
        }

        // TODO overwrite this
        return new BTDownloadMementoImpl();
    }

    @Override
    protected void fillInMemento(DownloadMemento memento) {
        // super.fillInMemento(memento);
        // BTDownloadMemento bmem = (BTDownloadMemento) memento;
        // bmem.setBtMetaInfoMemento(btMetaInfo.toMemento());

        // TODO need memento
    }

    @Override
    public synchronized void initFromMemento(DownloadMemento memento) throws InvalidDataException {
        // super.initFromMemento(memento);
        // BTDownloadMemento bmem = (BTDownloadMemento) memento;
        // initBtMetaInfo(btMetaInfoFactory.createBTMetaInfoFromMemento(bmem.
        // getBtMetaInfoMemento()));

        // TODO
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
        // torrentFileSystem.deleteIncompleteFiles();

        // TODO implement
    }

    @Override
    public File getCompleteFile() {
        return torrent.getCompleteFile();
    }

    @Override
    public boolean isMementoSupported() {
        return false;
    }

    @Override
    public List<File> getCompleteFiles() {
        return torrent.getCompleteFiles();
    }

    @Override
    public List<File> getIncompleteFiles() {
        return torrent.getIncompleteFiles();
    }

}

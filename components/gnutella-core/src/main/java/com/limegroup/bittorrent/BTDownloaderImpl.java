package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.io.InvalidDataException;
import org.limewire.libtorrent.LibTorrentEvent;
import org.limewire.libtorrent.LibTorrentInfo;
import org.limewire.libtorrent.LibTorrentManager;
import org.limewire.libtorrent.LibTorrentState;
import org.limewire.libtorrent.LibTorrentStatus;
import org.limewire.listener.EventListener;

import com.google.inject.Inject;
import com.limegroup.bittorrent.BTData.BTFileData;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadState;
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

    private final LibTorrentManager libTorrentManager;

    private volatile File torrent = null;

    private volatile File incompleteFile = null;

    private final TorrentStatus torrentStatus;

    private class TorrentStatus {
        private LibTorrentInfo info = null;

        private LibTorrentStatus status = null;

        public synchronized String getSha1() {
            return info.sha1;
        }

        public synchronized boolean isPaused() {
            return status == null ? false : status.paused;
        }

        public synchronized DownloadState getState() {
            if (status == null) {
                return DownloadState.QUEUED;
            }
            LibTorrentState state = LibTorrentState.forId(status.state);
            return convertState(state);
        }

        public synchronized boolean isFinished() {
            return status == null ? false : status.finished;
        }

        public synchronized long getTotalSize() {
            return info.content_length.longValue();
        }

        public synchronized long getTotalDownloaded() {
            return status == null ? 0 : status.total_done.longValue();
        }

        public synchronized int getNumPeers() {
            return status == null ? 0 : status.num_peers;
        }

        public synchronized int getPieceLength() {
            return info.piece_length;
        }

        public synchronized void setInfo(LibTorrentInfo info) {
            this.info = info;
        }

        public synchronized void setStatus(LibTorrentStatus status) {
            this.status = status;
        }

        private DownloadState convertState(LibTorrentState state) {
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
    }

    private List<String> paths = new ArrayList<String>();

    @Inject
    BTDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            LibTorrentManager libTorrentManager) {
        super(saveLocationManager);
        this.downloadManager = downloadManager;
        this.libTorrentManager = libTorrentManager;
        this.torrentStatus = new TorrentStatus();

    }

    @Override
    public void init(File torrent) throws IOException {
        this.torrent = torrent;
        FileInputStream fis = null;
        FileChannel fileChannel = null;
        try {
            fis = new FileInputStream(torrent);
            fileChannel = fis.getChannel();
            Map metaInfo = (Map) Token.parse(fileChannel);
            BTData btData = new BTDataImpl(metaInfo);
            String name = btData.getName();

            // TODO pull this from somewhere
            File torrentDownloadFolder = new File("/home/pvertenten/Desktop");
            incompleteFile = new File(torrentDownloadFolder, name);

            if (btData.getFiles() != null) {
                for (BTFileData fileData : btData.getFiles()) {
                    paths.add(fileData.getPath());
                }
            }

        } finally {
            IOUtils.close(fileChannel);
            IOUtils.close(fis);
        }

    }

    /**
     * Stops a torrent download. If the torrent is in seeding state, it does
     * nothing. (To stop a seeding torrent it must be stopped from the uploads
     * pane)
     */
    @Override
    public void stop() {
        // TODO, put back in logic
        libTorrentManager.removeTorrent(torrentStatus.getSha1());
    }

    @Override
    public void pause() {
        libTorrentManager.pauseTorrent(torrentStatus.getSha1());
    }

    @Override
    public boolean isPaused() {
        return torrentStatus.isPaused();
    }

    @Override
    public boolean isPausable() {
        return !isPaused();
    }

    @Override
    public boolean isInactive() {
        return isResumable() || torrentStatus.getState() == DownloadState.QUEUED;
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
        libTorrentManager.resumeTorrent(torrentStatus.getSha1());
        return true;
    }

    @Override
    public File getFile() {
        if (torrentStatus.isFinished()) {
            return getCompleteFile();
        } else {
            return getIncompleteFile();
        }
    }

    @Override
    public File getIncompleteFile() {
        return incompleteFile;
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
        return torrentStatus.getState();
    }

    @Override
    public int getRemainingStateTime() {
        return 0;
    }

    @Override
    public long getContentLength() {
        return torrentStatus.getTotalSize();
    }

    @Override
    public long getAmountRead() {
        return torrentStatus.getTotalDownloaded();
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
        return torrentStatus.getNumPeers();
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
        return torrentStatus.isFinished();
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
        return torrentStatus.getTotalDownloaded();
    }

    @Override
    public int getChunkSize() {
        return torrentStatus.getPieceLength();
    }

    @Override
    public long getAmountLost() {
        // TODO ???
        return 0;
    }

    @Override
    public void measureBandwidth() {
        // torrent.measureBandwidth();
        // averagedBandwidth.add(torrent.getMeasuredBandwidth(true));
        // TODO
    }

    @Override
    public float getMeasuredBandwidth() throws InsufficientDataException {
        // if (averagedBandwidth.size() < 3)
        // throw new InsufficientDataException();
        // return averagedBandwidth.average().floatValue();
        return 0;
        // TODO
    }

    @Override
    public float getAverageBandwidth() {
        // long now = stopTime > 0 ? stopTime : System.currentTimeMillis();
        // long runTime = now - startTime;
        // return runTime > 0 ? getTotalAmountDownloaded() / runTime : 0;

        return 0;
        // TODO
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
        // TODO
        // return urn;
        return null;
    }

    @Override
    public int getAmountPending() {
        // TODO ???
        return 0;
    }

    @Override
    public int getNumHosts() {
        return torrentStatus.getNumPeers();
    }

    @Override
    public List<Address> getSourcesAsAddresses() {
        // return torrent.getSourceAddresses();
        return null;
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

        LibTorrentInfo info = libTorrentManager.addTorrent(torrent);
        torrentStatus.setInfo(info);

        System.out.println(info);

        System.out.println("sha1_java: " + info.sha1);

        LibTorrentStatus status = libTorrentManager.getStatus(info.sha1);
        torrentStatus.setStatus(status);

        libTorrentManager.addListener(info.sha1, new EventListener<LibTorrentEvent>() {
            public void handleEvent(LibTorrentEvent event) {
                // TODO make threadsafe

                // LibTorrentAlert alert = event.getAlert();
                LibTorrentStatus status = event.getTorrentStatus();
                torrentStatus.setStatus(status);
            }

        });

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
        // // when finish is called it is expected that the downloader has
        // already
        // // been removed from the download manager.
        // assert downloadManager.contains(this) == false;
        // finished = true;
        // torrentManager.get().removeEventListener(this);
        // torrent = new FinishedTorrentDownload(torrent);
        // btMetaInfo = null;
        // TODO TODO
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
        if (torrentStatus.isFinished()) {
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
        // TODO real file.
        return getIncompleteFile();
    }

    @Override
    public List<File> getCompleteFiles() {
        List<File> files = new ArrayList<File>();
        File completeFile = getCompleteFile();
        if (paths.size() > 0) {
            for (String path : paths) {
                // TODO assuming unix path??
                File file = new File(completeFile, path);
                files.add(file);
            }
        } else {
            files.add(completeFile);
        }
        return files;
    }

    @Override
    public List<File> getIncompleteFiles() {
        List<File> files = new ArrayList<File>();
        File incompleteFile = getIncompleteFile();
        if (paths.size() > 0) {
            for (String path : paths) {
                // TODO assuming unix path??
                File file = new File(incompleteFile, path);
                files.add(file);
            }
        } else {
            files.add(incompleteFile);
        }
        return files;
    }

    @Override
    public boolean isMementoSupported() {
        return false;
    }

}

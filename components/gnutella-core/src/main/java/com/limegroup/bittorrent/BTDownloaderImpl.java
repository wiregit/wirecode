package com.limegroup.bittorrent;

import java.io.File;
import java.util.List;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.libtorrent.LibTorrentAlert;
import org.limewire.libtorrent.LibTorrentEvent;
import org.limewire.libtorrent.LibTorrentManager;
import org.limewire.libtorrent.LibTorrentState;
import org.limewire.libtorrent.LibTorrentStatus;
import org.limewire.listener.EventListener;

import com.google.inject.Inject;
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

    private final LibTorrentManager libTorrentManager;

    private volatile File torrent = null;

    private volatile String id = null;

    private volatile boolean paused = false;

    private volatile long contentLength;

    private volatile int numPeers;

    private volatile int nonInterestingPeers;

    private volatile int chokingPeers;

    private volatile boolean finished;

    private volatile long amountVerified;

    private volatile int pieceLength;

    private volatile long amountLost;

    private volatile int amountPending;

    private volatile int numConnections;

    private volatile int triedHostCount;

    // TODO thread safe
    private volatile DownloadState state = DownloadState.QUEUED;

    private long amountRead;

    @Inject
    BTDownloaderImpl(SaveLocationManager saveLocationManager, DownloadManager downloadManager,
            LibTorrentManager libTorrentManager) {
        super(saveLocationManager);
        this.downloadManager = downloadManager;
        this.libTorrentManager = libTorrentManager;
    }

    @Override
    public void init(File torrent) {
        this.torrent = torrent;
    }

    /**
     * Stops a torrent download. If the torrent is in seeding state, it does
     * nothing. (To stop a seeding torrent it must be stopped from the uploads
     * pane)
     */
    @Override
    public void stop() {

    }

    @Override
    public void pause() {
        libTorrentManager.pauseTorrent(id);
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public boolean isPausable() {
        // return torrent.isPausable();
        return false;
        // TODO

    }

    @Override
    public boolean isInactive() {
        // return isResumable() || torrent.getState() == TorrentState.QUEUED;
        return false;
        // TODO
    }

    @Override
    public boolean isLaunchable() {
        // return torrentFileSystem.getFiles().size() == 1
        // && torrentContext.getDiskManager().getLastVerifiedOffset() > 0;
        return false;
        // TODO
    }

    @Override
    public boolean isResumable() {
        // switch (torrent.getState()) {
        // case PAUSED:
        // case TRACKER_FAILURE:
        // return true;
        // }
        // return false;
        return false;
        // TODO
    }

    @Override
    public boolean resume() {
        libTorrentManager.resumeTorrent(id);
        return true;
    }

    @Override
    public File getFile() {
        // if (torrent.isComplete())
        // return torrentFileSystem.getCompleteFile();
        // return torrentFileSystem.getIncompleteFile();
        // TODO
        return null;
    }

    @Override
    public File getDownloadFragment() {
        // if (!isLaunchable())
        // return null;
        // if (torrent.isComplete())
        // return getFile();
        // long size = torrentContext.getDiskManager().getLastVerifiedOffset();
        // if (size <= 0)
        // return null;
        // File file = new
        // File(torrentFileSystem.getIncompleteFile().getParent(),
        // IncompleteFileManager.PREVIEW_PREFIX
        // + torrentFileSystem.getIncompleteFile().getName());
        // // Copy first block, returning if nothing was copied.
        // if (FileUtils.copy(torrentFileSystem.getIncompleteFile(), size, file)
        // <= 0)
        // return null;
        // return file;

        return null;
        // TODO
    }

    @Override
    public DownloadState getState() {
        // // aborted seeding torrents are shown as complete in the
        // // downloads pane.
        // if (torrent.isComplete())
        // return DownloadState.COMPLETE;
        // switch (torrent.getState()) {
        // case WAITING_FOR_TRACKER:
        // return DownloadState.WAITING_FOR_GNET_RESULTS;
        // case VERIFYING:
        // return DownloadState.RESUMING;
        // case CONNECTING:
        // return DownloadState.CONNECTING;
        // }
        //
        // if (torrent.isDownloading()) {
        // return DownloadState.DOWNLOADING;
        // }
        // switch (torrent.getState()) {
        // case SAVING:
        // return DownloadState.SAVING;
        // case SEEDING:
        // return DownloadState.COMPLETE;
        // case QUEUED:
        // return DownloadState.QUEUED;
        // case PAUSED:
        // return DownloadState.PAUSED;
        // case STOPPED:
        // return DownloadState.ABORTED;
        // case DISK_PROBLEM:
        // return DownloadState.DISK_PROBLEM;
        // case TRACKER_FAILURE:
        // return DownloadState.WAITING_FOR_USER; // let the user trigger a
        // // scrape
        // case SCRAPING:
        // return DownloadState.ITERATIVE_GUESSING; // bad name but practically
        // // the same
        // case INVALID:
        // return DownloadState.INVALID;
        // }
        // throw new IllegalStateException("unknown torrent state");

        return state;
        // TODO
    }

    @Override
    public int getRemainingStateTime() {
        // if (getState() != DownloadState.WAITING_FOR_GNET_RESULTS)
        // return 0;
        // return Math.max(0,
        // (int) (torrent.getNextTrackerRequestTime() -
        // System.currentTimeMillis()) / 1000);
        return 0;
        // TODO
    }

    @Override
    public long getContentLength() {
        // return torrentFileSystem.getTotalSize();
        return contentLength;
    }

    @Override
    public long getAmountRead() {
        // // if the download is complete, just return the length
        // if (btMetaInfo == null)
        // return getContentLength();
        //
        // // return the number of verified bytes
        // long ret = torrentContext.getDiskManager().getBlockSize();
        //
        // // if this is initial checking, add the number of processed bytes
        // // too.
        // if (torrent.getState() == TorrentState.VERIFYING)
        // ret += torrentContext.getDiskManager().getNumCorruptedBytes();
        // return ret;

        return amountRead;
        // TOOD
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
        return numPeers;
    }

    @Override
    public int getBusyHostCount() {
        return nonInterestingPeers;
    }

    @Override
    public int getQueuedHostCount() {
        return chokingPeers;
    }

    @Override
    public GUID getQueryGUID() {
        return null;
    }

    @Override
    public boolean isCompleted() {
        return finished;
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

    @Override
    public long getAmountVerified() {
        return amountVerified;
    }

    @Override
    public int getChunkSize() {
        return pieceLength;
    }

    @Override
    public long getAmountLost() {
        return amountLost;
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
        // return torrentFileSystem.getCompleteFile();
        // TODO
        return new File("SaveFile");
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
        return amountPending;
    }

    @Override
    public int getNumHosts() {
        return numConnections;
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
        
        state = DownloadState.CONNECTING;

        // TODO TODO

        id = libTorrentManager.addTorrent(torrent);
        
        LibTorrentStatus status = libTorrentManager.getStatus(id);
        LibTorrentState state = LibTorrentState.forId(status.state);
        setStatus(state);
        
        libTorrentManager.addListener(id, new EventListener<LibTorrentEvent>() {
            public void handleEvent(LibTorrentEvent event) {
                // TODO make threadsafe

                LibTorrentAlert alert = event.getAlert();
                LibTorrentStatus status = event.getTorrentStatus();

                paused = status.paused;
                amountVerified = status.total_done.longValue();
                amountRead = status.total_done.longValue();

                // TODO get real content length
                float length = amountVerified / status.progress;
                contentLength = (long) length;

                LibTorrentState state = LibTorrentState.forId(status.state);

                setStatus(state);
                System.out.println("event!");
            }

        });

        // TODO moving to complete folder when complete and starting to seed
        // again
    }

    private void setStatus(LibTorrentState state) {
        synchronized (BTDownloaderImpl.this) {

            switch (state) {
            case downloading:
                if (paused) {
                    BTDownloaderImpl.this.state = DownloadState.PAUSED;
                } else {
                    BTDownloaderImpl.this.state = DownloadState.DOWNLOADING;
                }
                break;
            case queued_for_checking:
                BTDownloaderImpl.this.state = DownloadState.RESUMING;
                break;
            case checking_files:
                BTDownloaderImpl.this.state = DownloadState.RESUMING;
                break;
            case seeding:
                BTDownloaderImpl.this.state = DownloadState.COMPLETE;
                break;
            case finished:
                BTDownloaderImpl.this.state = DownloadState.COMPLETE;
                break;
            case allocating:
                BTDownloaderImpl.this.state = DownloadState.CONNECTING;
                break;
            case downloading_metadata:
                BTDownloaderImpl.this.state = DownloadState.CONNECTING;
                break;
            }
        }
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
        return triedHostCount;
        // TODO
    }

    @Override
    public String getCustomIconDescriptor() {
        // if (torrentFileSystem.getFiles().size() == 1)
        // return null;
        // return BITTORRENT_DOWNLOAD;

        // TODO

        return BITTORRENT_DOWNLOAD;
    }

    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.BTDOWNLOADER;
    }

    @Override
    protected DownloadMemento createMemento() {
        if (finished)
            throw new IllegalStateException("creating memento for finished torrent!");
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<File> getFiles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<File> getIncompleteFiles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isMementoSupported() {
        return false;
    }

}

package org.limewire.libtorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentAlert;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.bittorrent.BTData.BTFileData;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.bittorrent.util.TorrentUtil;
import org.limewire.io.IOUtils;
import org.limewire.listener.AsynchronousMulticasterImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Class representing the torrent being downloaded. It is updated periodically
 * by the TorrentManager. It has all necessary helper methods to provide
 * functionality to the BTDownloaderImpl. It delegates calls to native methods
 * back to the TorrentManager.
 */
public class TorrentImpl implements Torrent {
    private static final Log LOG = LogFactory.getLog(TorrentImpl.class);

    private final EventMulticaster<TorrentEvent> listeners;

    private final TorrentManager torrentManager;

    private final AtomicReference<TorrentStatus> status;

    private final List<String> paths;

    private AtomicReference<File> torrentDataFile = new AtomicReference<File>(null);

    private AtomicReference<File> torrentFile = new AtomicReference<File>(null);

    private AtomicReference<File> fastResumeFile = new AtomicReference<File>(null);

    private String sha1 = null;

    private String name = null;

    private String trackerURL = null;

    private long totalSize = -1;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final AtomicBoolean complete = new AtomicBoolean(false);

    private Boolean isPrivate = null;

    @Inject
    public TorrentImpl(TorrentManager torrentManager,
            @Named("fastExecutor") ScheduledExecutorService fastExecutor) {
        this.torrentManager = torrentManager;
        listeners = new AsynchronousMulticasterImpl<TorrentEvent>(fastExecutor);
        status = new AtomicReference<TorrentStatus>();
        paths = new ArrayList<String>();
    }

    @Override
    public void addListener(EventListener<TorrentEvent> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<TorrentEvent> listener) {
        return listeners.removeListener(listener);
    }

    @Override
    public synchronized void init(String name, String sha1, long totalSize, String trackerURL,
            List<String> paths, File fastResumeFile, File torrentFile, File torrentDataFile,
            Boolean isPrivate) throws IOException {

        this.sha1 = sha1;
        this.trackerURL = trackerURL;
        if (paths != null) {
            this.paths.addAll(paths);
        }
        this.totalSize = totalSize;
        File torrentDownloadFolder = torrentManager.getTorrentSettings().getTorrentDownloadFolder();

        if (name != null) {
            this.name = name;
        }

        if (isPrivate != null) {
            this.isPrivate = isPrivate.booleanValue();
        }

        if (torrentFile != null && torrentFile.exists()) {
            FileInputStream fis = null;
            FileChannel fileChannel = null;
            try {
                fis = new FileInputStream(torrentFile);
                fileChannel = fis.getChannel();
                Map metaInfo = (Map) Token.parse(fileChannel);
                BTData btData = new BTDataImpl(metaInfo);
                if (this.name == null) {
                    this.name = btData.getName();
                }
                this.totalSize = getTotalSize(btData);

                buildPaths(btData);

                if (this.trackerURL == null) {
                    this.trackerURL = btData.getAnnounce();
                }

                if (this.sha1 == null) {
                    this.sha1 = StringUtils.toHexString(btData.getInfoHash());
                }

                if (this.isPrivate == null) {
                    this.isPrivate = btData.isPrivate();
                }

            } finally {
                IOUtils.close(fileChannel);
                IOUtils.close(fis);
            }
        }

        if (this.isPrivate == null) {
            // private by default if unknown
            this.isPrivate = Boolean.TRUE;
        }

        if (this.name == null || torrentDownloadFolder == null || this.totalSize <= 0
                || this.sha1 == null) {
            throw new IOException("There was an error initializing the torrent.");
        }

        this.fastResumeFile.set(fastResumeFile == null ? new File(torrentDownloadFolder, this.name
                + ".fastresume") : fastResumeFile);
        this.torrentDataFile.set(torrentDataFile == null ? new File(torrentDownloadFolder,
                this.name) : torrentDataFile);
        this.torrentFile.set(torrentFile == null ? new File(torrentDownloadFolder, this.name
                + ".torrent") : torrentFile);
    }

    private void buildPaths(BTData btData) {
        if (this.paths.size() == 0) {
            if (btData.getFiles() != null) {
                for (BTFileData fileData : btData.getFiles()) {
                    this.paths.add(fileData.getPath());
                }
            }
        }
    }

    private long getTotalSize(BTData btData) {
        long totalSize = 0;
        if (btData.getLength() == null) {
            if (btData.getFiles() != null) {
                for (BTFileData file : btData.getFiles()) {
                    totalSize += file.getLength();
                }
            }
        } else {
            totalSize = btData.getLength();
        }
        return totalSize;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void start() {
        if (!started.getAndSet(true)) {
            resume();
        }
    }

    @Override
    public File getTorrentFile() {
        return torrentFile.get();
    }

    @Override
    public File getFastResumeFile() {
        return fastResumeFile.get();
    }

    @Override
    public List<String> getPeers() {
        return torrentManager.getPeers(this);
    }

    @Override
    public synchronized void moveTorrent(File directory) {
        assert complete.get();
        torrentManager.moveTorrent(this, directory);
        torrentDataFile.set(new File(directory, torrentDataFile.get().getName()));

        // TODO would be nice to move the following logic to something outside of
        // the torrent code, since it is not really the torrent codes
        // responsibility.
        File oldFastResumeFile = fastResumeFile.get();
        File oldTorrentFile = torrentFile.get();
        fastResumeFile.set(new File(torrentManager.getTorrentSettings().getTorrentUploadsFolder(),
                oldFastResumeFile.getName()));
        torrentFile.set(new File(torrentManager.getTorrentSettings().getTorrentUploadsFolder(),
                oldTorrentFile.getName()));
        
        FileUtils.copy(oldTorrentFile, torrentFile.get());
        FileUtils.copy(oldFastResumeFile, fastResumeFile.get());
        FileUtils.forceDelete(oldTorrentFile);
        FileUtils.forceDelete(oldFastResumeFile);
    }

    @Override
    public void pause() {
        torrentManager.pauseTorrent(this);
    }

    @Override
    public void resume() {
        if (getStatus().isError()) {
            torrentManager.recoverTorrent(this);
        } else {
            torrentManager.resumeTorrent(this);
        }
    }

    @Override
    public float getDownloadRate() {
        TorrentStatus status = this.status.get();
        return status == null ? 0 : status.getDownloadRate();
    }

    @Override
    public String getSha1() {
        return sha1;
    }

    @Override
    public boolean isPaused() {
        TorrentStatus status = this.status.get();
        return status == null ? false : status.isPaused();
    }

    @Override
    public boolean isFinished() {
        return complete.get();
    }

    @Override
    public long getTotalSize() {
        return totalSize;
    }

    @Override
    public boolean isStarted() {
        return started.get();
    }

    @Override
    public String getTrackerURL() {
        return trackerURL;
    }

    @Override
    public boolean isMultiFileTorrent() {
        return paths.size() > 0;
    }

    @Override
    public long getTotalDownloaded() {
        TorrentStatus status = this.status.get();
        if (status == null) {
            return -1;
        } else {
            return status.getTotalDone();
        }
    }

    @Override
    public int getNumPeers() {
        TorrentStatus status = this.status.get();
        return status == null ? 0 : status.getNumPeers();
    }

    @Override
    public List<String> getPaths() {
        return paths;
    }

    @Override
    public File getTorrentDataFile() {
        return torrentDataFile.get();
    }

    @Override
    public boolean isSingleFileTorrent() {
        return !isMultiFileTorrent();
    }

    @Override
    public void stop() {
        if (!cancelled.getAndSet(true)) {
            torrentManager.removeTorrent(this);
        }
        listeners.broadcast(TorrentEvent.STOPPED);
    }

    @Override
    public long getTotalUploaded() {
        TorrentStatus status = this.status.get();
        if (status == null) {
            return 0;
        } else {
            return status.getTotalUpload();
        }
    }

    @Override
    public int getNumUploads() {
        TorrentStatus status = this.status.get();
        if (status == null) {
            return 0;
        } else {
            return status.getNumUploads();
        }
    }

    @Override
    public float getUploadRate() {
        TorrentStatus status = this.status.get();
        return status == null ? 0 : status.getUploadRate();
    }

    @Override
    public float getSeedRatio() {
        TorrentStatus status = this.status.get();
        if (status != null && status.getTotalDownload() != 0) {
            return (status.getTotalUpload() / getTotalSize());
        }
        return 0;
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public TorrentStatus getStatus() {
        return status.get();
    }

    @Override
    public void updateStatus(TorrentStatus torrentStatus) {
        if (!cancelled.get()) {
            synchronized (TorrentImpl.this) {
                TorrentImpl.this.status.set(torrentStatus);
                boolean newlyfinished = complete.get() != torrentStatus.isFinished()
                        && torrentStatus.isFinished();
                complete.set(torrentStatus.isFinished());

                if (newlyfinished) {
                    listeners.broadcast(TorrentEvent.COMPLETED);
                } else {
                    listeners.broadcast(TorrentEvent.STATUS_CHANGED);
                }
            }
        }
    }

    @Override
    public void alert(TorrentAlert alert) {
        synchronized (TorrentImpl.this) {
            if (alert.getCategory() == TorrentAlert.SAVE_RESUME_DATA_ALERT) {
                listeners.broadcast(TorrentEvent.FAST_RESUME_FILE_SAVED);
            }
        }
    }

    private List<File> getTorrentDataFiles() {
        return TorrentUtil.buildTorrentFiles(this, getTorrentDataFile());
    }

    @Override
    public boolean registerWithTorrentManager() {
        if (!torrentManager.isValid()) {
            return false;
        }

        for (File file : getTorrentDataFiles()) {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                try {
                    file.createNewFile();
                } catch (Throwable e) {
                    // non-fatal libtorrent will create them
                    LOG.error("Error touching file: " + file, e);
                }
            }
        }

        File torrent = torrentFile.get();
        File torrentParent = torrent.getParentFile();
        File torrentDownloadFolder = torrentManager.getTorrentSettings().getTorrentDownloadFolder();
        File torrentUploadFolder = torrentManager.getTorrentSettings().getTorrentUploadsFolder();
        if (!torrentParent.equals(torrentDownloadFolder)
                && !torrentParent.equals(torrentUploadFolder)) {
            // if the torrent file is not located in the incomplete or upload
            // directories it should be copied to the directory the torrent is
            // being downloaded to. This is to prevent the user from deleting
            // the torrent which we need to initiate a download properly.
            File newTorrentFile = new File(torrentDownloadFolder, getName() + ".torrent");
            FileUtils.copy(torrentFile.get(), newTorrentFile);
            torrentFile.set(newTorrentFile);
        }
        torrentManager.registerTorrent(this);
        return true;
    }

    @Override
    public int getNumConnections() {
        TorrentStatus status = getStatus();
        if (status != null) {
            return status.getNumConnections();
        }
        return 0;
    }

    @Override
    public boolean isPrivate() {
        return isPrivate;
    }
}

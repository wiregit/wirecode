package org.limewire.libtorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentAlert;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentEventType;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.bittorrent.TorrentParams;
import org.limewire.bittorrent.TorrentPeer;
import org.limewire.bittorrent.TorrentPiecesInfo;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.io.IOUtils;
import org.limewire.listener.AsynchronousEventMulticaster;
import org.limewire.listener.AsynchronousMulticasterImpl;
import org.limewire.listener.EventListener;
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
    private final Map<String, Object> properties = Collections
            .synchronizedMap(new HashMap<String, Object>(2));

    private final AsynchronousEventMulticaster<TorrentEvent> listeners;

    private final AtomicReference<TorrentStatus> status = new AtomicReference<TorrentStatus>(null);

    private final AtomicReference<TorrentInfo> torrentInfo = new AtomicReference<TorrentInfo>(null);

    private final AtomicReference<File> torrentDataFile = new AtomicReference<File>(null);

    private final AtomicReference<File> torrentFile = new AtomicReference<File>(null);

    private final AtomicReference<File> fastResumeFile = new AtomicReference<File>(null);

    private String sha1 = null;

    private String name = null;

    private String trackerURL = null;

    private final AtomicLong startTime = new AtomicLong(-1);

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    // used to decide if the torrent was just newly completed or not.
    private final AtomicBoolean complete = new AtomicBoolean(false);

    private final AtomicBoolean isPrivate = new AtomicBoolean(true);

    private final Lock lock = new ReentrantLock();

    private final LibTorrentWrapper libTorrent;

    @Inject
    public TorrentImpl(LibTorrentWrapper libTorrent,
            @Named("fastExecutor") ScheduledExecutorService fastExecutor) {
        this.libTorrent = libTorrent;
        listeners = new AsynchronousMulticasterImpl<TorrentEvent>(fastExecutor);
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
    public void init(TorrentParams params) throws IOException {
        lock.lock();
        try {
            this.sha1 = params.getSha1();
            this.trackerURL = params.getTrackerURL();
            this.name = params.getName();

            Boolean isPrivate = params.getPrivate();

            File torrentFile = params.getTorrentFile();
            File fastResumeFile = params.getFastResumeFile();
            File torrentDataFile = params.getTorrentDataFile();

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

                    if (this.trackerURL == null) {
                        this.trackerURL = btData.getTrackerUris().get(0).toASCIIString();
                    }

                    if (this.sha1 == null) {
                        this.sha1 = StringUtils.toHexString(btData.getInfoHash());
                    }

                    if (isPrivate == null) {
                        isPrivate = btData.isPrivate();
                    }

                } finally {
                    IOUtils.close(fileChannel);
                    IOUtils.close(fis);
                }
            }

            if (isPrivate != null) {
                this.isPrivate.set(isPrivate);
            }

            File downloadFolder = params.getDownloadFolder();

            if (this.name == null || downloadFolder == null || this.sha1 == null) {
                throw new IOException("There was an error initializing the torrent.");
            }

            this.fastResumeFile.set(fastResumeFile == null ? new File(downloadFolder, this.name
                    + ".fastresume") : fastResumeFile);
            this.torrentDataFile.set(torrentDataFile == null ? new File(downloadFolder, this.name)
                    : torrentDataFile);
            this.torrentFile.set(torrentFile == null ? new File(downloadFolder, this.name
                    + ".torrent") : torrentFile);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void start() {
        if (!started.getAndSet(true)) {
            lock.lock();
            try {
                startTime.set(System.currentTimeMillis());
                resume();
                listeners.broadcast(new TorrentEvent(this, TorrentEventType.STARTED));
            } finally {
                lock.unlock();
            }
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
    public void moveTorrent(File directory) {
        lock.lock();
        try {
            assert isFinished();
            libTorrent.move_torrent(sha1, directory.getAbsolutePath());
            torrentDataFile.set(new File(directory, torrentDataFile.get().getName()));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void pause() {
        lock.lock();
        try {
            libTorrent.pause_torrent(sha1);
            updateStatus(getStatusInner());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setTorrentFile(File torrentFile) {
        this.torrentFile.set(torrentFile);
    }

    @Override
    public void setFastResumeFile(File fastResumeFile) {
        this.fastResumeFile.set(fastResumeFile);
    }

    @Override
    public void resume() {
        lock.lock();
        try {
            if (getStatus().isError()) {
                libTorrent.clear_error_and_retry(sha1);
            } else {
                libTorrent.resume_torrent(sha1);
            }
            updateStatus(getStatusInner());
        } finally {
            lock.unlock();
        }
    }

    private LibTorrentStatus getStatusInner() {
        LibTorrentStatus status = new LibTorrentStatus();
        libTorrent.get_torrent_status(sha1, status);
        libTorrent.free_torrent_status(status);
        return status;
    }

    @Override
    public float getDownloadRate() {
        TorrentStatus status = this.status.get();
        return status == null ? 0 : status.getDownloadPayloadRate();
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
        TorrentStatus status = this.status.get();
        return status == null ? false : status.isFinished();
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
    public int getNumPeers() {
        TorrentStatus status = this.status.get();
        return status == null ? 0 : status.getNumPeers();
    }

    @Override
    public File getTorrentDataFile() {
        return torrentDataFile.get();
    }

    @Override
    public void stop() {
        lock.lock();
        try {
            if (started.get() && !cancelled.getAndSet(true)) {
                // updating the torrent info object 1 last time.
                if (isValid() && hasMetaData()) {
                    TorrentInfo ti = libTorrent.get_torrent_info(sha1);
                    torrentInfo.set(ti);
                }
            }
            listeners.broadcast(new TorrentEvent(this, TorrentEventType.STOPPED));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getTotalUploaded() {
        TorrentStatus status = this.status.get();
        if (status == null) {
            return 0;
        } else {
            return status.getAllTimePayloadUpload();
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
        return status == null ? 0 : status.getUploadPayloadRate();
    }

    @Override
    public float getSeedRatio() {
        TorrentStatus status = this.status.get();
        if (status != null) {
            float seedRatio = status.getSeedRatio();
            return seedRatio;
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
        lock.lock();
        try {
            if (!cancelled.get()) {
                TorrentImpl.this.status.set(torrentStatus);
                boolean newlyfinished = !complete.get() && torrentStatus.isFinished();
                complete.set(torrentStatus.isFinished());

                if (newlyfinished) {
                    listeners.broadcast(new TorrentEvent(this, TorrentEventType.COMPLETED));
                } else {
                    listeners.broadcast(new TorrentEvent(this, TorrentEventType.STATUS_CHANGED));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void handleFastResumeAlert(TorrentAlert alert) {
        lock.lock();
        try {
            listeners.broadcast(new TorrentEvent(this, TorrentEventType.FAST_RESUME_FILE_SAVED));
        } finally {
            lock.unlock();
        }
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
        return isPrivate.get();
    }

    @Override
    public List<TorrentFileEntry> getTorrentFileEntries() {
        lock.lock();
        try {
            if (!isValid()) {
                TorrentInfo torrentInfo = this.torrentInfo.get();
                if (torrentInfo == null) {
                    return Collections.emptyList();
                }
                return torrentInfo.getTorrentFileEntries();
            }

            TorrentFileEntry[] files = libTorrent.get_files(sha1);
            return Arrays.asList(files);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<TorrentPeer> getTorrentPeers() {
        lock.lock();
        try {
            TorrentPeer[] peers = libTorrent.get_peers(sha1);
            return Arrays.asList(peers);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isAutoManaged() {
        TorrentStatus status = this.status.get();
        return status == null ? false : status.isAutoManaged();
    }

    @Override
    public void setAutoManaged(boolean autoManaged) {
        lock.lock();
        try {
            libTorrent.set_auto_managed_torrent(sha1, autoManaged);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setTorrenFileEntryPriority(TorrentFileEntry torrentFileEntry, int priority) {
        lock.lock();
        try {
            libTorrent.set_file_priority(sha1, torrentFileEntry.getIndex(), priority);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public File getTorrentDataFile(TorrentFileEntry torrentFileEntry) {
        return new File(getTorrentDataFile().getParent(), torrentFileEntry.getPath());
    }

    @Override
    public boolean hasMetaData() {
        lock.lock();
        try {
            return torrentInfo.get() != null || libTorrent.has_metadata(sha1);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TorrentInfo getTorrentInfo() {
        lock.lock();
        try {
            if (isValid() && hasMetaData() && torrentInfo.get() == null) {
                TorrentInfo ti = libTorrent.get_torrent_info(sha1);
                torrentInfo.set(ti);
            }
            return torrentInfo.get();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getProperty(String key, T defaultValue) {
        T value = (T)properties.get(key);
        if(value == null) {
            value = defaultValue;
        }
        return value;
    }

    @Override
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    @Override
    public boolean isValid() {
        lock.lock();
        try {
            return libTorrent.is_valid(sha1);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long getStartTime() {
        return startTime.get();
    }

    @Override
    public Lock getLock() {
        return lock;
    }

    @Override
    public void forceReannounce() {
        lock.lock();
        try {
            libTorrent.force_reannounce(sha1);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void scrapeTracker() {
        lock.lock();
        try {
            libTorrent.scrape_tracker(sha1);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void saveFastResumeData() {
        lock.lock();
        try {
            libTorrent.signal_fast_resume_data_request(sha1);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public TorrentPiecesInfo getPiecesInfo() {
        lock.lock();
        try {
            return libTorrent.get_pieces_status(sha1);
        } finally {
            lock.unlock();
        }
    }
}

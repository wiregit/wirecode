package org.limewire.libtorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentAlert;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.bittorrent.TorrentInfo;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentParams;
import org.limewire.bittorrent.TorrentPeer;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.io.IOUtils;
import org.limewire.listener.AsynchronousEventMulticaster;
import org.limewire.listener.AsynchronousMulticasterImpl;
import org.limewire.listener.EventListener;
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
    private final Map<String, Object> properties = Collections.synchronizedMap(new HashMap<String, Object>(2));

    private final AsynchronousEventMulticaster<TorrentEvent> listeners;

    private final TorrentManager torrentManager;

    private final AtomicReference<TorrentStatus> status = new AtomicReference<TorrentStatus>(null);

    private final AtomicReference<TorrentInfo> torrentInfo = new AtomicReference<TorrentInfo>(null);

    private final AtomicReference<File> torrentDataFile = new AtomicReference<File>(null);

    private final AtomicReference<File> torrentFile = new AtomicReference<File>(null);

    private final AtomicReference<File> fastResumeFile = new AtomicReference<File>(null);

    private String sha1 = null;

    private String name = null;

    private String trackerURL = null;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    // used to decide if the torrent was just newly completed or not.
    private final AtomicBoolean complete = new AtomicBoolean(false);

    private final AtomicBoolean isPrivate = new AtomicBoolean(true);

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Inject
    public TorrentImpl(TorrentManager torrentManager,
            @Named("fastExecutor") ScheduledExecutorService fastExecutor) {
        this.torrentManager = torrentManager;
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
        lock.writeLock().lock();
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
                        this.trackerURL = btData.getAnnounce();
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

            File torrentDownloadFolder = torrentManager.getTorrentManagerSettings()
                    .getTorrentDownloadFolder();

            if (this.name == null || torrentDownloadFolder == null || this.sha1 == null) {
                throw new IOException("There was an error initializing the torrent.");
            }

            this.fastResumeFile.set(fastResumeFile == null ? new File(torrentDownloadFolder,
                    this.name + ".fastresume") : fastResumeFile);
            this.torrentDataFile.set(torrentDataFile == null ? new File(torrentDownloadFolder,
                    this.name) : torrentDataFile);
            this.torrentFile.set(torrentFile == null ? new File(torrentDownloadFolder, this.name
                    + ".torrent") : torrentFile);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void start() {
        if (!started.getAndSet(true)) {
            resume();
            listeners.broadcast(TorrentEvent.STARTED);
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
        lock.writeLock().lock();
        try {
            // TODO potentially rename the method, or at least put in another
            // parameter to use as the directory to move the torrent file and
            // fast resume files to.
            assert isFinished();
            torrentManager.moveTorrent(this, directory);
            torrentDataFile.set(new File(directory, torrentDataFile.get().getName()));

            // TODO would be nice to move the following logic to something
            // outside of the torrent code, since it is not really the torrent
            // codes responsibility.
            File oldFastResumeFile = fastResumeFile.get();
            File oldTorrentFile = torrentFile.get();
            fastResumeFile.set(new File(torrentManager.getTorrentManagerSettings()
                    .getTorrentUploadsFolder(), oldFastResumeFile.getName()));
            torrentFile.set(new File(torrentManager.getTorrentManagerSettings()
                    .getTorrentUploadsFolder(), oldTorrentFile.getName()));

            FileUtils.copy(oldTorrentFile, torrentFile.get());
            FileUtils.copy(oldFastResumeFile, fastResumeFile.get());
            FileUtils.forceDelete(oldTorrentFile);
            FileUtils.forceDelete(oldFastResumeFile);
        } finally {
            lock.writeLock().unlock();
        }
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
        lock.writeLock().lock();
        try {
            if (started.get() && !cancelled.getAndSet(true)) {
                // updating the torrent info object 1 last time.
                if (isValid() && hasMetaData()) {
                    TorrentInfo ti = torrentManager.getTorrentInfo(this);
                    torrentInfo.set(ti);
                }
                // removing the torrent handle from libtorrent.
                torrentManager.removeTorrent(this);
            }
            listeners.broadcast(TorrentEvent.STOPPED);
        } finally {
            lock.writeLock().unlock();
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
        lock.writeLock().lock();
        try {
            if (!cancelled.get()) {
                TorrentImpl.this.status.set(torrentStatus);
                boolean newlyfinished = !complete.get() && torrentStatus.isFinished();
                complete.set(torrentStatus.isFinished());

                if (newlyfinished) {
                    listeners.broadcast(TorrentEvent.COMPLETED);
                } else {
                    listeners.broadcast(TorrentEvent.STATUS_CHANGED);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void handleFastResumeAlert(TorrentAlert alert) {
        lock.writeLock().lock();
        try {
            listeners.broadcast(TorrentEvent.FAST_RESUME_FILE_SAVED);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean registerWithTorrentManager() {
        lock.writeLock().lock();
        if (!torrentManager.isValid()) {
            return false;
        }

        try {
            File torrent = torrentFile.get();
            File torrentParent = torrent.getParentFile();
            File torrentDownloadFolder = torrentManager.getTorrentManagerSettings()
                    .getTorrentDownloadFolder();
            File torrentUploadFolder = torrentManager.getTorrentManagerSettings()
                    .getTorrentUploadsFolder();
            if (!torrentParent.equals(torrentDownloadFolder)
                    && !torrentParent.equals(torrentUploadFolder)) {
                // if the torrent file is not located in the incomplete or
                // upload
                // directories it should be copied to the directory the torrent
                // is
                // being downloaded to. This is to prevent the user from
                // deleting
                // the torrent which we need to initiate a download properly.
                torrentDownloadFolder.mkdirs();
                File newTorrentFile = new File(torrentDownloadFolder, getName() + ".torrent");
                FileUtils.copy(torrentFile.get(), newTorrentFile);
                torrentFile.set(newTorrentFile);
            }
            torrentManager.registerTorrent(this);
        } finally {
            lock.writeLock().unlock();
        }
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
        return isPrivate.get();
    }

    @Override
    public List<TorrentFileEntry> getTorrentFileEntries() {
        lock.readLock().lock();
        try {
            if (!isValid()) {
                TorrentInfo torrentInfo = this.torrentInfo.get();
                if (torrentInfo == null) {
                    return Collections.emptyList();
                }
                return torrentInfo.getTorrentFileEntries();
            }

            return torrentManager.getTorrentFileEntries(this);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<TorrentPeer> getTorrentPeers() {
        lock.readLock().lock();
        try {
            return torrentManager.getTorrentPeers(this);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isAutoManaged() {
        TorrentStatus status = this.status.get();
        return status == null ? false : status.isAutoManaged();
    }

    @Override
    public void setAutoManaged(boolean autoManaged) {
        lock.writeLock().lock();
        try {
            torrentManager.setAutoManaged(this, autoManaged);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void setTorrenFileEntryPriority(TorrentFileEntry torrentFileEntry, int priority) {
        lock.writeLock().lock();
        try {
            torrentManager.setTorrenFileEntryPriority(this, torrentFileEntry, priority);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public File getTorrentDataFile(TorrentFileEntry torrentFileEntry) {
        return new File(getTorrentDataFile().getParent(), torrentFileEntry.getPath());
    }

    @Override
    public boolean hasMetaData() {
        lock.readLock().lock();
        try {
            return torrentInfo.get() != null || torrentManager.hasMetaData(this);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public TorrentInfo getTorrentInfo() {
        lock.readLock().lock();
        try {
            if (isValid() && hasMetaData() && torrentInfo.get() == null) {
                TorrentInfo ti = torrentManager.getTorrentInfo(this);
                torrentInfo.set(ti);
            }
            return torrentInfo.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    @Override
    public boolean isValid() {
        lock.readLock().lock();
        try {
            return torrentManager.isValid(this);
        } finally {
            lock.readLock().unlock();
        }
    }
}

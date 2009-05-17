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
import org.limewire.bittorrent.BTData.BTFileData;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.io.IOUtils;
import org.limewire.listener.AsynchronousMulticaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class Torrent implements ListenerSupport<TorrentEvent> {
    private final EventMulticaster<TorrentEvent> listeners;

    private final TorrentManager torrentManager;

    private final AtomicReference<LibTorrentStatus> status;

    private File incompleteFile = null;

    private File completeFile;

    private File torrentFile = null;

    private File fastResumeFile = null;

    private List<String> paths;

    private String sha1 = null;

    private String name;

    private String trackerURL;

    private long totalSize = -1;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final AtomicBoolean complete = new AtomicBoolean(false);

    @Inject
    public Torrent(TorrentManager torrentManager,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.torrentManager = torrentManager;
        // TODO not background executor?
        this.listeners = new AsynchronousMulticaster<TorrentEvent>(backgroundExecutor);
        this.status = new AtomicReference<LibTorrentStatus>();
        this.paths = new ArrayList<String>();
    }

    public void addListener(EventListener<TorrentEvent> listener) {
        listeners.addListener(listener);
    }

    public boolean removeListener(EventListener<TorrentEvent> listener) {
        return listeners.removeListener(listener);
    }

    public synchronized void init(String name, String sha1, long totalSize, String trackerURL,
            List<String> paths, File saveLocation, File fastResumeFile, File torrentFile) {
        this.name = name;
        File torrentDownloadFolder = torrentManager.getTorrentDownloadFolder();
        this.incompleteFile = new File(torrentDownloadFolder, name);
        this.completeFile = saveLocation;
        this.sha1 = sha1;
        this.trackerURL = trackerURL;
        this.paths.addAll(paths);
        this.totalSize = totalSize;
        this.fastResumeFile = fastResumeFile;

        if (torrentFile != null && torrentFile.exists()) {
            this.torrentFile = torrentFile;
        }
    }

    public synchronized void init(File torrentFile, File saveDir) throws IOException {
        FileInputStream fis = null;
        FileChannel fileChannel = null;
        File torrentDownloadFolder = torrentManager.getTorrentDownloadFolder();

        try {
            fis = new FileInputStream(torrentFile);
            fileChannel = fis.getChannel();
            Map metaInfo = (Map) Token.parse(fileChannel);
            BTData btData = new BTDataImpl(metaInfo);
            name = btData.getName();
            if (btData.getLength() == null) {
                this.totalSize = 0;
                if (btData.getFiles() != null) {
                    for (BTFileData file : btData.getFiles()) {
                        this.totalSize += file.getLength();
                    }
                }
            } else {
                this.totalSize = btData.getLength();
            }

            incompleteFile = new File(torrentDownloadFolder, name);
            fastResumeFile = new File(torrentDownloadFolder, name + ".fastresume");
            completeFile = new File(saveDir, name);

            if (btData.getFiles() != null) {
                for (BTFileData fileData : btData.getFiles()) {
                    paths.add(fileData.getPath());
                }
            }

            trackerURL = btData.getAnnounce();

            sha1 = StringUtils.toHexString(btData.getInfoHash());

        } finally {
            IOUtils.close(fileChannel);
            IOUtils.close(fis);
        }

        File torrentFileCopy = new File(torrentDownloadFolder, name + ".torrent");
        if (!torrentFile.equals(torrentFileCopy)) {
            FileUtils.copy(torrentFile, torrentFileCopy);
        }
        this.torrentFile = torrentFileCopy;
    }

    public String getName() {
        return name;
    }

    public void start() {
        if (!started.getAndSet(true)) {
            resume();
        }
    }

    public File getTorrentFile() {
        return torrentFile;
    }

    public File getFastResumeFile() {
        return fastResumeFile;
    }

    public List<String> getPeers() {
        return torrentManager.getPeers(this);
    }

    public void moveTorrent(File directory) {
        torrentManager.moveTorrent(this, directory);
    }

    public void pause() {
        torrentManager.pauseTorrent(this);
    }

    public void resume() {
        torrentManager.resumeTorrent(this);
    }

    public float getDownloadRate() {
        LibTorrentStatus status = this.status.get();
        return status == null ? 0 : status.download_rate;
    }

    public String getSha1() {
        return sha1;
    }

    public boolean isPaused() {
        LibTorrentStatus status = this.status.get();
        return status == null ? false : status.isPaused();
    }

    public boolean isFinished() {
        return complete.get();
    }

    public long getTotalSize() {
        return totalSize;
    }

    public boolean isStarted() {
        return started.get();
    }

    public String getTrackerURL() {
        return trackerURL;
    }

    public boolean isMultiFileTorrent() {
        return paths.size() > 0;
    }

    public long getTotalDownloaded() {
        LibTorrentStatus status = this.status.get();
        if (status == null) {
            return -1;
        } else {
            return status.getTotalDone();
        }
    }

    public int getNumPeers() {
        LibTorrentStatus status = this.status.get();
        return status == null ? 0 : status.num_peers;
    }

    public List<String> getPaths() {
        return paths;
    }

    private List<File> collectFiles(File rootFile) {
        List<File> files = new ArrayList<File>();
        if (paths.size() > 0) {
            for (String path : paths) {
                File file = new File(rootFile, path);
                files.add(file);
            }
        } else {
            files.add(rootFile);
        }
        return files;
    }

    public List<File> getCompleteFiles() {
        return collectFiles(getCompleteFile());
    }

    public List<File> getIncompleteFiles() {
        return collectFiles(getIncompleteFile());
    }

    public File getIncompleteFile() {
        return incompleteFile;
    }

    public File getCompleteFile() {
        return completeFile;
    }

    public boolean isSingleFileTorrent() {
        return !isMultiFileTorrent();
    }

    public void stop() {
        if (!cancelled.getAndSet(true)) {
            if (started.getAndSet(false)) {
                torrentManager.removeTorrent(this);
            }
            listeners.broadcast(TorrentEvent.STOPPED);
        }
    }

    public long getTotalUploaded() {
        LibTorrentStatus status = this.status.get();
        if (status == null) {
            return 0;
        } else {
            return status.getTotalUpload();
        }
    }

    public int getNumUploads() {
        LibTorrentStatus status = this.status.get();
        if (status == null) {
            return 0;
        } else {
            return status.num_uploads;
        }
    }

    public float getUploadRate() {
        LibTorrentStatus status = this.status.get();
        return status == null ? 0 : status.upload_rate;
    }

    public float getSeedRatio() {
        LibTorrentStatus status = this.status.get();
        if (status != null && status.getTotalDownload() != 0) {
            return (status.getTotalUpload() / status.getTotalDownload());
        }
        return 0;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public LibTorrentStatus getStatus() {
        return status.get();
    }

    public void updateStatus(LibTorrentStatus torrentStatus) {
        if (!cancelled.get()) {
            synchronized (Torrent.this) {
                Torrent.this.status.set(torrentStatus);
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
}
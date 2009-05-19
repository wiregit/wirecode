package org.limewire.libtorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

/**
 * Class representing the torrent being downloaded. It is updated periodically
 * by the TorrentManager. It has all necessary helper methods to provide
 * functionality to the BTDownloaderImpl. It delegates calls to native methods
 * back to the TorrentManager.
 */
public class Torrent implements ListenerSupport<TorrentEvent> {
    private final EventMulticaster<TorrentEvent> listeners;

    private final TorrentManager torrentManager;

    private final AtomicReference<LibTorrentStatus> status;

    private final List<String> paths;

    private File incompleteFile = null;

    private File completeFile = null;

    private File torrentFile = null;

    private volatile File fastResumeFile = null;

    private String sha1 = null;

    private String name = null;

    private String trackerURL = null;

    private long totalSize = -1;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final AtomicBoolean complete = new AtomicBoolean(false);

    @Inject
    public Torrent(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
        this.listeners = new AsynchronousMulticaster<TorrentEvent>(torrentManager
                .getTorrentExecutor());
        this.status = new AtomicReference<LibTorrentStatus>();
        this.paths = new ArrayList<String>();
    }

    public void addListener(EventListener<TorrentEvent> listener) {
        listeners.addListener(listener);
    }

    public boolean removeListener(EventListener<TorrentEvent> listener) {
        return listeners.removeListener(listener);
    }

    /**
     * Initializes the torrent from the given fields. Either the torrentFile and
     * saveDir fields cannot be null. Or the name, sha1, long totalSize,
     * trackerURL, paths, and saveDir fields must be set.
     * <p>
     * Otherwise if torrentFile is set and other fields are as well, the field
     * passed in will be used, and any missing field will be pulled from the
     * torrent file.
     */
    public synchronized void init(String name, String sha1, long totalSize, String trackerURL,
            List<String> paths, File fastResumeFile, File torrentFile, File saveDir)
            throws IOException {

        assert (name != null && sha1 != null && totalSize > 0 && trackerURL != null
                && paths != null && paths.size() > 0 && saveDir != null)
                || (torrentFile != null && torrentFile.exists() && saveDir != null);

        this.sha1 = sha1;
        this.trackerURL = trackerURL;
        if (paths != null) {
            this.paths.addAll(paths);
        }
        this.totalSize = totalSize;

        File torrentDownloadFolder = torrentManager.getTorrentDownloadFolder();

        if (name != null) {
            this.name = name;
            this.incompleteFile = new File(torrentDownloadFolder, name);
            this.fastResumeFile = fastResumeFile == null ? new File(torrentDownloadFolder, name
                    + ".fastresume") : fastResumeFile;
            this.completeFile = new File(saveDir, name);
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

                if (this.paths.size() == 0) {
                    if (btData.getFiles() != null) {
                        for (BTFileData fileData : btData.getFiles()) {
                            this.paths.add(fileData.getPath());
                        }
                    }
                }

                if (this.trackerURL == null) {
                    this.trackerURL = btData.getAnnounce();
                }

                if (this.sha1 == null) {
                    this.sha1 = StringUtils.toHexString(btData.getInfoHash());
                }

            } finally {
                IOUtils.close(fileChannel);
                IOUtils.close(fis);
            }

            this.incompleteFile = new File(torrentDownloadFolder, this.name);
            this.fastResumeFile = fastResumeFile == null ? new File(torrentDownloadFolder,
                    this.name + ".fastresume") : fastResumeFile;
            this.completeFile = new File(saveDir, this.name);

            File torrentFileCopy = new File(torrentDownloadFolder, this.name + ".torrent");
            if (!torrentFile.equals(torrentFileCopy)) {
                FileUtils.copy(torrentFile, torrentFileCopy);
            }
            this.torrentFile = torrentFileCopy;
        }

        for (File file : getIncompleteFiles()) {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        }
    }

    /**
     * Returns the name of this torrent.
     */
    public String getName() {
        return name;
    }

    /**
     * Starts the torrent.
     */
    public void start() {
        if (!started.getAndSet(true)) {
            resume();
        }
    }

    /**
     * Returns the torrent file backing this torrent if any exist. Null or a
     * non-existent file can be returned.
     */
    public File getTorrentFile() {
        return torrentFile;
    }

    /**
     * Returns the fastResume file backing this torrent if any. Null or a
     * non-existent file can be returned.
     */
    public File getFastResumeFile() {
        return fastResumeFile;
    }

    /**
     * Returns a list of peers connected to this torrent.
     */
    public List<String> getPeers() {
        return torrentManager.getPeers(this);
    }

    /**
     * Moves the torrent to the specified directory.
     */
    public void moveTorrent(File directory) {
        torrentManager.moveTorrent(this, directory);
        int count = 0;
        //TODO find a better way to do this.
        while (!getCompleteFile().exists()) {
            if (count++ > 50) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Pauses the torrent.
     */
    public void pause() {
        torrentManager.pauseTorrent(this);
    }

    /**
     * Resumes the torrent from a paused state.
     */
    public void resume() {
        if (getStatus().isError()) {
            torrentManager.recoverTorrent(this);
        } 
        else { 
            torrentManager.resumeTorrent(this);
        }
    }

    /**
     * Returns the download rate in bytes/second.
     */
    public float getDownloadRate() {
        LibTorrentStatus status = this.status.get();
        return status == null ? 0 : status.download_rate;
    }

    /**
     * Returns a hexString representation of this torrents sha1.
     */
    public String getSha1() {
        return sha1;
    }

    /**
     * Returns true if this torrent is paused, false otherwise.
     */
    public boolean isPaused() {
        LibTorrentStatus status = this.status.get();
        return status == null ? false : status.isPaused();
    }

    /**
     * Returns true if this torrent is finished, false otherwise.
     */
    public boolean isFinished() {
        return complete.get();
    }

    /**
     * Returns the total size of this torrent if all files were to be
     * downloaded.
     */
    public long getTotalSize() {
        return totalSize;
    }

    /**
     * Returns true if the torrent has been started, false otherwise.
     */
    public boolean isStarted() {
        return started.get();
    }

    /**
     * Returns the first tracker url to this torrent.
     */
    public String getTrackerURL() {
        return trackerURL;
    }

    /**
     * Returns true if this is a multi file torrent, false otherwise.
     */
    public boolean isMultiFileTorrent() {
        return paths.size() > 0;
    }

    /**
     * Returns the total amount of the torren that has fnished downloading.
     */
    public long getTotalDownloaded() {
        LibTorrentStatus status = this.status.get();
        if (status == null) {
            return -1;
        } else {
            return status.getTotalDone();
        }
    }

    /**
     * Returns the number of peers in this torrents swarm.
     */
    public int getNumPeers() {
        LibTorrentStatus status = this.status.get();
        return status == null ? 0 : status.num_peers;
    }

    /**
     * Returns the non absolute paths to all files in the torrent.
     */
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

    /**
     * Returns a list of where all files in the torrent where be when completed.
     */
    public List<File> getCompleteFiles() {
        return collectFiles(getCompleteFile());
    }

    /**
     * Returns a list of where all files in the torrent where be when
     * incomplete.
     */
    public List<File> getIncompleteFiles() {
        return collectFiles(getIncompleteFile());
    }

    /**
     * Returns the root incompelteFile for this torrent.
     */
    public File getIncompleteFile() {
        return incompleteFile;
    }

    /**
     * Returns the root compelete file for this torrent.
     */
    public File getCompleteFile() {
        return completeFile;
    }

    /**
     * Returns true if this is a single file torrent, false otherwise.
     */
    public boolean isSingleFileTorrent() {
        return !isMultiFileTorrent();
    }

    /**
     * Stops the torrent by removing it from the torrent manager.
     */
    public void stop() {
        if (!cancelled.getAndSet(true)) {
            if (started.getAndSet(false)) {
                torrentManager.removeTorrent(this);
            }
            listeners.broadcast(TorrentEvent.STOPPED);
        }
    }

    /**
     * Returns the total number of byte uploaded for this torrent.
     */
    public long getTotalUploaded() {
        LibTorrentStatus status = this.status.get();
        if (status == null) {
            return 0;
        } else {
            return status.getTotalUpload();
        }
    }

    /**
     * Returns current number of upload connections.
     */
    public int getNumUploads() {
        LibTorrentStatus status = this.status.get();
        if (status == null) {
            return 0;
        } else {
            return status.num_uploads;
        }
    }

    /**
     * Returns the current upload rate in bytes/second.
     */
    public float getUploadRate() {
        LibTorrentStatus status = this.status.get();
        return status == null ? 0 : status.upload_rate;
    }

    /**
     * Returns the current seed ratio, with 1.0 being at 100%.
     */
    public float getSeedRatio() {
        LibTorrentStatus status = this.status.get();
        if (status != null && status.getTotalDownload() != 0) {
            return (status.getTotalUpload() / status.getTotalDownload());
        }
        return 0;
    }

    /**
     * Returns true if this torrent has been canceled, false otherwise.
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Returns a status object representing this torrents internal state.
     */
    public LibTorrentStatus getStatus() {
        return status.get();
    }

    /**
     * Updates this torrents internal state using the given LibTorrentStatus
     * object.
     */
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

    /**
     * Updates this torrents internal state using the given LibTorrentAlerts.
     */
    public void alert(LibTorrentAlert alert) {
        synchronized (Torrent.this) {
            if (alert.category == LibTorrentAlert.SAVE_RESUME_DATA_ALERT && alert.data != null) {
                fastResumeFile = new File(alert.data);
            }
        }
    }
}
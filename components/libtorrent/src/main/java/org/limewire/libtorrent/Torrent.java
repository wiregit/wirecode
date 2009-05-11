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
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

import com.google.inject.Inject;

public class Torrent {
    private final EventListenerList<TorrentEvent> listeners;

    private final TorrentManager torrentManager;

    private final AtomicReference<LibTorrentStatus> status;

    private File incompleteFile = null;

    private File completeFile;

    private File torrentFile = null;

    private List<String> paths;

    private String sha1 = null;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private String name;

    private String trackerURL;

    private long totalSize = -1;

    private LibTorrentInfo info = null;

    private String fastResumePath = null;

    private boolean cancelled = false;

    private boolean complete = false;

    @Inject
    public Torrent(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
        this.listeners = new EventListenerList<TorrentEvent>();
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
            List<String> paths, File saveLocation, String fastResumeData) {
        this.name = name;
        File torrentDownloadFolder = torrentManager.getTorrentDownloadFolder();
        this.incompleteFile = new File(torrentDownloadFolder, name);
        this.completeFile = saveLocation;
        this.sha1 = sha1;
        this.trackerURL = trackerURL;
        this.paths.addAll(paths);
        this.totalSize = totalSize;
        this.fastResumePath = fastResumeData;
    }

    public synchronized void init(File torrentFile, File saveDir) throws IOException {
        this.torrentFile = torrentFile;
        FileInputStream fis = null;
        FileChannel fileChannel = null;
        try {
            fis = new FileInputStream(torrentFile);
            fileChannel = fis.getChannel();
            Map metaInfo = (Map) Token.parse(fileChannel);
            BTData btData = new BTDataImpl(metaInfo);
            name = btData.getName();

            File torrentDownloadFolder = torrentManager.getTorrentDownloadFolder();

            incompleteFile = new File(torrentDownloadFolder, name);
            completeFile = new File(saveDir, name);

            if (btData.getFiles() != null) {
                for (BTFileData fileData : btData.getFiles()) {
                    paths.add(fileData.getPath());
                }
            }

            trackerURL = btData.getAnnounce();
            System.out.println("announce (tracker URL): " + trackerURL);

            sha1 = TorrentSHA1ConversionUtils.toHexString(btData.getInfoHash());

        } finally {
            IOUtils.close(fileChannel);
            IOUtils.close(fis);
        }
    }

    public String getName() {
        return name;
    }

    public void start() {
        if (!started.getAndSet(true)) {
            // TODO clean up this logic for picking which addTorrent method to
            // use
            if (torrentFile != null) {
                info = torrentManager.addTorrent(torrentFile);
            } else {
                torrentManager.addTorrent(sha1, trackerURL, fastResumePath);
            }

            torrentManager.addStatusListener(sha1, new EventListener<LibTorrentStatusEvent>() {
                public void handleEvent(LibTorrentStatusEvent event) {
                    LibTorrentStatus status = event.getTorrentStatus();
                    updateStatus(status);
                }

                private synchronized void updateStatus(LibTorrentStatus status) {
                    Torrent.this.status.set(status);
                    boolean newlyfinished = complete != status.finished && status.finished;
                    complete = status.finished;

                    if (newlyfinished) {
                        listeners.broadcast(TorrentEvent.COMPLETED);
                    } else {
                        listeners.broadcast(TorrentEvent.STATUS_CHANGED);
                    }

                }
            });

            // Add the listener for collecting fast resume data
            torrentManager.addAlertListener(sha1, new EventListener<LibTorrentAlertEvent>() {
                @Override
                public void handleEvent(LibTorrentAlertEvent event) {

                    if (event.getAlert().category == LibTorrentAlert.SAVE_RESUME_DATA_ALERT
                            && event.getAlert().data != null) {
                        fastResumePath = event.getAlert().data;

                        // System.out.println(fastResumePath);

                    }
                }
            });
        }
    }

    public List<String> getPeers() {
        return torrentManager.getPeers(getSha1());
    }

    public boolean moveTorrent(File directory) {
        return torrentManager.moveTorrent(getSha1(), directory);
    }

    public void pause() {
        torrentManager.pauseTorrent(getSha1());
    }

    public void resume() {
        torrentManager.resumeTorrent(getSha1());
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
        return status == null ? false : status.paused;
    }

    public boolean isFinished() {
        return complete;
    }

    public long getTotalSize() {
        if (info != null)
            return info.getContentLength();

        return totalSize;
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

    public List<File> getCompleteFiles() {
        List<File> files = new ArrayList<File>();
        File completeFile = getCompleteFile();
        if (paths.size() > 0) {
            for (String path : paths) {
                File file = new File(completeFile, path);
                if (file.exists()) {
                    files.add(file);
                }
            }
        } else {
            files.add(completeFile);
        }
        return files;
    }

    public List<File> getIncompleteFiles() {
        List<File> files = new ArrayList<File>();
        File incompleteFile = getIncompleteFile();
        if (paths.size() > 0) {
            for (String path : paths) {
                File file = new File(incompleteFile, path);
                if (!file.exists()) {
                    files.add(file);
                }
            }
        } else {
            files.add(incompleteFile);
        }
        return files;
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
        if (!cancelled) {
            if (started.getAndSet(false)) {
                torrentManager.removeTorrent(sha1);
            }
            cancelled = true;
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
        return cancelled;
    }

    public LibTorrentStatus getStatus() {
        return status.get();
    }

    public String getFastResumePath() {
        return fastResumePath;
    }
}
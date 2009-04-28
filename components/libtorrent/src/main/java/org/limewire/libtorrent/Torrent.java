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

public class Torrent {
    private final EventListenerList<TorrentEvent> listeners;

    private final TorrentManager torrentManager;

    private final AtomicReference<LibTorrentStatus> status;

    private LibTorrentInfo info = null;

    private File incompleteFile = null;

    private File completeFile;

    private File torrentFile = null;

    private List<String> paths;

    private String sha1 = null;

    private BTData btData = null;

    private final AtomicBoolean started = new AtomicBoolean(false);

    private String name;

    private String announce;

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

    public synchronized void init(String name, String sha1, String announce, File saveDir) {
        File torrentDownloadFolder = torrentManager.getTorrentDownloadFolder();
        incompleteFile = new File(torrentDownloadFolder, name);
        completeFile = new File(saveDir, name);
        this.sha1 = sha1;
        this.announce = announce;
        // TODO where to get paths to files?
    }

    public synchronized void init(File torrentFile, File saveDir) throws IOException {
        this.torrentFile = torrentFile;
        FileInputStream fis = null;
        FileChannel fileChannel = null;
        try {
            fis = new FileInputStream(torrentFile);
            fileChannel = fis.getChannel();
            Map metaInfo = (Map) Token.parse(fileChannel);
            btData = new BTDataImpl(metaInfo);
            name = btData.getName();

            File torrentDownloadFolder = torrentManager.getTorrentDownloadFolder();
            incompleteFile = new File(torrentDownloadFolder, name);
            completeFile = new File(saveDir, name);

            if (btData.getFiles() != null) {
                for (BTFileData fileData : btData.getFiles()) {
                    paths.add(fileData.getPath());
                }
            }

            System.out.println("announce: " + btData.getAnnounce());

            String hexString = toHexString(btData.getInfoHash());
            sha1 = hexString;

        } finally {
            IOUtils.close(fileChannel);
            IOUtils.close(fis);
        }
    }

    public byte[] getInfoHash() {
        return btData.getInfoHash();
    }

    public String getName() {
        return name;
    }

    private String toHexString(byte[] block) {
        StringBuffer hexString = new StringBuffer(block.length * 2);
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
                'E', 'F' };
        int high = 0;
        int low = 0;
        for (int i = 0; i < block.length; i++) {
            high = ((block[i] & 0xf0) >> 4);
            low = (block[i] & 0x0f);
            hexString.append(hexChars[high]);
            hexString.append(hexChars[low]);
        }
        return hexString.toString().toLowerCase();
    }

    public void start() {
        if (!started.getAndSet(true)) {
            LibTorrentInfo info = null;
            
            //TODO clean up this logic for picking which addTorrent method to use
            if (torrentFile != null) {
                info = torrentManager.addTorrent(torrentFile);
            } else {
                info = torrentManager.addTorrent(sha1, announce);
            }

            this.info = info;

            //assert sha1.equals(info.sha1);

            torrentManager.addListener(sha1, new EventListener<LibTorrentEvent>() {
                public void handleEvent(LibTorrentEvent event) {
                    LibTorrentStatus status = event.getTorrentStatus();
                    updateStatus(status);
                }

                private synchronized void updateStatus(LibTorrentStatus status) {
                    Torrent.this.status.set(status);
                    listeners.broadcast(new TorrentEvent());
                }
            });
        }
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

    public void cancel() {
        torrentManager.removeTorrent(getSha1());
    }

    public String getSha1() {
        return info == null ? null : info.sha1;
    }

    public boolean isPaused() {
        LibTorrentStatus status = this.status.get();
        return status == null ? false : status.paused;
    }

    public boolean isFinished() {
        LibTorrentStatus status = this.status.get();
        return status == null ? false : status.finished;
    }

    public long getTotalSize() {
        if (info == null) {
            return -1;
        } else {
            return info.getContentLength();
        }
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

    public int getPieceLength() {
        return info == null ? -1 : info.piece_length;
    }

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
        if (started.getAndSet(false)) {
            torrentManager.removeTorrent(sha1);
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

    public LibTorrentStatus getStatus() {
        return status.get();
    }
}
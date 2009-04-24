/**
 * 
 */
package org.limewire.libtorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.settings.SharingSettings;
import org.limewire.io.IOUtils;
import org.limewire.listener.EventListener;

import com.limegroup.bittorrent.BTData;
import com.limegroup.bittorrent.BTDataImpl;
import com.limegroup.bittorrent.BTData.BTFileData;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.Downloader.DownloadState;

public class Torrent {

    private final TorrentManager torrentManager;

    private final AtomicReference<LibTorrentStatus> status;

    private LibTorrentInfo info = null;

    private File incompleteFile = null;

    private File completeFile;

    private List<String> paths;

    public Torrent(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
        this.status = new AtomicReference<LibTorrentStatus>();
        this.paths = new ArrayList<String>();
    }

    public synchronized void init(File torrentFile) throws IOException {
        FileInputStream fis = null;
        FileChannel fileChannel = null;
        try {
            fis = new FileInputStream(torrentFile);
            fileChannel = fis.getChannel();
            Map metaInfo = (Map) Token.parse(fileChannel);
            BTData btData = new BTDataImpl(metaInfo);
            String name = btData.getName();

            // TODO pull this from somewhere
            File torrentDownloadFolder = torrentManager.getTorrentDownloadFolder();
            incompleteFile = new File(torrentDownloadFolder, name);
            completeFile = new File(SharingSettings.getSaveDirectory(name), name);

            if (btData.getFiles() != null) {
                for (BTFileData fileData : btData.getFiles()) {
                    paths.add(fileData.getPath());
                }
            }
        } finally {
            IOUtils.close(fileChannel);
            IOUtils.close(fis);
        }

        // TODO not the right place for this really

        LibTorrentInfo info = torrentManager.addTorrent(torrentFile);
        setInfo(info);
        torrentManager.pauseTorrent(info.sha1);
    }

    public void start() {
        // TODO make a real start method.
        torrentManager.resumeTorrent(info.sha1);
        torrentManager.addListener(info.sha1, new EventListener<LibTorrentEvent>() {
            public void handleEvent(LibTorrentEvent event) {
                LibTorrentStatus status = event.getTorrentStatus();
                setStatus(status);
            }
        });
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

    public DownloadState getState() {
        LibTorrentStatus status = this.status.get();
        if (status == null) {
            return DownloadState.QUEUED;
        }
        LibTorrentState state = LibTorrentState.forId(status.state);
        return convertState(state);
    }

    public boolean isFinished() {
        LibTorrentStatus status = this.status.get();
        return status == null ? false : status.finished;
    }

    public long getTotalSize() {
        BigInteger size = new BigInteger(info.content_length);

        return size.longValue();
    }

    public long getTotalDownloaded() {
        LibTorrentStatus status = this.status.get();
        if (status == null) {
            return 0;
        } else {
            BigInteger done = new BigInteger(status.total_done);
            return done.longValue();
        }
    }

    public int getNumPeers() {
        LibTorrentStatus status = this.status.get();
        return status == null ? 0 : status.num_peers;
    }

    public int getPieceLength() {
        return info == null ? -1 : info.piece_length;
    }

    public synchronized void setInfo(LibTorrentInfo info) {
        this.info = info;
    }

    public synchronized void setStatus(LibTorrentStatus status) {
        this.status.set(status);
    }

    private DownloadState convertState(LibTorrentState state) {
        //TODO support error states
        
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
}
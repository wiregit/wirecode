package org.limewire.libtorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.core.settings.SharingSettings;
import org.limewire.io.IOUtils;
import org.limewire.listener.EventListener;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;

import com.limegroup.bittorrent.BTData;
import com.limegroup.bittorrent.BTDataImpl;
import com.limegroup.bittorrent.BTData.BTFileData;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadState;

public class Torrent {

    private final TorrentManager torrentManager;

    private final AtomicReference<LibTorrentStatus> status;

    private LibTorrentInfo info = null;

    private File incompleteFile = null;

    private File completeFile;

    private File torrentFile = null;

    private List<String> paths;

    private final AtomicBoolean complete = new AtomicBoolean(false);

    private String sha1 = null;

    private URN urn = null;

    public Torrent(TorrentManager torrentManager) {
        this.torrentManager = torrentManager;
        this.status = new AtomicReference<LibTorrentStatus>();
        this.paths = new ArrayList<String>();
    }

    public synchronized void init(File torrentFile) throws IOException {
        this.torrentFile = torrentFile;
        FileInputStream fis = null;
        FileChannel fileChannel = null;
        try {
            fis = new FileInputStream(torrentFile);
            fileChannel = fis.getChannel();
            Map metaInfo = (Map) Token.parse(fileChannel);
            BTData btData = new BTDataImpl(metaInfo);
            String name = btData.getName();

            File torrentDownloadFolder = torrentManager.getTorrentDownloadFolder();
            incompleteFile = new File(torrentDownloadFolder, name);
            completeFile = new File(SharingSettings.getSaveDirectory(name), name);

            if (btData.getFiles() != null) {
                for (BTFileData fileData : btData.getFiles()) {
                    paths.add(fileData.getPath());
                }
            }

            urn = URN.createSHA1UrnFromBytes(btData.getInfoHash());

            String hexString = toHexString(btData.getInfoHash());
            sha1 = hexString;

        } finally {
            IOUtils.close(fileChannel);
            IOUtils.close(fis);
        }
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

    public synchronized void start() {
        LibTorrentInfo info = torrentManager.addTorrent(torrentFile);
        setInfo(info);

        assert sha1.equals(info.sha1);

        torrentManager.addListener(sha1, new EventListener<LibTorrentEvent>() {
            public void handleEvent(LibTorrentEvent event) {
                LibTorrentStatus status = event.getTorrentStatus();
                updateStatus(status);
            }
        });
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

    public synchronized void setInfo(LibTorrentInfo info) {
        this.info = info;
    }

    private synchronized void updateStatus(LibTorrentStatus status) {
        this.status.set(status);

        if (status.finished && !complete.getAndSet(status.finished)) {
            File completeDir = getCompleteFile().getParentFile();
            torrentManager.moveTorrent(getSha1(), completeDir);
        }
    }

    private DownloadState convertState(LibTorrentState state) {
        // TODO support error states

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
package org.limewire.libtorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.TorrentParams;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.io.IOUtils;
import org.limewire.util.StringUtils;

public class LibTorrentParams implements TorrentParams {

    private final File downloadFolder;

    private String name = null;;

    private String sha1 = null;

    private String trackerURL = null;

    private File fastResumeFile = null;

    private File torrentFile = null;

    private File torrentDataFile = null;

    private Boolean isPrivate = null;

    private final AtomicBoolean filled = new AtomicBoolean(false);

    /**
     * Creates the torrent params using the required field download folder and a
     * torrent file. After fill is called, the name and sha1 must be filled in.
     */
    public LibTorrentParams(File downloadFolder, File torrentFile) {
        this.downloadFolder = downloadFolder;
        this.torrentFile = torrentFile;
    }

    /**
     * Created the torrent params using the current three required fields of
     * name downloadfolder and sha1.
     */
    public LibTorrentParams(File downloadFolder, String name, String sha1) {
        this.downloadFolder = downloadFolder;
        // TODO make the name no longer a required field.
        this.name = name;
        this.sha1 = sha1;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getSha1() {
        return sha1;
    }

    @Override
    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    @Override
    public String getTrackerURL() {
        return trackerURL;
    }

    @Override
    public void setTrackerURL(String trackerURL) {
        this.trackerURL = trackerURL;
    }

    @Override
    public File getFastResumeFile() {
        return fastResumeFile;
    }

    @Override
    public void setFastResumeFile(File fastResumeFile) {
        this.fastResumeFile = fastResumeFile;
    }

    @Override
    public File getTorrentFile() {
        return torrentFile;
    }

    @Override
    public File getDownloadFolder() {
        return downloadFolder;
    }

    @Override
    public void setTorrentFile(File torrentFile) {
        filled.set(false);
        this.torrentFile = torrentFile;
    }

    @Override
    public File getTorrentDataFile() {
        return torrentDataFile;
    }

    @Override
    public void setTorrentDataFile(File torrentDataFile) {
        this.torrentDataFile = torrentDataFile;
    }

    @Override
    public Boolean getPrivate() {
        return isPrivate;
    }

    @Override
    public void setPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    @Override
    public void fill() throws IOException {
        if (!filled.getAndSet(true)) {
            String sha1 = getSha1();
            File torrentFile = getTorrentFile();
            String name = getName();
            String trackerURL = getTrackerURL();
            Boolean isPrivate = getPrivate();
            File torrentDataFile = getTorrentDataFile();
            File fastResumeFile = getFastResumeFile();

            if (torrentFile != null && torrentFile.exists()) {
                FileInputStream fis = null;
                FileChannel fileChannel = null;
                try {
                    fis = new FileInputStream(torrentFile);
                    fileChannel = fis.getChannel();
                    Map metaInfo = (Map) Token.parse(fileChannel);
                    BTData btData = new BTDataImpl(metaInfo);
                    if (name == null) {
                        name = btData.getName();
                        setName(name);
                    }

                    if (trackerURL == null) {
                        trackerURL = btData.getTrackerUris().get(0).toASCIIString();
                        setTrackerURL(trackerURL);
                    }

                    if (sha1 == null) {
                        sha1 = StringUtils.toHexString(btData.getInfoHash());
                        setSha1(sha1);
                    }

                    if (isPrivate == null) {
                        isPrivate = btData.isPrivate();
                        setPrivate(isPrivate);
                    }

                } finally {
                    IOUtils.close(fileChannel);
                    IOUtils.close(fis);
                }

            }

            File downloadFolder = getDownloadFolder();
            if (name == null || downloadFolder == null || sha1 == null) {
                // TODO eventually we need to support this without the name
                // parameter
                throw new IOException("There was an error initializing the torrent parameters.");
            }

            setFastResumeFile(fastResumeFile == null ? new File(downloadFolder, name
                    + ".fastresume") : fastResumeFile);
            setTorrentDataFile(torrentDataFile == null ? new File(downloadFolder, name)
                    : torrentDataFile);
            setTorrentFile(torrentFile == null ? new File(downloadFolder, name + ".torrent")
                    : torrentFile);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("sha1: ").append(sha1).append("\n");
        sb.append("trackerURL: ").append(trackerURL).append("\n");
        sb.append("name: ").append(name).append("\n");
        sb.append("torrentFile: ").append(torrentFile).append("\n");
        sb.append("torrentDataFile: ").append(torrentDataFile).append("\n");
        sb.append("downloadFolder: ").append(downloadFolder).append("\n");
        sb.append("isPrivate: ").append(isPrivate).append("\n");
        sb.append("fastResumeFile: ").append(fastResumeFile).append("\n");

        return sb.toString();
    }

}
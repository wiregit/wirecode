package org.limewire.bittorrent;

import java.io.File;

public class TorrentParams {

    private final File downloadFolder;
    
    private String name = null;;

    private String sha1 = null;

    private String trackerURL = null;

    private File fastResumeFile = null;

    private File torrentFile = null;

    private File torrentDataFile = null;
    
    private Boolean isPrivate = null;
    
    public TorrentParams(File downloadFolder, File torrentFile) {
        this.downloadFolder = downloadFolder;
        this.torrentFile = torrentFile;
    }
    
    public TorrentParams(File downloadFolder, String name, String sha1) {
        this.downloadFolder = downloadFolder;
        this.name = name;
        this.sha1 = sha1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getTrackerURL() {
        return trackerURL;
    }

    public void setTrackerURL(String trackerURL) {
        this.trackerURL = trackerURL;
    }

    public File getFastResumeFile() {
        return fastResumeFile;
    }

    public void setFastResumeFile(File fastResumeFile) {
        this.fastResumeFile = fastResumeFile;
    }

    public File getTorrentFile() {
        return torrentFile;
    }
    
    public File getDownloadFolder() {
        return downloadFolder;
    }

    public void setTorrentFile(File torrentFile) {
        this.torrentFile = torrentFile;
    }

    public File getTorrentDataFile() {
        return torrentDataFile;
    }

    public void setTorrentDataFile(File torrentDataFile) {
        this.torrentDataFile = torrentDataFile;
    }

    public Boolean getPrivate() {
        return isPrivate;
    }

    public void setPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}

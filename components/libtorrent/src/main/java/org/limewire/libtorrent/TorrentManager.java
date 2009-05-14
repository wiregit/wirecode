package org.limewire.libtorrent;

import java.io.File;
import java.util.List;

import org.limewire.lifecycle.Service;
import org.limewire.listener.EventListener;

public interface TorrentManager extends Service {

    public void addStatusListener(String id, EventListener<LibTorrentStatusEvent> listener);
    
    public void addAlertListener(String id, EventListener<LibTorrentAlertEvent> listener);

    public void addTorrent(String sha1, File torrent, File fastResumeFile);
    
    public void removeTorrent(String id);

    public void pauseTorrent(String id);

    public void resumeTorrent(String id);

    public LibTorrentStatus getStatus(String id);

    public List<String> getPeers(String id);
    
    public File getTorrentDownloadFolder();

    public void moveTorrent(String id, File directory);

    public int getNumActiveTorrents();

    public void addTorrent(String sha1, String trackerURI, File fastResumeData);

    public void free(LibTorrentStatus oldStatus);
}
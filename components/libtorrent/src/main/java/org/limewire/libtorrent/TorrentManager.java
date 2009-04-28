package org.limewire.libtorrent;

import java.io.File;

import org.limewire.lifecycle.Service;
import org.limewire.listener.EventListener;

public interface TorrentManager extends Service {

    public abstract void addListener(String id, EventListener<LibTorrentEvent> listener);

    public abstract LibTorrentInfo addTorrent(File torrent);
    
    public abstract void removeTorrent(String id);

    public abstract void pauseTorrent(String id);

    public abstract void resumeTorrent(String id);

    public abstract LibTorrentStatus getStatus(String id);

    public abstract File getTorrentDownloadFolder();

    public abstract boolean moveTorrent(String id, File directory);

    public abstract int getNumActiveTorrents();

    public abstract LibTorrentInfo addTorrent(String sha1, String trackerURI);
}
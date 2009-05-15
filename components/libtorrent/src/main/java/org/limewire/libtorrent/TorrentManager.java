package org.limewire.libtorrent;

import java.io.File;
import java.util.List;

import org.limewire.lifecycle.Service;

public interface TorrentManager extends Service {

    public void removeTorrent(String id);

    public void pauseTorrent(String id);

    public void resumeTorrent(String id);

    public List<String> getPeers(String id);

    public File getTorrentDownloadFolder();

    public void moveTorrent(String id, File directory);

    public int getNumActiveTorrents();

    public boolean isDownloading(File torrentFile);

    void registerTorrent(Torrent torrent);
}
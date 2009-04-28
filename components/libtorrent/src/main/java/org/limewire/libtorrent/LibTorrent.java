package org.limewire.libtorrent;

import org.limewire.libtorrent.callback.AlertCallback;

import com.sun.jna.Library;
import com.sun.jna.Memory;

public interface LibTorrent extends Library {

    public void initialize();
    
    public void init(String path);

    public LibTorrentInfo add_torrent(String path);
    
    public LibTorrentInfo add_torrent_old(String sha1, String trackerURI);
    
    public int pause_torrent(String id);

    public int resume_torrent(String id);

    public void get_alerts(AlertCallback alertCallback);

    public LibTorrentStatus get_torrent_status(String id);

    public LibTorrentStatus get_torrent_status(String id, Memory memory);

    public int remove_torrent(String id);

    public void print();

    public boolean move_torrent(String id, String absolutePath);

    public void abort_torrents();
}

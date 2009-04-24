package org.limewire.libtorrent;

import org.limewire.libtorrent.callback.AlertCallback;

import com.sun.jna.Library;
import com.sun.jna.Memory;

public interface LibTorrent extends Library {

    public void init(String path);

    public LibTorrentInfo add_torrent(String path);

    public int pause_torrent(String id);

    public int resume_torrent(String id);

    public boolean is_torrent_paused(String id);

    public boolean is_torrent_seed(String id);

    public boolean is_torrent_finished(String id);

    public boolean is_torrent_valid(String id);

    public void get_alerts(AlertCallback alertCallback);

    public LibTorrentStatus get_torrent_status(String id);

    public LibTorrentStatus get_torrent_status(String id, Memory memory);

    public int remove_torrent(String id);

    public void print();

    public boolean move_torrent(String id, String absolutePath);
}

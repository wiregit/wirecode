package org.limewire.libtorrent;

import org.limewire.libtorrent.callback.AlertCallback;

import com.sun.jna.Library;
import com.sun.jna.Memory;

public interface LibTorrent extends Library {

    public void initialize();
    
    public void init(String path);

    public LibTorrentInfo add_torrent(String path);
    
    public void add_torrent_existing(String sha1, String trackerURI, String fastResumeData);
    
    public int pause_torrent(String id);

    public int resume_torrent(String id);

    public void get_alerts(AlertCallback alertCallback);

    public LibTorrentStatus get_torrent_status(String id);

    public LibTorrentStatus get_torrent_status(String id, Memory memory);

    public int get_num_peers(String id);
    
    public void get_peers(String id, Memory memory);
    
    public boolean signal_fast_resume_data_request(String id);
    
    public int remove_torrent(String id);

    public void print();

    public boolean move_torrent(String id, String absolutePath);

    public void abort_torrents();
    
}
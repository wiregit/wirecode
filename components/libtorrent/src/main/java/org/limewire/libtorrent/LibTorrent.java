package org.limewire.libtorrent;

import org.limewire.libtorrent.callback.AlertCallback;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

interface LibTorrent extends Library {

    public void initialize();
    
    public void init(String path);

    public void add_torrent(LibTorrentInfo info, String path, 
            LongHeap longHeap, Sha1Heap sha1Heap, Pointer ptr);
    
    public void add_torrent_existing(String sha1, String trackerURI, String fastResumeData);
    
    public int pause_torrent(String id);

    public int resume_torrent(String id);

    public void get_alerts(AlertCallback alertCallback);

    public void get_torrent_status(String id, LibTorrentStatus status,
            LongHeap longHeap1, LongHeap longHeap2, LongHeap longHeap3);

    public int get_num_peers(String id);
    
    public void get_peers(String id, Memory memory);
    
    public boolean signal_fast_resume_data_request(String id);
    
    public int remove_torrent(String id);

    public boolean move_torrent(String id, String absolutePath);

    public void abort_torrents();
    
}

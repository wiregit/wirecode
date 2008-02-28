package com.limegroup.bittorrent;

public interface TorrentDHTManager extends TorrentEventListener {    
    
    public void handleTorrentEvent(TorrentEvent evt);
    
    public void init ();
}
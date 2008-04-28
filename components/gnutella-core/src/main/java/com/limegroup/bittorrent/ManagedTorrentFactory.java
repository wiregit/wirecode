package com.limegroup.bittorrent;

public interface ManagedTorrentFactory {

    /**
     * Creates an instance of ManagedTorrent for a .torrent file based on the
     * torrent context
     * 
     * @param context
     * @return a new instance of ManagedTorrent
     */
    public ManagedTorrent createFromContext(TorrentContext context);

}
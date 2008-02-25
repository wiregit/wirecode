package com.limegroup.bittorrent.dht;

import com.limegroup.bittorrent.ManagedTorrent;

public interface DHTPeerLocator {
    /**
     * Searches for peers sharing the given torrent file
     */
    public void startSearching(ManagedTorrent managedTorrent);

}

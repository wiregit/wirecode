package com.limegroup.bittorrent.dht;

import com.limegroup.bittorrent.ManagedTorrent;

/**
 * DHTPeerLocator Interface defines a method for looking up a peer in DHT given
 * the managedTorrent instance of the torrent
 */

public interface DHTPeerLocator {

    /**
     * Initialization method
     */
    public void init();

    /**
     * Searches for peers sharing the given torrent.
     * 
     * @param managedTorrent a managedTorrent instance of the torrent.
     */
    public void locatePeer(ManagedTorrent managedTorrent);

}

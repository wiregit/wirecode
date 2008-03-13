package com.limegroup.bittorrent.dht;

import com.limegroup.bittorrent.ManagedTorrent;

/**
 * <code>DHTPeerLocator</code> defines an interface for looking up a peer in
 * DHT given the <code>ManagedTorrent</code> instance of the torrent.
 */
public interface DHTPeerLocator {

    /**
     * Registers the listeners used.
     */
    public void init();

    /**
     * Searches for peers sharing the given torrent. If no peers are found, then
     * the search stop.
     * 
     * @param managedTorrent a <code>ManagedTorrent</code> instance of the
     *        torrent.
     */
    public void locatePeer(ManagedTorrent managedTorrent);

}

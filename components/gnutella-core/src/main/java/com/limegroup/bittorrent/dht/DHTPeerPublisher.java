package com.limegroup.bittorrent.dht;

import com.limegroup.bittorrent.ManagedTorrent;

/**
 * <code>DHTPeerPublisher</code> defines an interface for storing network information in DHT
 * for a torrent file.
 */
public interface DHTPeerPublisher {

    /**
     * Register the listeners used.
     */
    public void init();

    /**
     * Stores the network information of the local host as a seeder in DHT for
     * the given torrent.
     * 
     * @param managedTorrent the torrent the peer is sharing.
     */
    public void publishYourself(ManagedTorrent managedTorrent);

}

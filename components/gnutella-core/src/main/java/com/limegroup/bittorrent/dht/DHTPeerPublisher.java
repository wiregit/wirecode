package com.limegroup.bittorrent.dht;

import com.limegroup.bittorrent.ManagedTorrent;

/**
 * DHTPeerPublisher Interface defines a method for publishing yourself as a host
 * of a torrent in DHT.
 */
public interface DHTPeerPublisher {

    public void init();

    /**
     * Stores the network information of the local host as a seeder in DHT for
     * the given torrent.
     * 
     * @param managedTorrent the torrent the peer is sharing.
     */
    public void publishYourself(ManagedTorrent managedTorrent);

}

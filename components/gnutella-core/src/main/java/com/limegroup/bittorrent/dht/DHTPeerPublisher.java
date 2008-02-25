package com.limegroup.bittorrent.dht;

import com.limegroup.bittorrent.ManagedTorrent;

public interface DHTPeerPublisher {

    /**
     * Stores the network information of the peer as a location the given
     * torrent could be downloaded from
     * 
     * @param managedTorrent the torrent the peer is sharing
     * 
     */
    public void publishYourself(ManagedTorrent managedTorrent);

}

package com.limegroup.bittorrent.dht;

import com.limegroup.gnutella.URN;

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
     * @param urn SHA1 hash of the torrent file.
     */
    public void publishYourself(URN urn);

}

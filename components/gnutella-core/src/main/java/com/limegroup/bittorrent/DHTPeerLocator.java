package com.limegroup.bittorrent;



public interface DHTPeerLocator {

    /**
     * Stores the local node in the DHT
     * 
     */
    public void publishYourSelf();

    /**
     * Searches for peers sharing the given torrent file
     */
    public void startSearching();

}

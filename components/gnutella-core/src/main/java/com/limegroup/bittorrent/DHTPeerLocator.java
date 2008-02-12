package com.limegroup.bittorrent;

import org.limewire.nio.observer.Shutdownable;


public interface DHTPeerLocator {

    public void publish(TorrentLocation torLoc);

    public Shutdownable startSearching();

}

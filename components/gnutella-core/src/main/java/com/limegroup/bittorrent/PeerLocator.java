package com.limegroup.bittorrent;

import org.limewire.nio.observer.Shutdownable;


public interface PeerLocator {
        
    public void publish();
        
    public Shutdownable startSearching();
    

}

package com.limegroup.bittorrent;

import org.limewire.nio.observer.Shutdownable;


public interface PeerLocator {
    

    public enum PeerLocatorState {
        READY, SEARCHING, SUCCESS, FAILURE, CANCLED; 
    }

    
    public PeerLocatorState getState();
    
    public void publish();
        
    public Shutdownable startSearching();
    

}

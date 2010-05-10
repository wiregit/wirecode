package com.limegroup.gnutella.dht2;

import java.util.EventListener;

/**
 * An interface to listen for {@link DHTEvent}s.
 */
public interface DHTEventListener extends EventListener {
    
    /**
     * Called for {@link DHTEvent}s.
     */
    public void handleDHTEvent(DHTEvent evt);
}

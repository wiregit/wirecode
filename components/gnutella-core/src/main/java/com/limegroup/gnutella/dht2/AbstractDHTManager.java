package com.limegroup.gnutella.dht2;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.mojito2.util.EventUtils;
import org.limewire.util.Objects;

/**
 * 
 */
abstract class AbstractDHTManager implements DHTManager {

    /**
     * List of event listeners for ConnectionLifeCycleEvents.
     */
    private final List<DHTEventListener> listeners 
        = new CopyOnWriteArrayList<DHTEventListener>();
    
    @Override
    public void addEventListener(DHTEventListener listener) {
        listeners.add(Objects.nonNull(listener, "listener"));
    }
    
    @Override
    public void removeEventListener(DHTEventListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 
     */
    void dispatchEvent(final DHTEvent evt) {
        if (!listeners.isEmpty()) {
            Runnable event = new Runnable() {
                @Override
                public void run() {
                    for (DHTEventListener l : listeners) {
                        l.handleDHTEvent(evt);
                    }
                }
            };
            
            EventUtils.fireEvent(event);
        }
    }
}

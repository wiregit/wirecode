package com.limegroup.gnutella.dht;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.mojito.util.EventUtils;
import org.limewire.util.Objects;

import com.limegroup.gnutella.dht.DHTEvent.Type;

/**
 * An abstract implementation of {@link DHTManager}
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
     * Fires a start event
     */
    protected void fireStarting() {
        dispatchEvent(new DHTEvent(Type.STARTING, this));
    }
    
    /**
     * Fires a stopped event
     */
    protected void fireStopped() {
        dispatchEvent(new DHTEvent(Type.STOPPED, this));
    }
    
    /**
     * Fires a connecting event
     */
    protected void fireConnecting() {
        dispatchEvent(new DHTEvent(Type.CONNECTING, this));
    }
    
    /**
     * Fires a connected event
     */
    protected void fireConnected() {
        dispatchEvent(new DHTEvent(Type.CONNECTED, this));
    }
    
    @Override
    public void dispatchEvent(final DHTEvent evt) {
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

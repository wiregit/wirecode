package org.limewire.swarm;

import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.collection.Range;

public class SwarmListenerList {

    private final SwarmCoordinator swarmCoordinator;

    private final CopyOnWriteArrayList<SwarmListener> listeners = new CopyOnWriteArrayList<SwarmListener>();

    public SwarmListenerList(SwarmCoordinator swarmCoordinator) {
        assert swarmCoordinator != null;
        this.swarmCoordinator = swarmCoordinator;
    }

    public void downloadCompleted(SwarmFileSystem swarmDownload) {
        for (SwarmListener listener : listeners) {
            listener.downloadCompleted(swarmCoordinator, swarmDownload);
        }
    }
    
    public void blockLeased(Range block) {
        for (SwarmListener listener : listeners) {
            listener.blockLeased(swarmCoordinator, block);
        }
    }
    
    public void blockUnleased(Range block) {
        for (SwarmListener listener : listeners) {
            listener.blockUnleased(swarmCoordinator, block);
        }
    }
    
    public void blockPending(Range block) {
        for (SwarmListener listener : listeners) {
            listener.blockPending(swarmCoordinator, block);
        }
    }
    
    public void blockUnpending(Range block) {
        for (SwarmListener listener : listeners) {
            listener.blockUnpending(swarmCoordinator, block);
        }
    }
    
    public void blockWritten(Range block) {
        for (SwarmListener listener : listeners) {
            listener.blockWritten(swarmCoordinator, block);
        }
    }
    
    public void blockVerificationFailed(Range failedRange) {
        for (SwarmListener listener : listeners) {
            listener.blockVerificationFailed(swarmCoordinator, failedRange);
        }
    }
    
    public void blockVerified(Range block) {
        for (SwarmListener listener : listeners) {
            listener.blockVerified(swarmCoordinator, block);
        }
    }

    public void add(SwarmListener swarmListener) {
        listeners.add(swarmListener);
    }
}
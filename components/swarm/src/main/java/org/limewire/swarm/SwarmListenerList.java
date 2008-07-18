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
    
    public void verificationFailed(Range failedRange) {
        for (SwarmListener listener : listeners) {
            listener.verificationFailed(swarmCoordinator, failedRange);
        }
    }
    
    

    public void add(SwarmListener swarmListener) {
        listeners.add(swarmListener);
    }
}
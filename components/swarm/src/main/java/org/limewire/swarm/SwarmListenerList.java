package org.limewire.swarm;

import java.util.concurrent.CopyOnWriteArrayList;

public class SwarmListenerList {

    private final SwarmCoordinator swarmCoordinator;

    private final CopyOnWriteArrayList<SwarmListener> listeners = new CopyOnWriteArrayList<SwarmListener>();

    public SwarmListenerList(SwarmCoordinator swarmCoordinator) {
        assert swarmCoordinator != null;
        this.swarmCoordinator = swarmCoordinator;
    }

    public void downloadCompleted(SwarmDownload swarmDownload) {
        for (SwarmListener listener : listeners) {
            listener.fileCompleted(swarmCoordinator, swarmDownload);
        }
    }

    public void add(SwarmListener swarmListener) {
        listeners.add(swarmListener);
    }
}
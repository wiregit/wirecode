package org.limewire.swarm.impl;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmStatus;

public class EchoSwarmSourceListener implements SwarmSourceListener {

    public EchoSwarmSourceListener() {
    }

    public void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        System.out.println("connected: " + source);
    }

    public void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        System.out.println("connetionFailed: " + source);
    }

    public void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        System.out.println("connectionClosed: " + source);
    }

    public void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        System.out.println("responseProcessed: " + source + " status: " + status);
    }

    public void finished(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        System.out.println("finished: " + source);
    }

}

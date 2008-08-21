package org.limewire.swarm.impl;

import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmStatus;

public class EchoSwarmSourceListener implements SwarmSourceListener {

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#connected(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        System.out.println("connected: " + source);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#connectFailed(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        System.out.println("connetionFailed: " + source);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#connectionClosed(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        System.out.println("connectionClosed: " + source);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#responseProcessed(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource, org.limewire.swarm.SwarmStatus)
     */
    public void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        System.out.println("responseProcessed: " + source + " status: " + status);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmSourceListener#finished(org.limewire.swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void finished(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        System.out.println("finished: " + source);
    }

}

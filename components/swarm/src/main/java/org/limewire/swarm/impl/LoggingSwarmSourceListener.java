package org.limewire.swarm.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmStatus;

public class LoggingSwarmSourceListener implements SwarmSourceListener {

    private static final Log LOG = LogFactory.getLog(LoggingSwarmSourceListener.class);

    public LoggingSwarmSourceListener() {
    }

    public void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        LOG.trace("connected: " + source);
    }

    public void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        LOG.trace("connetionFailed: " + source);
    }

    public void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        LOG.trace("connectionClosed: " + source);
    }

    public void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        LOG.trace("responseProcessed: " + source + " status: " + status);
    }

    public void finished(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        LOG.trace("finished: " + source);
    }

}

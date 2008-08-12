package org.limewire.swarm.impl;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmStatus;

public class ReconnectingSwarmSourceListener implements SwarmSourceListener {

    private static final Log LOG = LogFactory.getLog(ReconnectingSwarmSourceListener.class);

    private final Map<SwarmSource, SwarmStatus> connectionStatus;

    public ReconnectingSwarmSourceListener() {
        connectionStatus = Collections.synchronizedMap(new WeakHashMap<SwarmSource, SwarmStatus>());
    }

    public void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {

    }

    public void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {

    }

    public void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        SwarmStatus status = connectionStatus.get(source);
        connectionClosed(swarmSourceHandler, source, status);
    }

    private void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        // TODO we can be smarter about reconnecting
        // check the explicit error and try reconnecting
        // or allow a few errors before letting the connection close

        if (status != null && status.isFinished() || swarmSourceHandler.isComplete()) {
            LOG.trace("finished, not reconnecting: " + source + " status: " + status);
        } else if (status != null && status.isError()) {
            LOG.trace("error, not reconnecting: " + source + " status: " + status);
        } else if (status == null || status.isOk()) {
            LOG.trace("reconnecting: " + source + " status: " + status);
            try {
                Thread.sleep(500);// wait a little before connecting
            } catch (InterruptedException e) {
                LOG.warn("sleep interrupted", e);
            }
            // re-add this source back to the swarmer
            swarmSourceHandler.addSource(source);
        }
    }

    public void responseProcessed(SwarmSourceHandler swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        connectionStatus.put(source, status);
    }

    public void finished(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
        connectionStatus.put(source, new FinishedSwarmStatus());
    }

}

package org.limewire.swarm.impl;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceDownloader;
import org.limewire.swarm.SwarmSourceListener;
import org.limewire.swarm.SwarmStatus;

public class ReconnectingSwarmSourceListener implements SwarmSourceListener {

    private static final Log LOG = LogFactory.getLog(ReconnectingSwarmSourceListener.class);

    private SwarmStatus connectionStatus = null;

    /*
     * (non-Javadoc)
     * 
     * @seeorg.limewire.swarm.SwarmSourceListener#connected(org.limewire.swarm.
     * SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connected(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        LOG.tracef("connected: {0}", source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.limewire.swarm.SwarmSourceListener#connectFailed(org.limewire.swarm
     * .SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connectFailed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        LOG.tracef("connectFailed: {0}", source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.limewire.swarm.SwarmSourceListener#connectionClosed(org.limewire.
     * swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void connectionClosed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        connectionClosed(swarmSourceHandler, source, connectionStatus);
    }

    private void connectionClosed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        // TODO we can be smarter about reconnecting
        // check the explicit error and try reconnecting
        // or allow a few errors before letting the connection close

        if (status != null && status.isFinished() || swarmSourceHandler.isComplete()
                || source.isFinished()) {
            LOG.tracef("finished, not reconnecting: {0} status: {1}", source, status);
        } else if (status != null && status.isError()) {
            LOG.tracef("error, not reconnecting: {0} status: {1}", source, status);
        } else if (status == null || status.isOk()) {
            LOG.tracef("reconnecting: {0} status: {1}", source, status);
            try {
                Thread.sleep(500);// wait a little before connecting
            } catch (InterruptedException e) {
                LOG.warn("sleep interrupted", e);
            }
            // re-add this source back to the swarmer
            swarmSourceHandler.addSource(source);
        } else {
            LOG.tracef("connectionClosed: {0} status: {1}", source, status);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.limewire.swarm.SwarmSourceListener#responseProcessed(org.limewire
     * .swarm.SwarmSourceHandler, org.limewire.swarm.SwarmSource,
     * org.limewire.swarm.SwarmStatus)
     */
    public void responseProcessed(SwarmSourceDownloader swarmSourceHandler, SwarmSource source,
            SwarmStatus status) {
        connectionStatus = status;
        LOG.tracef("responseProcessed: {0} status: {1}", source, status);
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.limewire.swarm.SwarmSourceListener#finished(org.limewire.swarm.
     * SwarmSourceHandler, org.limewire.swarm.SwarmSource)
     */
    public void finished(SwarmSourceDownloader swarmSourceHandler, SwarmSource source) {
        connectionStatus = new FinishedSwarmStatus();
        LOG.tracef("finished: {0}", source);
    }

}

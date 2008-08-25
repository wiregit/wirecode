package org.limewire.swarm.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceDownloader;
import org.limewire.swarm.SwarmSourceType;
import org.limewire.swarm.Swarmer;
import org.limewire.util.Objects;

public class SwarmerImpl implements Swarmer {

    private static final Log LOG = LogFactory.getLog(SwarmerImpl.class);

    private final Map<SwarmSourceType, SwarmSourceDownloader> sourceHandlers;

    private final SwarmCoordinator swarmCoordinator;

    public SwarmerImpl(SwarmCoordinator swarmCoordinator) {
        this.swarmCoordinator = Objects.nonNull(swarmCoordinator, "swarmCoordinator");
        this.sourceHandlers = Collections
                .synchronizedMap(new HashMap<SwarmSourceType, SwarmSourceDownloader>());
    }

    public SwarmSourceDownloader getSwarmSourceHandler(Class<SwarmSource> clazz) {
        return sourceHandlers.get(clazz);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.Swarmer#register(org.limewire.swarm.SwarmSourceType, org.limewire.swarm.SwarmSourceHandler)
     */
    public void register(SwarmSourceType type, SwarmSourceDownloader sourceHandler) {
        sourceHandlers.put(type, sourceHandler);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.Swarmer#addSource(org.limewire.swarm.SwarmSource)
     */
    public void addSource(SwarmSource source) {
        SwarmSourceType type = source.getType();

        if (!hasHandler(type)) {
            throw new IllegalStateException("No swarm source handler is registered for type: "
                    + type);
        }

        SwarmSourceDownloader sourceHandler = sourceHandlers.get(type);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding source: " + source);
        }
        sourceHandler.addSource(source);

    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.Swarmer#start()
     */
    public void start() {
        for (SwarmSourceDownloader handler : sourceHandlers.values()) {
            try {
                handler.start();
            } catch (IOException iox) {
                LOG.warn("Unable to start swarm source handler: " + handler);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.Swarmer#shutdown()
     */
    public void shutdown() {
        for (SwarmSourceDownloader handler : sourceHandlers.values()) {
            try {
                handler.shutdown();
            } catch (IOException iox) {
                LOG.warn("Unable to shutdown swarm source handler: " + handler);
            }
        }
        try {
            swarmCoordinator.finish();
        } catch (IOException iox) {
            LOG.warn("Unable to shutdown swarm coordinator: " + swarmCoordinator);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.Swarmer#getMeasuredBandwidth(boolean)
     */
    public float getMeasuredBandwidth(boolean downstream) {
        float bandwidth = 0;
        for (SwarmSourceDownloader handler : sourceHandlers.values()) {
            bandwidth += handler.getMeasuredBandwidth(downstream);
        }
        return bandwidth;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.Swarmer#hasHandler(org.limewire.swarm.SwarmSourceType)
     */
    public boolean hasHandler(SwarmSourceType type) {
        return sourceHandlers.containsKey(type);
    }

}

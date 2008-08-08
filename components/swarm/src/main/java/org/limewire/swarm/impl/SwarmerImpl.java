package org.limewire.swarm.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceEventListener;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.Swarmer;
import org.limewire.swarm.http.SwarmHttpSourceHandler;
import org.limewire.util.Objects;

public class SwarmerImpl implements Swarmer {

    private static final Log LOG = LogFactory.getLog(SwarmerImpl.class);

    private final Map<SwarmSource.Type, SwarmSourceHandler> sourceHandlers;

    private final SwarmCoordinator swarmCoordinator;

    public SwarmerImpl(SwarmCoordinator swarmCoordinator) {
        this.swarmCoordinator = Objects.nonNull(swarmCoordinator, "swarmCoordinator");
        this.sourceHandlers = Collections
                .synchronizedMap(new HashMap<SwarmSource.Type, SwarmSourceHandler>());
        register(SwarmSource.Type.HTTP, new SwarmHttpSourceHandler(swarmCoordinator));
    }

    public SwarmSourceHandler getSwarmSourceHandler(Class<SwarmSource> clazz) {
        return sourceHandlers.get(clazz);
    }

    public void register(SwarmSource.Type type, SwarmSourceHandler sourceHandler) {
        sourceHandlers.put(type, sourceHandler);
    }

    public void addSource(SwarmSource source) {
        addSource(source, null);
    }

    public void addSource(SwarmSource source, SwarmSourceEventListener sourceEventListener) {
        SwarmSourceHandler sourceHandler = sourceHandlers.get(source.getType());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding source: " + source);
        }

        sourceHandler.addSource(source, sourceEventListener);
    }

    public void start() {
        for (SwarmSourceHandler handler : sourceHandlers.values()) {
            try {
                handler.start();
            } catch (IOException iox) {
                LOG.warn("Unable to start handler: " + handler);
            }
        }
    }

    public void shutdown() {
        for (SwarmSourceHandler handler : sourceHandlers.values()) {
            try {
                handler.shutdown();
            } catch (IOException iox) {
                LOG.warn("Unable to shutdown swarm handler: " + handler);
            }
        }
        try {
            swarmCoordinator.finish();
        } catch (IOException iox) {
            LOG.warn("Unable to swarm coordinator" + swarmCoordinator);
        }
    }

    public float getMeasuredBandwidth(boolean downstream) {
        float bandwidth = 0;
        for (SwarmSourceHandler handler : sourceHandlers.values()) {
            bandwidth += handler.getMeasuredBandwidth(downstream);
        }
        return bandwidth;
    }

}

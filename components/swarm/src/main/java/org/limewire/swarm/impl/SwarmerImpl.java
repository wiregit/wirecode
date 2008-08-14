package org.limewire.swarm.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmSourceType;
import org.limewire.swarm.Swarmer;
import org.limewire.swarm.http.SwarmHttpSourceHandler;
import org.limewire.util.Objects;

public class SwarmerImpl implements Swarmer {

    private static final Log LOG = LogFactory.getLog(SwarmerImpl.class);

    private final Map<SwarmSourceType, SwarmSourceHandler> sourceHandlers;

    private final SwarmCoordinator swarmCoordinator;

    public SwarmerImpl(SwarmCoordinator swarmCoordinator) {
        this.swarmCoordinator = Objects.nonNull(swarmCoordinator, "swarmCoordinator");
        this.sourceHandlers = Collections
                .synchronizedMap(new HashMap<SwarmSourceType, SwarmSourceHandler>());
        register(SwarmSourceType.HTTP, new SwarmHttpSourceHandler(swarmCoordinator));
    }

    public SwarmSourceHandler getSwarmSourceHandler(Class<SwarmSource> clazz) {
        return sourceHandlers.get(clazz);
    }

    public void register(SwarmSourceType type, SwarmSourceHandler sourceHandler) {
        sourceHandlers.put(type, sourceHandler);
    }

    public void addSource(SwarmSource source) {
        SwarmSourceType type = source.getType();

        if (!hasHandler(type)) {
            throw new IllegalStateException("No swarm source handler is registered for type: "
                    + type);
        }

        SwarmSourceHandler sourceHandler = sourceHandlers.get(type);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding source: " + source);
        }
        sourceHandler.addSource(source);

    }

    public void start() {
        for (SwarmSourceHandler handler : sourceHandlers.values()) {
            try {
                handler.start();
            } catch (IOException iox) {
                LOG.warn("Unable to start swarm source handler: " + handler);
            }
        }
    }

    public void shutdown() {
        for (SwarmSourceHandler handler : sourceHandlers.values()) {
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

    public float getMeasuredBandwidth(boolean downstream) {
        float bandwidth = 0;
        for (SwarmSourceHandler handler : sourceHandlers.values()) {
            bandwidth += handler.getMeasuredBandwidth(downstream);
        }
        return bandwidth;
    }

    public boolean hasHandler(SwarmSourceType type) {
        return sourceHandlers.containsKey(type);
    }

    public boolean hasSource(SwarmSource source) {
        for (SwarmSourceHandler handler : sourceHandlers.values()) {
            if (handler.hasSource(source)) {
                return true;
            }
        }
        return false;
    }

    public boolean isBadSource(SwarmSource source) {
        for (SwarmSourceHandler handler : sourceHandlers.values()) {
            if (handler.isBadSource(source)) {
                return true;
            }
        }
        return false;
    }

}

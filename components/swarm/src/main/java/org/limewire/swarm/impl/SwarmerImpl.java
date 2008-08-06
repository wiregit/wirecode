package org.limewire.swarm.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceEventListener;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.Swarmer;

public class SwarmerImpl implements Swarmer {

    private static final Log LOG = LogFactory.getLog(SwarmerImpl.class);

    private final Map<Class, SwarmSourceHandler> sourceHandlers;

    public SwarmerImpl() {
        sourceHandlers = Collections.synchronizedMap(new HashMap<Class, SwarmSourceHandler>());
    }

    public void addSource(SwarmSource source) {
        addSource(source, null);
    }

    public void addSource(SwarmSource source, SwarmSourceEventListener sourceEventListener) {
        SwarmSourceHandler sourceHandler = sourceHandlers.get(source.getClass());

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
                LOG.warn("Unable to shutdown handler: " + handler);
            }
        }
    }

    public void register(Class clazz, SwarmSourceHandler sourceHandler) {
        sourceHandlers.put(clazz, sourceHandler);
    }

}

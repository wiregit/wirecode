package org.limewire.swarm;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SwarmerImpl /*implements Swarmer*/ {

    private Map<Class, SwarmSourceHandler> sourceHandlers = Collections
            .synchronizedMap(new HashMap<Class, SwarmSourceHandler>());

    private static final Log LOG = LogFactory.getLog(SwarmerImpl.class);

    public SwarmerImpl() {
        this(null);
    }

    public SwarmerImpl(SourceEventListener globalSourceEventListener) {
    }

    public void addSource(final SwarmSource source) {
        addSource(source, null);
    }

    public void addSource(final SwarmSource source, SourceEventListener sourceEventListener) {
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

    public boolean isActive() {
        for (SwarmSourceHandler handler : sourceHandlers.values()) {
            if (handler.isActive()) {
                return true;
            }
        }
        return false;
    }

    public void register(Class clazz, SwarmSourceHandler sourceHandler) {
        sourceHandlers.put(clazz, sourceHandler);
    }

}

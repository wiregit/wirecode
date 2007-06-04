package com.limegroup.gnutella.store.storeserver;

import java.util.Map;

import org.limewire.store.storeserver.api.Server;

import com.limegroup.gnutella.store.storeserver.StoreManager;

/**
 * Generic base class for {@link StoreManager.Handler}s.
 */
public abstract class AbstractHandler extends HasName implements StoreManager.Handler {
    
    public AbstractHandler(String name) { super(name); }
    public AbstractHandler() { super(); }
    
    /**
     * A {@link Handler} that doesn't respond with anything.
     */
    public abstract static class OK extends AbstractHandler {
        
        public OK(String name) { super(name); }
        
        /**
         * Override this to do something without responding.
         */
        protected abstract void doHandle(Map<String, String> args);
        
        public final String handle(final Map<String, String> args) {
            doHandle(args);
            return Server.Responses.OK;
        }
    }
}
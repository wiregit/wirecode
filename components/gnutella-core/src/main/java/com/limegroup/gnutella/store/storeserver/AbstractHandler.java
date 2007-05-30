package com.limegroup.gnutella.store.storeserver;

import java.util.Map;

import org.limewire.store.storeserver.api.IServer;

import com.limegroup.gnutella.store.storeserver.IStoreServer;

/**
 * Generic base class for {@link IStoreServer.Handler}s.
 * 
 * @author jpalm
 */
public abstract class AbstractHandler extends HasName implements IStoreServer.Handler {
    
    public AbstractHandler(String name) { super(name); }
    public AbstractHandler() { super(); }
    
    /**
     * A {@link Handler} that doesn't respond with anything.
     * 
     * @author jpalm
     */
    public abstract static class OK extends AbstractHandler {
        
        public OK(String name) { super(name); }
        
        /**
         * Override this to do something without responding.
         */
        protected abstract void doHandle(Map<String, String> args);
        
        public final String handle(final Map<String, String> args) {
            doHandle(args);
            return IServer.Responses.OK;
        }
    }
}
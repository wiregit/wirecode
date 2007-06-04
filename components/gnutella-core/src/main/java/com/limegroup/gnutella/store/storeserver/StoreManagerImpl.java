package com.limegroup.gnutella.store.storeserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.store.storeserver.api.AbstractDispatchee;
import org.limewire.store.storeserver.api.ConnectionListener;
import org.limewire.store.storeserver.api.Dispatchee;
import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.api.URLSocketOpenner;


/**
 * Encapsulates a local server and dispatchee.
 */
final class StoreManagerImpl implements StoreManager {
    
    // -----------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------
  
    static StoreManagerImpl newDemoInstance() {
        final Server s = Server.FACTORY.newInstance(8090, new URLSocketOpenner());
        final DispatcheeImpl d = new DispatcheeImpl(s);
        StoreManagerImpl res = new StoreManagerImpl(s, d);
        d.server = res;
        return res;
    }
    
    // -----------------------------------------------------------------
    // Instance
    // -----------------------------------------------------------------

    private final Server localServer;
    private final Map<String, StoreManager.Handler> commands2handlers  
        = new HashMap<String, StoreManager.Handler>();
    private final Map<String, List<StoreManager.Listener>> commands2listenerLists 
        = new HashMap<String, List<StoreManager.Listener>>();
    
    private StoreManagerImpl(Server localServer, Dispatchee dispatchee) {
        this.localServer = localServer;
        this.localServer.setDispatchee(dispatchee);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.store.storeserver.IStoreServer#getLocalServer()
     */
    public final Server getLocalServer() {
        return this.localServer;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.store.storeserver.IStoreServer#start()
     */
    public final void start() {
        this.localServer.start();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.store.storeserver.IStoreServer#shutDown(long)
     */
    public final void shutDown(long millis) {
        this.localServer.shutDown(millis);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.store.storeserver.IStoreServer#addConnectionListener(org.limewire.store.storeserver.api.IConnectionListener)
     */
    public final boolean addConnectionListener(ConnectionListener lis) {
        return this.localServer.getDispatchee().addConnectionListener(lis);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.store.storeserver.IStoreServer#removeConnectionListener(org.limewire.store.storeserver.api.IConnectionListener)
     */
    public final boolean removeConnectionListener(ConnectionListener lis) {
        return this.localServer.getDispatchee().removeConnectionListener(lis);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.store.storeserver.IStoreServer#registerHandler(java.lang.String, com.limegroup.gnutella.store.storeserver.StoreServer.Handler)
     */
    public final boolean registerHandler(String cmd, StoreManager.Handler lis) {
        if (cmd == null) {
            throw new NullPointerException("Null command for handler: " + lis.name());
        }
        final String hash = hash(cmd);
        return commands2handlers.get(hash) != null ? false : commands2handlers.put(hash, lis) != null;
    }
    
    private String hash(String cmd) {
        return cmd.toLowerCase();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.store.storeserver.IStoreServer#registerListener(java.lang.String, com.limegroup.gnutella.store.storeserver.StoreServer.Listener)
     */
    public final boolean registerListener(String cmd, StoreManager.Listener lis) {
        if (cmd == null) {
            throw new NullPointerException("Null command for listener: " + lis.name());
        }
        final String hash = hash(cmd);
        List<StoreManager.Listener> lst = commands2listenerLists.get(hash);
        if (lst == null) commands2listenerLists.put(hash, lst = new ArrayList<StoreManager.Listener>());
        return lst.contains(lis) ? false : lst.add(lis);
    }
    
    // ------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------
    
    /**
     * Dispatch the command <tt>cmd</tt> to a handler and all the listeners.
     * and return the result from the handler.
     * 
     * @param cmd   command
     * @param args  arguments
     * @return      result from the handler mapped to <tt>cmd</tt> or {@link ErrorCodes#UNKNOWN_COMMAND}.
     */
    private String dispatch(String cmd, Map<String, String> args) {
        if (cmd == null) {
            return localServer.report(Server.ErrorCodes.UNKNOWN_COMMAND);
        }
        final String hash = hash(cmd);
        StoreManager.Handler h = commands2handlers.get(hash);
        String res = null;
        boolean handled = false;
        if (h != null) {
            handled = true;
            res = h.handle(args);
        }
        List<StoreManager.Listener> ls = commands2listenerLists.get(hash);
        if (ls != null && !ls.isEmpty()) {
            handled = true;
            for (StoreManager.Listener l : ls) l.handle(args);
        }
        if (!handled) {
            return localServer.report(Server.ErrorCodes.UNKNOWN_COMMAND);
        } else {
            if (res == null) {
                return Server.Responses.OK;
            } else {
                return res;
            }
        }
    } 
    
    
    // -----------------------------------------------------------------
    // Dispatchee
    // -----------------------------------------------------------------
        
    /**
     * Default {@link Dispatchee} class that handles commands from the local server.
     * 
     * @author jpalm
     */
    final static class DispatcheeImpl extends AbstractDispatchee {
        
        private StoreManagerImpl server;

        public DispatcheeImpl(Server server) {
            super(server);
        }

        public String dispatch(String cmd, Map<String, String> args) {
            return this.server.dispatch(cmd, args);
        }

        protected void connectionChanged(boolean isConnected) {
            System.out.println("noteConnected:" + isConnected);
        }
    }
   
}

package com.limegroup.gnutella.store.storeserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.store.storeserver.core.AbstractDispatchee;
import org.limewire.store.storeserver.core.ConnectionListener;
import org.limewire.store.storeserver.core.Dispatchee;
import org.limewire.store.storeserver.core.ErrorCodes;
import org.limewire.store.storeserver.core.LocalServer;
import org.limewire.store.storeserver.core.Request;
import org.limewire.store.storeserver.core.Responses;
import org.limewire.store.storeserver.core.Server;
import org.limewire.store.storeserver.core.Server.Handler;
import org.limewire.store.storeserver.core.Server.Listener;
import org.limewire.store.storeserver.local.LocalLocalServer;


/**
 * Encapsulates a local server and dispatchee.
 * 
 * @author jpalm
 */
public final class StoreServer implements ConnectionListener.HasSome {
    
    // -----------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------
    
    private static StoreServer instance;
    
    public static StoreServer instance() {
        if (instance == null) instance = newDemoInstance();
        return instance;
    }
    
    private static StoreServer newDemoInstance() {
        final LocalServer s = new LocalLocalServer(8090, false);
        s.setDebug(true);
        final DispatcheeImpl d = new DispatcheeImpl(s);
        StoreServer res = new StoreServer(s, d);
        d.server = res;
        return res;
    }
    
    // -----------------------------------------------------------------
    // Instance
    // -----------------------------------------------------------------

    private final LocalServer localServer;
    private final Map<String, Handler> commands2handlers  = new HashMap<String, Handler>();
    private final Map<String, List<Listener>> commands2listenerLists = new HashMap<String, List<Listener>>();
    
    public StoreServer(LocalServer localServer, Dispatchee dispatchee) {
        this.localServer = localServer;
        this.localServer.setDispatchee(dispatchee);
    }
    
    /**
     * Returns the local server.
     * 
     * @return the local server
     */
    public final LocalServer getLocalServer() {
        return this.localServer;
    }
    
    /**
     * Starts this service.
     */
    public void start() {
        Server.start(this.localServer);
    }
    
    public final boolean addConnectionListener(ConnectionListener lis) {
        return this.localServer.getDispatchee().addConnectionListener(lis);
    }

    public final boolean removeConnectionListener(ConnectionListener lis) {
        return this.localServer.getDispatchee().removeConnectionListener(lis);
    }
    
    /**
     * Register a listener for the command <tt>cmd</tt>, and returns <tt>true</tt> on success
     * and <tt>false</tt> on failure.  There can be only <b>one</b> {@link Handler} for
     * every command.
     * 
     * @param cmd   String that invokes this listener
     * @param lis   listener
     * @return <tt>true</tt> if we added, <tt>false</tt> for a problem or if this command
     *         is already registered
     */
    public final boolean registerHandler(String cmd, Handler lis) {
        if (cmd == null) {
            throw new NullPointerException("Null command for handler: " + lis.name());
        }
        final String hash = hash(cmd);
        return commands2handlers.get(hash) != null ? false : commands2handlers.put(hash, lis) != null;
    }
    
    private String hash(String cmd) {
        return cmd.toLowerCase();
    }

    /**
     * Registers a listener for the command <tt>cmd</tt>.  There can be multiple listeners
     * 
     * @param cmd
     * @param lis
     * @return
     */
    public final boolean registerListener(String cmd, Listener lis) {
        if (cmd == null) {
            throw new NullPointerException("Null command for listener: " + lis.name());
        }
        final String hash = hash(cmd);
        List<Listener> lst = commands2listenerLists.get(hash);
        if (lst == null) commands2listenerLists.put(hash, lst = new ArrayList<Listener>());
        return lst.contains(lis) ? false : lst.add(lis);
    }
    
    /**
     * An event that is generated.
     * 
     * @author jeff
     */
    public interface Event {
   
        /**
         * Returns the name of the command.
         * 
         * @return the name of the command
         */
        String getCommand();
        
        /**
         * Returns the (<em>name</em>,<em>value</em>) arguments to this command.
         * 
         * @return the (<em>name</em>,<em>value</em>) arguments to this command
         */
        Map<String, String> getArguments();
    }
    
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
            return localServer.report(ErrorCodes.UNKNOWN_COMMAND);
        }
        final String hash = hash(cmd);
        Handler h = commands2handlers.get(hash);
        String res = null;
        boolean handled = false;
        final Request req = null;
        if (h != null) {
            handled = true;
            res = h.handle(args, req);
        }
        List<Listener> ls = commands2listenerLists.get(hash);
        if (!ls.isEmpty()) {
            handled = true;
            for (Listener l : ls) l.handle(args, req);
        }
        if (!handled) {
            return localServer.report(ErrorCodes.UNKNOWN_COMMAND);
        } else {
            if (res == null) {
                return Responses.OK;
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
        
        private StoreServer server;

        public DispatcheeImpl(LocalServer server) {
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

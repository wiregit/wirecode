package com.limegroup.gnutella.store.storeserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.store.storeserver.api.AbstractDispatchee;
import org.limewire.store.storeserver.api.IConnectionListener;
import org.limewire.store.storeserver.api.IDispatchee;
import org.limewire.store.storeserver.api.IServer;
import org.limewire.store.storeserver.core.Server.Handler;


/**
 * Encapsulates a local server and dispatchee.
 * 
 * @author jpalm
 */
public final class StoreServer implements IConnectionListener.HasSome {
    
    // -----------------------------------------------------------------
    // Factory
    // -----------------------------------------------------------------
    
    private static StoreServer instance;
    
    public static StoreServer instance() {
        if (instance == null) instance = newDemoInstance();
        return instance;
    }
    
    private static StoreServer newDemoInstance() {
        final IServer s = IServer.FACTORY.newInstance(8090, true);
        final DispatcheeImpl d = new DispatcheeImpl(s);
        StoreServer res = new StoreServer(s, d);
        d.server = res;
        return res;
    }
    
    // -----------------------------------------------------------------
    // Instance
    // -----------------------------------------------------------------

    private final IServer localServer;
    private final Map<String, Handler> commands2handlers  = new HashMap<String, Handler>();
    private final Map<String, List<Listener>> commands2listenerLists = new HashMap<String, List<Listener>>();
    
    public StoreServer(IServer localServer, IDispatchee dispatchee) {
        this.localServer = localServer;
        this.localServer.setDispatchee(dispatchee);
    }
    
    /**
     * Returns the local server.
     * 
     * @return the local server
     */
    public final IServer getLocalServer() {
        return this.localServer;
    }
    
    /**
     * Starts this service.
     */
    public void start() {
        this.localServer.start();
    }
    
    public final boolean addConnectionListener(IConnectionListener lis) {
        return this.localServer.getDispatchee().addConnectionListener(lis);
    }

    public final boolean removeConnectionListener(IConnectionListener lis) {
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
    
    // ------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------
    
    /** 
     * Handles commands. 
     */
    public interface Handler {
      
      /**
       * Perform some operation on the incoming message and return the result.
       * 
       * @param args  CGI params
       * @return      the result of performing some operation on the incoming message
       */
      String handle(Map<String, String> args);

      /**
       * Returns the unique name of this instance.
       * 
       * @return the unique name of this instance
       */
      String name();
    }
    
    /** 
     * Handles commands, but does NOT return a result.
     */
    public interface Listener {
      
      /**
       * Perform some operation on the incoming message.
       * 
       * @param args  CGI params
       */
      void handle(Map<String, String> args);

      /**
       * Returns the unique name of this instance.
       * 
       * @return the unique name of this instance
       */
      String name();
    }
    
    abstract static class HasName {

        private final String name;

        public HasName(final String name) {
          this.name = name;
        }

        public HasName() {
          String n = getClass().getName();
          int ilast;
          ilast = n.lastIndexOf(".");
          if (ilast != -1) n = n.substring(ilast + 1);
          ilast = n.lastIndexOf("$");
          if (ilast != -1) n = n.substring(ilast + 1);
          this.name = n;
        }

        public final String name() {
          return name;
        }

        protected final String getArg(final Map<String, String> args, final String key) {
          final String res = args.get(key);
          return res == null ? "" : res;
        }
        
    }
    
    /**
     * Generic base class for {@link Listener}s.
     * 
     * @author jpalm
     */
    public static abstract class AbstractListener extends HasName implements Listener {
        public AbstractListener(String name) { super(name); }
        public AbstractListener() { super(); }
    }
    
    /**
     * Generic base class for {@link Handler}s.
     * 
     * @author jpalm
     */
    public static abstract class AbstractHandler extends HasName implements Handler {
        public AbstractHandler(String name) { super(name); }
        public AbstractHandler() { super(); }
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
        System.out.println("dispatch: " + cmd + "(" + args + ")");
        if (cmd == null) {
            return localServer.report(IServer.ErrorCodes.UNKNOWN_COMMAND);
        }
        final String hash = hash(cmd);
        Handler h = commands2handlers.get(hash);
        String res = null;
        boolean handled = false;
        if (h != null) {
            handled = true;
            res = h.handle(args);
        }
        List<Listener> ls = commands2listenerLists.get(hash);
        if (ls != null && !ls.isEmpty()) {
            handled = true;
            for (Listener l : ls) l.handle(args);
        }
        if (!handled) {
            return localServer.report(IServer.ErrorCodes.UNKNOWN_COMMAND);
        } else {
            if (res == null) {
                return IServer.Responses.OK;
            } else {
                return res;
            }
        }
    } 
    
    
    // -----------------------------------------------------------------
    // Dispatchee
    // -----------------------------------------------------------------
        
    /**
     * Default {@link IDispatchee} class that handles commands from the local server.
     * 
     * @author jpalm
     */
    final static class DispatcheeImpl extends AbstractDispatchee {
        
        private StoreServer server;

        public DispatcheeImpl(IServer server) {
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

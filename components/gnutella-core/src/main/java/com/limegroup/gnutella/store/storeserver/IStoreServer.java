package com.limegroup.gnutella.store.storeserver;

import java.util.Map;

import org.limewire.store.storeserver.api.IConnectionListener;
import org.limewire.store.storeserver.api.IServer;
import org.limewire.store.storeserver.api.IConnectionListener.HasSome;


/**
 * The interface to which GUI and other units program for the store server.
 * 
 * @author jpalm
 */
public interface IStoreServer extends IConnectionListener.HasSome {
    
    /**
     * Is able to return and possibly construct an instance.
     * 
     * @author jpalm
     */
    public interface Holder {
        
        /**
         * Returns and possibly constructs the single instance.
         * 
         * @return and possibly constructs the single instance
         */
        IStoreServer instance();
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

    public final static Holder HOLDER = new Holder() {
        private StoreServer instance;       
        public StoreServer instance() {
            if (instance == null) instance = StoreServer.newDemoInstance();
            return instance;
        }        
    };

    /**
     * Returns the local server.
     * 
     * @return the local server
     */
    public abstract IServer getLocalServer();

    /**
     * Starts this service.
     * 
     * @see IServer#start()
     */
    public abstract void start();

    /**
     * Shuts down this service.
     * 
     * @param millis milliseconds to wait before causing an abrupt stop
     * @see IServer#shutDown(long)
     */
    public abstract void shutDown(long millis);

    /**
     * @see HasSome#addConnectionListener(IConnectionListener)
     */
    public abstract boolean addConnectionListener(IConnectionListener lis);

    /**
     * @see HasSome#removeConnectionListener(IConnectionListener)
     */
    public abstract boolean removeConnectionListener(IConnectionListener lis);

    /**
     * Register a listener for the command <tt>cmd</tt>, and returns <tt>true</tt> on success
     * and <tt>false</tt> on failure.  There can be only <b>one</b> {@link IStoreServer.Handler} for
     * every command.
     * 
     * @param cmd   String that invokes this listener
     * @param lis   listener
     * @return <tt>true</tt> if we added, <tt>false</tt> for a problem or if this command
     *         is already registered
     */
    public abstract boolean registerHandler(String cmd, IStoreServer.Handler lis);

    /**
     * Registers a listener for the command <tt>cmd</tt>.  There can be multiple listeners
     * 
     * @param cmd
     * @param lis
     * @return
     */
    public abstract boolean registerListener(String cmd, IStoreServer.Listener lis);

}
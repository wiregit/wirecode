package com.limegroup.gnutella.store.server;


import java.util.Map;

import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.store.server.ConnectionListener;



/**
 * The interface to which GUI and other units program for the store server. This
 * class contains an instance of {@link HttpRequestHandler} which can attach to
 * an acceptor. To instantiate the store manager one would call:
 * 
 * <blockquote>
 * <pre>
 * StoreManager.HOLDER.instance()
 * </pre>
 * </blockquote>
 * 
 * One can add {@link Handler}s and {@link Listener}s to an instance by
 * calling the following methods
 * <ul>
 * <li>{@link #registerHandler(String, com.limegroup.gnutella.store.storeserver.StoreManager.Handler)}</li>
 * <li>{@link #registerListener(String, com.limegroup.gnutella.store.storeserver.StoreManager.Listener)}</li>
 * </ul>
 * with the rules that only <u>one</u> handler may be registered per command
 * and zero or more listeners may be added for every command. It's expected that
 * at least one handler or listener will be registered for every command.
 */
public interface StoreManager extends ConnectionListener.HasSome {
    
    public final static Holder HOLDER = new Holder() {
        private StoreManagerImpl instance;       
        public StoreManagerImpl instance() {
            if (instance == null) instance = StoreManagerImpl.newDemoInstance();
            return instance;
        }        
    };
    
    /**
     * Returns the instance of {@link HttpRequestHandler} responsible for passing along messages.
     * 
     * @return the instance of {@link HttpRequestHandler} responsible for passing along messages
     */
    HttpRequestHandler getHandler();
    
    /**
     * Is able to return and possibly construct an instance.
     */
    public interface Holder {
        
        /**
         * Returns and possibly constructs the single instance.
         * 
         * @return and possibly constructs the single instance
         */
        StoreManager instance();
    }

    /**
     * Handles commands.
     */
    public interface Handler {
    
        /**
         * Perform some operation on the incoming message and return the result.
         * 
         * @param args CGI params
         * @return the result of performing some operation on the incoming
         *         message
         */
        String handle(Map<String, String> args);
    
        /**
         * Returns the unique name of this instance.
         * 
         * @return the unique name of this instance
         */
        String name();
        
        public interface CanRegister {
            /**
             * Register a handler for the command <tt>cmd</tt>, and returns
             * <tt>true</tt> on success and <tt>false</tt> on failure. There
             * can be only <b>one</b> {@link StoreManager.Handler} for every
             * command.
             * 
             * @param cmd String that invokes this listener
             * @param lis handler
             * @return <tt>true</tt> if we added, <tt>false</tt> for a
             *         problem or if this command is already registered
             */
            boolean registerHandler(String cmd, Handler lis);
        }
    }

    /**
     * Handles commands, but does NOT return a result.
     */
    public interface Listener {
    
        /**
         * Perform some operation on the incoming message.
         * 
         * @param args CGI params
         * @param req incoming {@link Request}
         */
        void handle(Map<String, String> args);
    
        /**
         * Returns the unique name of this instance.
         * 
         * @return the unique name of this instance
         */
        String name();
        
        public interface CanRegister {
    
            /**
             * Register a listener for the command <tt>cmd</tt>, and returns
             * <tt>true</tt> on success and <tt>false</tt> on failure. There
             * can be only <b>one</b> {@link Handler} for every
             * command.
             * 
             * @param cmd String that invokes this listener
             * @param lis listener
             * @return <tt>true</tt> if we added, <tt>false</tt> for a
             *         problem or if this command is already registered
             */
            boolean registerListener(String cmd, Listener lis);
        }        
    }

    /**
     * Register a listener for the command <tt>cmd</tt>, and returns <tt>true</tt> on success
     * and <tt>false</tt> on failure.  There can be only <b>one</b> {@link StoreManager.Handler} for
     * every command.
     * 
     * @param cmd   String that invokes this listener
     * @param lis   listener
     * @return <tt>true</tt> if we added, <tt>false</tt> for a problem or if this command
     *         is already registered
     */
    boolean registerHandler(String cmd, Handler lis);

    /**
     * Registers a listener for the command <tt>cmd</tt>.  There can be multiple listeners
     * 
     * @param cmd
     * @param lis
     * @return
     */
    boolean registerListener(String cmd, Listener lis);

}
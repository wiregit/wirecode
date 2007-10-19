package com.limegroup.gnutella.lws.server;


import java.util.Map;

import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.lws.server.ConnectionListener;
import org.limewire.lws.server.LWSDispatcher;

/**
 * The interface to which GUI and other units program for the store server. This
 * class contains an instance of {@link HttpRequestHandler} which can attach to
 * an acceptor. <br/><br/> One can add {@link Handler}s and {@link Listener}s
 * to an instance by calling the following methods:
 * <ul>
 * <li>{@link #registerHandler(String, com.limegroup.gnutella.store.storeserver.LWSManager.Handler)}</li>
 * <li>{@link #registerListener(String, com.limegroup.gnutella.store.storeserver.LWSManager.Listener)}</li>
 * </ul>
 * with the rules that only <u>one</u> handler may be registered per command
 * and zero or more listeners may be added for every command. It's expected that
 * at least one handler or listener will be registered for every command. <br>
 * 
 * <br/>
 * <br/>
 * 
 * Here is an example of registering handlers, in whichthe
 * example we want to control the music player from a web page.  The actions inside
 * <code>doHandle</code> may become outdated as the rest of the code outside
 * this package changes, but it's meant to exemplify using this class:
 * 
 * <pre>
 * LimeWireCore.getLWSManger().registerHandler(&quot;Back&quot;, new StoreManager.AbstractHandler.OK(&quot;Back&quot;) {
 *     protected void doHandle(Map&lt;String, String&gt; args) {
 *         MediaPlayerComponent.instance().backSong();
 *     }
 * });
 * LimeWireCore.getLWSManger().registerHandler(&quot;Stop&quot;, new StoreManager.AbstractHandler.OK(&quot;Stop&quot;) {
 *     protected void doHandle(Map&lt;String, String&gt; args) {
 *         MediaPlayerComponent.instance().doStopSong();
 *     }
 * });
 * LimeWireCore.getLWSManger().registerHandler(&quot;Play&quot;, new StoreManager.AbstractHandler.OK(&quot;Play&quot;) {
 *     protected void doHandle(Map&lt;String, String&gt; args) {
 *         MediaPlayerComponent.instance().playSong();
 *     }
 * });
 * LimeWireCore.getLWSManger().registerHandler(&quot;Next&quot;, new StoreManager.AbstractHandler.OK(&quot;Next&quot;) {
 *     protected void doHandle(Map&lt;String, String&gt; args) {
 *         MediaPlayerComponent.instance().nextSong();
 *     }
 * });
 * </pre>
 */
public interface LWSManager {
       
    /**
     * The prefix to all requests. This will be stripped off when sending to our
     * handlers.
     */
    String PREFIX = LWSDispatcher.PREFIX;
    
    /**
     * Returns the instance of {@link HttpRequestHandler} responsible for
     * passing along messages.
     * 
     * @return the instance of {@link HttpRequestHandler} responsible for
     *         passing along messages
     */
    HttpRequestHandler getHandler();
    
    /**
     * Returns <tt>true</tt> if <tt>lis</tt> was added as a listener,
     * <tt>false</tt> otherwise.
     * 
     * @param lis new listener
     * @return <tt>true</tt> if <tt>lis</tt> was added as a listener,
     *         <tt>false</tt> otherwise.
     */
    boolean addConnectionListener(ConnectionListener lis);

    /**
     * Returns <tt>true</tt> if <tt>lis</tt> was removed as a listener,
     * <tt>false</tt> otherwise.
     * 
     * @param lis old listener
     * @return <tt>true</tt> if <tt>lis</tt> was removed as a listener,
     *         <tt>false</tt> otherwise.
     */
    boolean removeConnectionListener(ConnectionListener lis);    
    
    /**
     * Defines the interface to handle commands sent to a {@link LWSManager}.
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
    }
    
    /**
     * Register a handler for the command <tt>cmd</tt>, and returns
     * <tt>true</tt> on success and <tt>false</tt> on failure. There
     * can be only <b>one</b> {@link LWSManager.Handler} for every
     * command.
     * 
     * @param cmd String that invokes this listener
     * @param lis handler
     * @return <tt>true</tt> if we added, <tt>false</tt> for a
     *         problem or if this command is already registered
     */
    boolean registerHandler(String cmd, Handler lis);

    /**
     * Defines the interface to handle commands, but does <b>NOT</b> return a
     * result.
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
    }
    
    /**
     * Registers a {@link LWSManager.Listener} for the command <tt>cmd</tt>.
     * There can be multiple {@link LWSManager.Listener}s.
     * 
     * @param cmd String that invokes this listener
     * @param lis listener to register
     * @return <tt>true</tt> if we added, <tt>false</tt> for a problem or if
     *         this command is already registered
     */
    boolean registerListener(String cmd, Listener lis);  
    
    /**
     * An abstract implementation of {@link Handler} that abstracts away
     * {@link #name()}.
     */
    public abstract class AbstractHandler implements Handler {

        private final String name;

        public AbstractHandler(String name) {
            this.name = name;
        }

        public final String name() {
            return name;
        }
    }
    
    /**
     * An abstract implementation of {@link AbstractHandler} that abstracts away
     * returning a value from {@link #handle(Map)}.
     */
    public abstract static class OK extends AbstractHandler {

        public OK(String name) {
            super(name);
        }

        public final String handle(Map<String, String> args) {
            doHandle(args);
            return "OK";
        }

        /**
         * Override this to do some work, but return nothing.
         * 
         * @param args unchanged CGI parameters passed in to
         *        {@link #handle(Map)}
         */
        abstract void doHandle(Map<String, String> args);
    }    
    
    /**
     * An abstract implementation of {@link Listener} that abstracts away
     * {@link #name()}.
     */
    public abstract class AbstractListener implements Listener {

        private final String name;
        
        public AbstractListener(String name) {
            this.name = name;
        }

        public final String name() {
            return name;
        }   
    }    

}
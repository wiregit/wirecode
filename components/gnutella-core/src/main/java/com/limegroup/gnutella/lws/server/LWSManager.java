package com.limegroup.gnutella.lws.server;


import java.util.Map;

import org.apache.http.nio.protocol.NHttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.lws.server.LWSConnectionListener;
import org.limewire.lws.server.LWSDispatcher;


/**
 * The interface to which GUI and other units program for the store server. This
 * class contains an instance of {@link HttpRequestHandler} which can attach to
 * an acceptor. <br/><br/> One can add {@link LWSManagerCommandResponseHandler}s
 * to an instance by calling the following method:
 * {@link #registerHandler(String, com.limegroup.gnutella.store.storeserver.LWSManager.LWSManagerCommandResponseHandler)}</li>
 * only <u>one</u> handler may be registered per command. It's expected that
 * at one handler will be registered for every command. <br>
 * 
 * <br/>
 * <br/>
 * 
 * Here is an example of registering handlers, in which the
 * example we want to control the music player from a web page.  The actions inside
 * <code>doHandle</code> may become outdated as the rest of the code outside
 * this package changes, but it's meant to exemplify using this class:
 * 
 * <pre>
 * LWSManager mgr = inj.getInstance(LWSManager.class);
 * mgr.registerHandler(&quot;Back&quot;, new StoreManager.AbstractHandler.OK(&quot;Back&quot;) {
 *     protected void doHandle(Map&lt;String, String&gt; args) {
 *         MediaPlayerComponent.instance().backSong();
 *     }
 * });
 * mgr.registerHandler(&quot;Stop&quot;, new StoreManager.AbstractHandler.OK(&quot;Stop&quot;) {
 *     protected void doHandle(Map&lt;String, String&gt; args) {
 *         MediaPlayerComponent.instance().doStopSong();
 *     }
 * });
 * mgr.registerHandler(&quot;Play&quot;, new StoreManager.AbstractHandler.OK(&quot;Play&quot;) {
 *     protected void doHandle(Map&lt;String, String&gt; args) {
 *         MediaPlayerComponent.instance().playSong();
 *     }
 * });
 * mgr.registerHandler(&quot;Next&quot;, new StoreManager.AbstractHandler.OK(&quot;Next&quot;) {
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
    NHttpRequestHandler getHandler();
        
    /**
     * Returns <code>true</code> if <code>this</code> is connected.
     * 
     * @return <code>true</code> if <code>this</code> is connected
     */
    boolean isConnected();
    
    /**
     * Returns <code>true</code> if <code>lis</code> was added as a listener,
     * <code>false</code> otherwise.
     * 
     * @param lis new listener
     * @return <code>true</code> if <code>lis</code> was added as a listener,
     *         <code>false</code> otherwise.
     */
    boolean addConnectionListener(LWSConnectionListener lis);

    /**
     * Returns <code>true</code> if <code>lis</code> was removed as a listener,
     * <code>false</code> otherwise.
     * 
     * @param lis old listener
     * @return <code>true</code> if <code>lis</code> was removed as a listener,
     *         <code>false</code> otherwise.
     */
    boolean removeConnectionListener(LWSConnectionListener lis);    
    
    /**
     * Register a handler for the command <code>cmd</code>There
     * can be only <b>one</b> {@link LWSManagerCommandResponseHandler} for every
     * command.  If one already exists, it will be overwritten.
     * 
     * @param cmd String that invokes this listener
     * @param lis handler
     * @return <code>true</code> if we added <code>lis</code>
     */
    boolean registerHandler(String cmd, LWSManagerCommandResponseHandler lis);
    
    /**
     * Unregisters a handler for the command <code>cmd</code>, and returns
     * <code>true</code> on success and <code>false</code> on failure. There can be
     * only <b>one</b> {@link LWSManagerCommandResponseHandler} for every command.
     * 
     * @param cmd String that invokes this listener
     * @return <code>true</code> if we removed the {@link LWSManagerCommandResponseHandler} for
     *         <code>cmd</code>
     */
    boolean unregisterHandler(String cmd);

    /**
     * An abstract implementation of {@link LWSManagerCommandResponseHandler} that abstracts away
     * {@link #name()}.
     */
    public abstract class AbstractHandler implements LWSManagerCommandResponseHandler {

        private final String name;

        public AbstractHandler(String name) {
            this.name = name;
        }

        public final String name() {
            return name;
        }
        
        @Override
        public String toString() {
            return "Handler(" + name() + ")";
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
     * Clears all the handlers.  This is used mainly for testing.
     */
    void clearHandlers();

}
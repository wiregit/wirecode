package org.limewire.store.server;

import org.apache.http.protocol.HttpRequestHandler;

import com.limegroup.gnutella.store.storeserver.StoreManager.Handler;
import com.limegroup.gnutella.store.storeserver.StoreManager.Listener;

/**
 * This is the main part of this component and allows us to attach our
 * authentication scheme and export and interface so they we can attach
 * instances of {@link HttpRequestHandler} to http acceptors. To open this
 * package one should call:
 * 
 * <blockquote>
 * 
 * <pre>
 * Dispatcher.CREATOR.newInstance(sender, dispatchee)
 * </pre>
 * 
 * </blockquote> providing the correct arguments.
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
 * 
 */
public interface Dispatcher extends HttpRequestHandler, ConnectionListener.HasSome, SendsMessagesToServer {
    
    public final static Creator CREATOR = new Creator() {
        public Dispatcher newInstance(SendsMessagesToServer sender, Dispatchee dispatchee) {
            final StoreServerDispatcher s = new StoreServerDispatcher(sender);
            s.setDispatchee(dispatchee);
            return s;
        }
        
    };
    
    /**
     * Intances of this interface (there is one) will create a new dispatcher
     * instance that can be used to connect to an http acceptor.
     */
    interface Creator {
        
        /**
         * Returns a new {@link Dispatcher} that will connect to the outside
         * world with <code>send</code> and dispatch all messages after
         * authentication to <code>dispatchee</code>.
         * 
         * @param sender connector to the outside world with her
         * @param dispatchee pass local messages along after authentication
         * @return a new {@link Dispatcher} that will connect to the outside
         *         world with <code>send</code> and dispatch all messages
         *         after authentication to <code>dispatchee</code>.
         */
        Dispatcher newInstance(SendsMessagesToServer sender, Dispatchee dispatchee);
    }

}
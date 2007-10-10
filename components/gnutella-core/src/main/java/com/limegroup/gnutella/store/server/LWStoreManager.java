package com.limegroup.gnutella.store.server;


import java.util.Map;

import org.apache.http.protocol.HttpRequestHandler;
import org.limewire.store.server.ConnectionListener;
import org.limewire.store.server.Dispatcher;

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
 * <li>{@link #registerHandler(String, com.limegroup.gnutella.store.storeserver.LWStoreManager.Handler)}</li>
 * <li>{@link #registerListener(String, com.limegroup.gnutella.store.storeserver.LWStoreManager.Listener)}</li>
 * </ul>
 * with the rules that only <u>one</u> handler may be registered per command
 * and zero or more listeners may be added for every command. It's expected that
 * at least one handler or listener will be registered for every command.
 * <br>
 * Here is an example of registering handlers, in which we want to control the music player from a web page:
 * <pre>
 *        RouterService.getStoreManager().registerHandler("Back", new StoreManager.AbstractHandler.OK("Back") {
 *            protected void doHandle(Map<String, String> args) {
 *                MediaPlayerComponent.instance().backSong();
 *            }
 *        });
 *        RouterService.getStoreManager().registerHandler("Stop", new StoreManager.AbstractHandler.OK("Stop") {
 *            protected void doHandle(Map<String, String> args) {
 *                MediaPlayerComponent.instance().doStopSong();
 *            }
 *        });
 *        RouterService.getStoreManager().registerHandler("Play", new StoreManager.AbstractHandler.OK("Play") {
 *            protected void doHandle(Map<String, String> args) {
 *                MediaPlayerComponent.instance().playSong();
 *            }
 *        });
 *        RouterService.getStoreManager().registerHandler("Next", new StoreManager.AbstractHandler.OK("Next") {
 *            protected void doHandle(Map<String, String> args) {
 *                MediaPlayerComponent.instance().nextSong();
 *            }
 *        }); 
 * </pre>
 */
public interface LWStoreManager extends ConnectionListener.HasSome {
       
    /**
     * The prefix to all requests. This will be stripped off when sending to our
     * handlers.
     */
    String PREFIX = Dispatcher.PREFIX;
    
    /**
     * Returns the instance of {@link HttpRequestHandler} responsible for passing along messages.
     * 
     * @return the instance of {@link HttpRequestHandler} responsible for passing along messages
     */
    HttpRequestHandler getHandler();
    
    /**
     * Register a listener for the command <tt>cmd</tt>, and returns <tt>true</tt> on success
     * and <tt>false</tt> on failure.  There can be only <b>one</b> {@link LWStoreManager.Handler} for
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
             * can be only <b>one</b> {@link LWStoreManager.Handler} for every
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
        
        /**
         * Something that can register a listener.
         */
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
     * An abstract implementation of {@link Handler} that abstract away
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

        /**
         * An abstract implementation of {@link AbstractHandler} that abstract away
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
    }
    
    /**
     * An abstract implementation of {@link Listener} that abstract away {@link #name()}.
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
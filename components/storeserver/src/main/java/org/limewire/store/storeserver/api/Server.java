package org.limewire.store.storeserver.api;

import java.io.IOException;
import java.net.Socket;

import org.limewire.store.storeserver.local.LocalLocalServer;

/**
 * This represents an instance of a local server.
 */
public interface Server {

    /**
     * Single instance of {@link Factory}.
     */
    public final static Factory FACTORY = new Factory() {
        public Server newInstance(int port, OpensSocket openner) {
            return new LocalLocalServer("localhost", 8090, openner);
        }
    };
    
    /**
     * Sets the new {@link Dispatchee} instance.
     * 
     * @param d the new {@link Dispatchee} instance
     */
    void setDispatchee(Dispatchee d);

    /**
     * Returns the {@link Dispatchee} instance.
     * 
     * @return the {@link Dispatchee} instance
     */
    Dispatchee getDispatchee();

    /**
     * Starts up the server.
     */
    void start();

    /**
     * Shuts down the server, waiting for <code>millis</code> milliseconds.
     * 
     * @param millis milliseconds to wait before abruptly shutting everything
     *        down
     */
    void shutDown(long millis);

    /**
     * Reports a message.
     * 
     * @param msg message to report
     * @return reponse back to code after reporting <code>msg</code>
     */
    String report(String msg);    

    public interface Factory {

        /**
         * Constructs a new {@link Server} on port <code>port</code>
         * 
         * @param port port on which to construct the server
         * @param openner delegate which opens streams on our behalf
         * @return a new instance of {@link Server}, not started
         */
        Server newInstance(int port, OpensSocket openner);
    }

    /**
     * Something that can open an input stream on behalf of this component. The
     * default implementation would be <code>new URL(url).openConnection()</code>
     * but due to the <em>core's</em> connection manager we'll have to have a
     * way of hooking into that without this component knowing about it.
     * 
     * @see URLSocketOpenner
     */
    public interface OpensSocket {
        
        /**
         * Opens a connection based on the passed in URL <code>url</code>.
         * 
         * @param host  URL to open
         * @param port TODO
         * @return a connection based on the passed in URL <code>url</code>.
         * @throws IOException if an IO error occurs
         */
        Socket open(String host, int port) throws IOException;
    }

    /**
     * Reponses sent back from servers.
     */
    public interface Responses {

        /**
         * Success.
         */
        String OK = "ok";

        /**
         * When there was a command sent to the local host, but no
         * {@link Dispatchee} was set up to handle it.
         */
        String NO_DISPATCHEE = "no.dispatcher";

        /**
         * When there was a {@link Dispatchee} to handle this command, but it
         * didn't understand it.
         */
        String UNKNOWN_COMMAND = "unknown.command";

    }

    /**
     * Collection of all the commands we send.
     */
    public interface Commands {

        /**
         * Sent from Code to Local with no parameters.
         */
        String START_COM = "StartCom";

        /**
         * Sent from Local to Remote with parameters.
         * <ul>
         * <li>{@link Server.Parameters#PUBLIC}</li>
         * <li>{@link Server.Parameters#PRIVATE}</li>
         * </ul>
         */
        String STORE_KEY = "StoreKey";

        /**
         * Sent from Code to Remote with parameters.
         * <ul>
         * <li>{@link Server.Parameters#PRIVATE}</li>
         * </ul>
         */
        String GIVE_KEY = "GiveKey";

        /**
         * Send from Code to Local with no parameters.
         */
        String DETATCH = "Detatch";

        /**
         * Sent from Code to Local with parameters.
         * <ul>
         * <li>{@link Server.Parameters#PRIVATE}</li>
         * </ul>
         */
        String AUTHENTICATE = "Authenticate";

        /* Testing */

        /**
         * Sent from Code to Local with parameters.
         * <ul>
         * <li>{@link Server.Parameters#MSG}</li>
         * </ul>
         */
        String ECHO = "Echo";

        public final static String ALERT = "Alert";
    }

    /**
     * Parameter names.
     */
    public interface Parameters {

        /**
         * Name of the callback function.
         */
        String CALLBACK = "callback";

        /**
         * Private key.
         */
        String PRIVATE = "private";

        /**
         * Public key.
         */
        String PUBLIC = "public";

        /**
         * Name of the command to send to the {@link Dispatchee}.
         */
        String COMMAND = "command";

        /**
         * Message to send to the <tt>ECHO</tt> command.
         */
        String MSG = "msg";

        /**
         * Name of a URL.
         */
        String URL = "url";

    }

    /**
     * Codes that are sent to the code (javascript) when an error occurs.
     */
    public interface ErrorCodes {

        /**
         * Indicating an invalid public key.
         */
        String INVALID_PUBLIC_KEY = "invalid.public.key";

        /**
         * Indicating an invalid private key.
         */
        String INVALID_PRIVATE_KEY = "invalid.private.key";

        /**
         * Indicating an invalid public key or IP address.
         */
        String INVALID_PUBLIC_KEY_OR_IP = "invalid.public.key.or.ip.address";

        /**
         * Indicating the code has not included a callback parameter.
         */
        String MISSING_CALLBACK_PARAMETER = "missing.callback.parameter";

        /**
         * A command was not understood or did not have valid handler or
         * listener.
         */
        String UNKNOWN_COMMAND = "unkown.command";

        /**
         * No private key has been generated yet.
         */
        String UNITIALIZED_PRIVATE_KEY = "uninitialized.private.key";

        /**
         * No private key parameter was supplied.
         */
        String MISSING_PRIVATE_KEY_PARAMETER = "missing.private.parameter";

        /**
         * No public key parameter was supplied.
         */
        String MISSING_PUBLIC_KEY_PARAMETER = "missing.public.parameter";

        /**
         * No command parameter was supplied to decide on a handler.
         */
        String MISSING_COMMAND_PARAMETER = "missing.command.parameter";

    }

    /**
     * A general place for constants.
     */
    public interface Constants {

        /**
         * The length of the public and private keys generated.
         */
        int KEY_LENGTH = 10; // TODO

        /**
         * Carriage return and line feed.
         */
        String NEWLINE = "\r\n";

        /**
         * The quote used to surround callbacks. We need to escape this in the
         * strings that we pass back to the callback.
         */
        char CALLBACK_QUOTE = '\'';

        /**
         * The String version of {@link #CALLBACK_QUOTE}.
         */
        String CALLBACK_QUOTE_STRING = String.valueOf(CALLBACK_QUOTE);

        /**
         * The callback in which error messages are wrapped.
         */
        String ERROR_CALLBACK = "error";

        /**
         * The string that separates arguments in the {@link Parameters#MSG}
         * argument when the command {@link Parameters#COMMAND} parameter is
         * <tt>Msg</tt>. This is the urlencoded version of <tt>&</tt>,
         * which is <tt>%26</tt>.
         */
        String ARGUMENT_SEPARATOR = "%26";

    }
}

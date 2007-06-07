/**
 * 
 */
package org.limewire.store.server;


import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import com.limegroup.gnutella.store.storeserver.StoreManager;



/**
 * Instances of this class will receive HTTP requests and are responsible to
 * doling them out to listeners.
 * 
 */
abstract class DispatcherSupport implements Dispatcher {
    
    private final static Log LOG = LogFactory.getLog(DispatcherSupport.class);
    private final Map<String, Handler> names2handlers = new HashMap<String, Handler>();
    private Dispatchee dispatchee;
    
    public DispatcherSupport() {
        Handler[] hs = createHandlers();
        LOG.debug("creating " + hs.length + " handler(s)...");
        for (Handler h : hs) {
            this.names2handlers.put(h.name().toLowerCase(), h);
            LOG.debug(" - " + h.name());
        }
    }
    
    // ------------------------------------------------------
    // Interface
    // ------------------------------------------------------
    
    public void handle(HttpRequest httpReq, HttpResponse httpRes, HttpContext c) throws HttpException, IOException {
        String request = httpReq.getRequestLine().getUri();
        final String req = makeFile(request);
        final Handler h = names2handlers.get(req.toLowerCase());
        final String res;
        if (h == null) {
            LOG.error("Couldn't create a handler for " + req);
            res = report(DispatcherSupport.ErrorCodes.UNKNOWN_COMMAND);
        } else {
            LOG.debug("have handler: " + h.name());
            final Map<String, String> args = DispatcherSupport.getArgs(request);
            res = h.handle(args);
        }
        if (res == null) {
            //DispatcherSupport.report(Dispatcher.ErrorCodes.UNKNOWN_COMMAND);
        } else {
            //todo
        }
        
    }
    public final boolean addConnectionListener(ConnectionListener lis) {
        return getDispatchee().addConnectionListener(lis);
    }

    public final boolean removeConnectionListener(ConnectionListener lis) {
        return getDispatchee().removeConnectionListener(lis);
    }  
    
    /* (non-Javadoc)
     * @see org.limewire.store.server.Dispatcher#sendMsgToRemoteServer(java.lang.String, java.util.Map)
     */
    public abstract String sendMsgToRemoteServer(String msg, Map<String, String> args);
    
    /**
     * Override this to create the {@link Handler}s to use.
     * 
     * @return the list of handlers that will take your requests.
     */
    protected abstract Handler[] createHandlers();
    
    /* (non-Javadoc)
     * @see org.limewire.store.server.Dispatcher#setDispatchee(org.limewire.store.server.Dispatchee)
     */
    public final void setDispatchee(Dispatchee dispatchee) {
        this.dispatchee = dispatchee;
    }

    /**
     * Returns the {@link Dispatchee} instance.
     * 
     * @return the {@link Dispatchee} instance
     */
    final Dispatchee getDispatchee() {
        return dispatchee;
    }
    
    /**
     * Create an instance of Handler from the top level name as well as trying a
     * static inner class and calls its {@link Handler#handle()} method.
     */
    final String handle(final String request, final PrintStream out) {
        final String req = makeFile(request);
        final Handler h = names2handlers.get(req.toLowerCase());
        if (h == null) {
            LOG.error("Couldn't create a handler for " + req);
            return report(DispatcherSupport.ErrorCodes.UNKNOWN_COMMAND);
        }
        LOG.debug("have handler: " + h.name());
        final Map<String, String> args = DispatcherSupport.getArgs(request);
        final String res = h.handle(args);
        return res;
    }    

    final void note(final String pattern, final Object... os) {
        if (LOG.isDebugEnabled()) LOG.info(MessageFormat.format(pattern, os));
    }    
   
    /**
     * Wraps the message <tt>error</tt> in the call back
     * {@link Constants.ERROR_CALLBACK}. <br>
     * Example: If the error message is <tt>"You stink!"</tt> the wrapped
     * message would be <tt>error("You stink!")</tt>.
     * 
     * @param error the error message
     * @return the message <tt>error</tt> in the call back
     */
    public static final String report(final String error) {
        return wrapCallback(DispatcherSupport.Constants.ERROR_CALLBACK, Util
                .wrapError(error));
    }
    
    /**
     * Returns the arguments to the right of the <code>?</code>. <br>
     * <code>static</code> for testing
     * 
     * @param request may be <code>null</code>
     * @return the arguments to the right of the <code>?</code>
     */
    static Map<String, String> getArgs(final String request) {
        if (request == null || request.length() == 0) {
            return Util.EMPTY_MAP_OF_STRING_X_STRING;
        }
        final int ihuh = request.indexOf('?');
        if (ihuh == -1) {
            return Util.EMPTY_MAP_OF_STRING_X_STRING;
        }
        final String rest = request.substring(ihuh + 1);
        return Util.parseArgs(rest);
    }

    /**
     * Wraps the message <tt>msg</tt> using callback function
     * <tt>callback</tt>. The msesage is surrounded by
     * {@link Constants.CALLBACK_QUOTE}s and all quotes in the message,
     * {@link Constants.CALLBACK_QUOTE}, are escaped.
     * 
     * @param callback the function in which <tt>msg</tt> is wrapped
     * @param msg the message
     * @return the message <tt>msg</tt> using callback function
     *         <tt>callback</tt>
     */
    protected static final String wrapCallback(final String callback,
            final String msg) {
        if (Util.isEmpty(callback)) {
            return msg;
        } else {
            char q = DispatcherSupport.Constants.CALLBACK_QUOTE;
            String s = DispatcherSupport.Constants.CALLBACK_QUOTE_STRING;
            return callback + "(" + q + msg.replace(s, "\\" + s) + q + ")";
        }
    }

    /**
     * Removes hash and other stuff.
     */
    private String makeFile(final String s) {
        String res = s;
        final char[] cs = { '#', '?' };
        for (char c : cs) {
            final int id = res.indexOf(c);
            if (id != -1)
                res = res.substring(0, id);
        }
        return res;
    }

    /**
     * Something with a name.
     */
    abstract static class HasName {

        private final String name;

        public HasName(final String name) {
            this.name = name;
        }

        public HasName() {
            String n = getClass().getName();
            int ilast;
            ilast = n.lastIndexOf(".");
            if (ilast != -1)
                n = n.substring(ilast + 1);
            ilast = n.lastIndexOf("$");
            if (ilast != -1)
                n = n.substring(ilast + 1);
            this.name = n;
        }

        public final String name() {
            return name;
        }

        protected final String getArg(final Map<String, String> args,
                final String key) {
            final String res = args.get(key);
            return res == null ? "" : res;
        }

    }

    // ------------------------------------------------------------
    // Handlers and Listeners
    // ------------------------------------------------------------
    
    /**
     * Handles commands.
     */
    interface Handler {

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
            boolean register(String cmd, Handler lis);
        }
    }
    
    /**
     * Handles commands, but does NOT return a result.
     */
    interface Listener {

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
        
         interface CanRegister {

            /**
             * Register a listener for the command <tt>cmd</tt>, and returns
             * <tt>true</tt> on success and <tt>false</tt> on failure. There
             * can be only <b>one</b> {@link StoreManager.Handler} for every
             * command.
             * 
             * @param cmd String that invokes this listener
             * @param lis listener
             * @return <tt>true</tt> if we added, <tt>false</tt> for a
             *         problem or if this command is already registered
             */
            boolean register(String cmd, Listener lis);
        }        
    }    
    
    /**
     * Generic base class for {@link Handler}s.
     */
    abstract static class AbstractHandler extends HasName implements Handler {
        protected AbstractHandler(String name) {
            super(name);
        }
        protected AbstractHandler() {
            super();
        }
    }
    
    /**
     * Generic base class for {@link Listener}s.
     */
    abstract static class AbstractListener extends HasName implements Listener {
        protected AbstractListener(String name) {
            super(name);
        }
        protected AbstractListener() {
            super();
        }
    }

    /**
     * A {@link Handler} requiring a callback specified by the
     * parameter {@link Parameters#CALLBACK}.
     */
    protected abstract class HandlerWithCallback extends AbstractHandler {

        public final String handle(final Map<String, String> args) {
            String callback = getArg(args, DispatcherSupport.Parameters.CALLBACK);
            if (callback == null) {
                return report(DispatcherSupport.ErrorCodes.MISSING_CALLBACK_PARAMETER);
            }
            return DispatcherSupport.wrapCallback(callback, handleRest(args));
        }

        /**
         * Returns the result <b>IN PLAIN TEXT</b>. Override this to provide
         * functionality after the {@link Parameters#CALLBACK} argument has been
         * extracted. This method should <b>NOT</b> wrap the result in the
         * callback, nor should it be called from any other method except this
         * abstract class.
         * 
         * @param args original, untouched arguments
         * @return result <b>IN PLAIN TEXT</b>
         */
        protected abstract String handleRest(Map<String, String> args);
    }
    
    /**
     * Something that can open an input stream on behalf of this component. The
     * default implementation would be <code>new URL(url).openConnection()</code>
     * but due to the <em>core's</em> connection manager we'll have to have a
     * way of hooking into that without this component knowing about it.
     * 
     * @see URLSocketOpenner
     */
    interface OpensSocket {
        
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
     * Collection of all the commands we send.
     */
    interface Commands {
    
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
        
        /**
         * Sent from Code to Local with unknown parameters
         */
        String MSG = "Msg";
    
        public final static String ALERT = "Alert";
    }

    /**
     * Parameter names.
     */
    interface Parameters {
    
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
        
        /**
         * IP to store. This is sent in StoreKey from the local server to the
         * remote server, but in the real system will be ignored.
         */
        String IP = "ip";
    
    }

    /**
     * Codes that are sent to the code (javascript) when an error occurs.
     */
    interface ErrorCodes {
    
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
    
        /**
         * No IP was given.  In ths real system this will not be used.
         */
        String MISSING_IP_PARAMETER = "missing.ip.parameter";
    
    }

    /**
     * Reponses sent back from servers.
     */
    interface Responses {
    
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
     * A general place for constants.
     */
   interface Constants {
    
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
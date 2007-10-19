/**
 * 
 */
package org.limewire.lws.server;


import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.limewire.service.ErrorService;

/**
 * Instances of this class will receive HTTP requests and are responsible to
 * doling them out to listeners.  This is abstract so we can have test
 * cases using some of the logic.
 */
public abstract class LWSDispatcherSupport implements LWSDispatcher {
    
    private final static Log LOG = LogFactory.getLog(LWSDispatcherSupport.class);
    private final Map<String, Handler> names2handlers = new HashMap<String, Handler>();
    private ReceivesCommandsFromDispatcher dispatchee;
    
    public LWSDispatcherSupport() {
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
    
    public void handle(HttpRequest httpReq, final HttpResponse res, HttpContext c) throws HttpException, IOException {
        String request = httpReq.getRequestLine().getUri();
        final String req = getOnlytheFileRequestPortionOfURL(request);
        final Handler h = names2handlers.get(req.toLowerCase());
        if (h == null) {
            LOG.error("Couldn't create a handler for " + req);
            String str = report(LWSDispatcherSupport.ErrorCodes.UNKNOWN_COMMAND);
            res.setEntity(new StringEntity(str));
            return;
        }
        LOG.debug("have handler: " + h.name());
        final Map<String, String> args = LWSDispatcherSupport.getArgs(request);
        h.handle(args, new StringCallback() {

            public void process(String response) {
                try {
                    res.setEntity(new StringEntity(response));
                } catch (UnsupportedEncodingException e) {
                    ErrorService.error(e);
                }
            }
            
        });
    }
    
    public final boolean addConnectionListener(ConnectionListener lis) {
        return getDispatchee().addConnectionListener(lis);
    }

    public final boolean removeConnectionListener(ConnectionListener lis) {
        return getDispatchee().removeConnectionListener(lis);
    }
    
    /**
     * Override this to create the {@link Handler}s to use.
     * 
     * @return the list of handlers that will take your requests.
     */
    protected abstract Handler[] createHandlers();
    
    /* (non-Javadoc)
     * @see org.limewire.store.server.Dispatcher#setDispatchee(org.limewire.store.server.Dispatchee)
     */
    public final void setDispatchee(ReceivesCommandsFromDispatcher dispatchee) {
        this.dispatchee = dispatchee;
    }

    /**
     * Returns the {@link ReceivesCommandsFromDispatcher} instance.
     * 
     * @return the {@link ReceivesCommandsFromDispatcher} instance
     */
    final ReceivesCommandsFromDispatcher getDispatchee() {
        return dispatchee;
    }
    
    /**
     * Create an instance of Handler from the top level name as well as trying a
     * static inner class and calls its {@link Handler#handle()} method.
     */
    final void handle(String request,  PrintStream out, StringCallback callback) {
        final String req = getOnlytheFileRequestPortionOfURL(request);
        final Handler h = names2handlers.get(req.toLowerCase());
        if (h == null) {
            LOG.error("Couldn't create a handler for " + req);
            callback.process(report(LWSDispatcherSupport.ErrorCodes.UNKNOWN_COMMAND));
        }
        LOG.debug("have handler: " + h.name());
        final Map<String, String> args = LWSDispatcherSupport.getArgs(request);
        h.handle(args, callback);
    }    

    final void note(String pattern, Object... os) {
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
    public static final String report(String error) {
        return wrapCallback(LWSDispatcherSupport.Constants.ERROR_CALLBACK, LWSServerUtil.wrapError(error));
    }
    
    /**
     * Returns the arguments to the right of the <code>?</code>. <br>
     * <code>static</code> for testing
     * 
     * @param request may be <code>null</code>
     * @return the arguments to the right of the <code>?</code>
     */
    static Map<String, String> getArgs(String request) {
        if (request == null || request.length() == 0) {
            return Collections.emptyMap();
        }
        int ihuh = request.indexOf('?');
        if (ihuh == -1) {
            return Collections.emptyMap();
        }
        final String rest = request.substring(ihuh + 1);
        return LWSServerUtil.parseArgs(rest);
    }

    /**
     * Wraps the message <tt>msg</tt> using callback function
     * <tt>callback</tt>. The msesage is surrounded by
     * {@link Constants.CALLBACK_QUOTE}s and all quotes in the message,
     * {@link Constants.CALLBACK_QUOTE}, are escaped.
     * 
     * @param callback the function in which <tt>msg</tt> is wrapped, this can
     *        be <code>null</code>
     * @param msg the message
     * @return the message <tt>msg</tt> using callback function
     *         <tt>callback</tt>
     */
    protected static final String wrapCallback(final String callback,
            final String msg) {
        if (LWSServerUtil.isEmpty(callback)) {
            return msg;
        } else {
            char q = LWSDispatcherSupport.Constants.CALLBACK_QUOTE;
            String s = LWSDispatcherSupport.Constants.CALLBACK_QUOTE_STRING;
            return callback + "(" + q + msg.replace(s, "\\" + s) + q + ")";
        }
    }

    /**
     * Removes hash and other stuff.
     */
    private String getOnlytheFileRequestPortionOfURL(final String s) {
        int iprefix = s.indexOf(PREFIX);
        String res = iprefix == -1 ? s : s.substring(iprefix + PREFIX.length());
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
         * @param callback TODO
         */
        void handle(Map<String, String> args, StringCallback callback);

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
        protected String report(String msg) {
            return LWSDispatcherSupport.report(msg);
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

        public final void handle(final Map<String, String> args, final StringCallback cb) {
            final String callback = args.get(LWSDispatcherSupport.Parameters.CALLBACK);
            if (callback == null) {
                cb.process(report(LWSDispatcherSupport.ErrorCodes.MISSING_CALLBACK_PARAMETER));
                return;
            }
            //
            // We want to make sure to check if the result is an error.  In which case
            // we want to wrap it in the error callback, rather than the normal one
            //
            handleRest(args, new StringCallback() {

                public void process(String res) {
                    String str;
                    if (LWSServerUtil.isError(res)) {
                        str = LWSDispatcherSupport.wrapCallback(Constants.ERROR_CALLBACK, res);
                    } else {
                        str = LWSDispatcherSupport.wrapCallback(callback, res);
                    }  
                    cb.process(str);
                }
                
            });
        }

        /**
         * Returns the result <b>IN PLAIN TEXT</b>. Override this to provide
         * functionality after the {@link Parameters#CALLBACK} argument has been
         * extracted. This method should <b>NOT</b> wrap the result in the
         * callback, nor should it be called from any other method except this
         * abstract class.
         * 
         * <br/><br/>
         * 
         * Instances of this class
         * must not use {@link #report(String)}, and <b>must</b> only pass back
         * error codes from {@link ErrorCodes}.  To ensure that {@link #report(String)}
         * is implemented to throw a {@link RuntimeException}.
         * 
         * @param args original, untouched arguments
         * @return result <b>IN PLAIN TEXT</b>
         */
        protected abstract void handleRest(Map<String, String> args, StringCallback callback);
        
        /**
         * Overrides {@link AbstractHandler#report(String)} by simply wrapping
         * the error with the error prefix as defined in {@link LWSServerUtil#wrapError(String)}
         * so that we don't wrap it in a callback.
         */
        protected final String report(String error) {
            return LWSServerUtil.wrapError(error);
        }
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
    public interface Commands {
    
        /**
         * Sent from Code to Local with no parameters.
         */
        String START_COM = "StartCom";
    
        /**
         * Sent from Local to Remote with parameters.
         * <ul>
         * <li>{@link LocalServer.Parameters#PUBLIC}</li>
         * <li>{@link LocalServer.Parameters#PRIVATE}</li>
         * </ul>
         */
        String STORE_KEY = "StoreKey";
    
        /**
         * Sent from Code to Remote with parameters.
         * <ul>
         * <li>{@link LocalServer.Parameters#PRIVATE}</li>
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
         * <li>{@link LocalServer.Parameters#PRIVATE}</li>
         * </ul>
         */
        String AUTHENTICATE = "Authenticate";
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
         * Name of the command to send to the {@link ReceivesCommandsFromDispatcher}.
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
    
        /**
         * No IP was given.  In ths real system this will not be used.
         */
        String MISSING_IP_PARAMETER = "missing.ip.parameter";
    
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
         * {@link ReceivesCommandsFromDispatcher} was set up to handle it.
         */
        String NO_DISPATCHEE = "no.dispatcher";
    
        /**
         * When there was a {@link ReceivesCommandsFromDispatcher} to handle this command, but it
         * didn't understand it.
         */
        String UNKNOWN_COMMAND = "unknown.command";
    
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
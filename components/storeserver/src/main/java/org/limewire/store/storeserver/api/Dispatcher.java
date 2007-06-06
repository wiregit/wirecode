/**
 * 
 */
package org.limewire.store.storeserver.api;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.store.storeserver.core.Request;
import org.limewire.store.storeserver.util.Util;

/**
 * Instances of this class will receive HTTP requests and are responsible to
 * doling them out to listeners.
 * 
 */
public abstract class Dispatcher {
    
    private final static Log LOG = LogFactory.getLog(Dispatcher.class);
    private Dispatchee dispatchee;
    
    public Dispatcher() {
        Dispatcher.Handler[] hs = createHandlers();
        LOG.debug("creating " + hs.length + " handler(s)...");
        for (Dispatcher.Handler h : hs) {
            this.names2handlers.put(h.name().toLowerCase(), h);
            LOG.debug(" - " + h.name());
        }
    }
    
    /**
     * Sets the new {@link Dispatchee} instance.
     * 
     * @param d the new {@link Dispatchee} instance
     */
    public final void setDispatchee(Dispatchee dispatchee) {
        this.dispatchee = dispatchee;
    }

    /**
     * Returns the {@link Dispatchee} instance.
     * 
     * @return the {@link Dispatchee} instance
     */
    public final Dispatchee getDispatchee() {
        return dispatchee;
    }
    
    /**
     * Create an instance of Handler from the top level name as well as trying a
     * static inner class and calls its {@link Handler#handle()} method.
     */
    public String handle(final String request, final PrintStream out, Request incoming) {
        final String req = makeFile(request);
        final Dispatcher.Handler h = names2handlers.get(req.toLowerCase());
        if (h == null) {
            LOG.error("Couldn't create a handler for " + req);
            return report(Server.ErrorCodes.UNKNOWN_COMMAND);
        }
        LOG.debug("have handler: " + h.name());
        final Map<String, String> args = Dispatcher.getArgs(request);
        final String res = h.handle(args, incoming);
        // note("response from {0}: {1}", h.name(), res);
        return res;
    }


    
    /**
     * Send a message.
     * 
     * @param args
     */
    public abstract String sendMsg(String msg, Map<String, String> args);
    

    public final void note(final String pattern, final Object... os) {
        if (LOG.isDebugEnabled()) LOG.info(MessageFormat.format(pattern, os));
    }    

    /**
     * Override this to create the {@link Dispatcher.Handler}s to use.
     * 
     * @return the list of handlers that will take your requests.
     */
    protected abstract Dispatcher.Handler[] createHandlers();

    private final Map<String, Dispatcher.Handler> names2handlers = new HashMap<String, Dispatcher.Handler>();



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
        return wrapCallback(Server.Constants.ERROR_CALLBACK, Util
                .wrapError(error));
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
            char q = Server.Constants.CALLBACK_QUOTE;
            String s = Server.Constants.CALLBACK_QUOTE_STRING;
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
     * Returns the arguments to the right of the <code>?</code>. <br>
     * <code>static</code> for testing
     * 
     * @param request may be <code>null</code>
     * @return the arguments to the right of the <code>?</code>
     */
    public static Map<String, String> getArgs(final String request) {
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
     * Something with a name.
     */
    abstract class HasName {

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

    /**
     * Generic base class for {@link Dispatcher.Listener}s.
     */
    public abstract class AbstractListener extends HasName implements
            Dispatcher.Listener {
        protected AbstractListener(String name) {
            super(name);
        }

        protected AbstractListener() {
            super();
        }
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
         * @param args CGI params
         * @param req incoming {@link Request}
         * @return the result of performing some operation on the incoming
         *         message
         */
        String handle(Map<String, String> args, Request req);

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
         * @param args CGI params
         * @param req incoming {@link Request}
         */
        void handle(Map<String, String> args, Request req);

        /**
         * Returns the unique name of this instance.
         * 
         * @return the unique name of this instance
         */
        String name();
    }

    /**
     * Generic base class for {@link Dispatcher.Handler}s.
     */
    public abstract class AbstractHandler extends HasName implements
            Dispatcher.Handler {
        protected AbstractHandler(String name) {
            super(name);
        }

        protected AbstractHandler() {
            super();
        }
    }

    /**
     * A {@link Dispatcher.Handler} requiring a callback specified by the
     * parameter {@link Parameters#CALLBACK}.
     */
    protected abstract class HandlerWithCallback extends AbstractHandler {

        public final String handle(final Map<String, String> args, Request req) {
            String callback = getArg(args, Server.Parameters.CALLBACK);
            if (callback == null) {
                return Dispatcher
                        .report(Server.ErrorCodes.MISSING_CALLBACK_PARAMETER);
            }
            return Dispatcher.wrapCallback(callback, handleRest(args, req));
        }

        /**
         * Returns the result <b>IN PLAIN TEXT</b>. Override this to provide
         * functionality after the {@link Parameters#CALLBACK} argument has been
         * extracted. This method should <b>NOT</b> wrap the result in the
         * callback, nor should it be called from any other method except this
         * abstract class.
         * 
         * @param args original, untouched arguments
         * @param req originating {@link Request} object
         * @return result <b>IN PLAIN TEXT</b>
         */
        protected abstract String handleRest(Map<String, String> args, Request req);
    }

}
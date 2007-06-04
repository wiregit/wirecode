package org.limewire.store.storeserver.core;

import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ManagedThread;
import org.limewire.service.ErrorService;
import org.limewire.store.storeserver.api.Server;
import org.limewire.store.storeserver.util.Util;

/**
 * Base class for servers, both local and remote.
 */
public abstract class AbstractServer implements Runnable  {

    private static final Log LOG = LogFactory.getLog(AbstractServer.class);
    private final static int NUM_WORKERS = 5;
    private static final byte[] NEWLINE = { (byte) '\r', (byte) '\n' };
    private final int port;
    private final String name;

    private boolean done = false;

    private final List<Thread> threads = new ArrayList<Thread>(NUM_WORKERS);
    private final List<Worker> workers = new ArrayList<Worker>(NUM_WORKERS);
    private final Map<String, Handler> names2handlers = new HashMap<String, Handler>();

    private boolean hasShutDown;
    private Thread runner;

    // --------------------------------------------------------
    // Interface
    // --------------------------------------------------------

    /**
     * Returns and starts a {@link Thread} for <tt>s</tt>.
     * 
     * @param s the server in questions
     * @return a {@link Thread} for <tt>s</tt>
     */
    public static Thread start(final AbstractServer s) {
        Thread t = new ManagedThread(s);
        t.start();
        s.runner = t;
        return t;
    }

    public AbstractServer(final int port, final String name) {
        this.port = port;
        this.name = name;
        Handler[] hs = createHandlers();
        note("creating {0} handler(s)...", hs.length);
        for (Handler h : hs) {
            this.names2handlers.put(h.name().toLowerCase(), h);
            note(" - {0}", h.name());
        }
        note("connecting on port {0}", port);
    }

    /**
     * Send a message.
     * 
     * @param args
     */
    public abstract String sendMsg(String msg, Map<String, String> args);

    /**
     * Main entry point.
     */
    public final void run() {
        this.hasShutDown = false;
        noteRun();
        createWorkers();
        go();
    }

    public final void setDone(final boolean done) {
        this.done = done;
    }

    public final boolean isDone() {
        return this.done;
    }

    public final int getPort() {
        return port;
    }

    public final String getName() {
        return name;
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
    public final String report(final String error) {
        return wrapCallback(Server.Constants.ERROR_CALLBACK, Util
                .wrapError(error));
    }
    
    /**
     * Attempts to join this thread and then set Done to <tt>true</tt>.
     * 
     * @param millis milliseconds to wait for a join
     */
    public final void shutDown(final long millis) {
        note("shutting down");
        if (hasShutDown)
            return;
        hasShutDown = true;
        setDone(true);
        for (Iterator<Thread> it = threads.iterator(); it.hasNext();) {
            stop(it.next(), millis);
        }
        stop(runner, millis);
        runner = null;
    }    

    // --------------------------------------------------------
    // Interface to subclasses
    // --------------------------------------------------------

    protected final void handle(final Throwable t) {
        ErrorService.error(t);
    }

    protected final void handle(final Throwable t, final String msg) {
        ErrorService.error(t, msg);
    }

    /**
     * Set by {@link #start(AbstractServer)}.
     */
    protected final Thread getRunner() {
        return runner;
    }

    /**
     * Override this to know when we're starting.
     */
    protected void noteRun() {
    }

    /**
     * Override this to create the {@link Handler}s to use.
     * 
     * @return the list of handlers that will take your requests.
     */
    protected abstract Handler[] createHandlers();

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
    protected final String wrapCallback(final String callback, final String msg) {
        if (Util.isEmpty(callback)) {
            return msg;
        } else {
            char q = Server.Constants.CALLBACK_QUOTE;
            String s = Server.Constants.CALLBACK_QUOTE_STRING;
            return callback + "(" + q + msg.replace(s, "\\" + s) + q + ")";
        }
    }
    
    /**
     * Only set when someone has called {@link #shutDown(long)} to differentiate
     * between forced and accidental shutdowns.
     */
    protected final boolean hasShutDown() {
        return this.hasShutDown;
    }

    // --------------------------------------------------------
    // Private
    // --------------------------------------------------------

    private void createWorkers() {
        for (int i = 0; i < NUM_WORKERS; ++i) {
            addNewThread(new Worker(), "store server worker #" + i);
        }
    }

    private void go() {
        try {
            final ServerSocket ss = new ServerSocket(port);
            while (!isDone()) {
                Socket s = ss.accept();
                synchronized (workers) {
                    if (workers.isEmpty()) {
                        Worker ws = new Worker();
                        ws.setSocket(s);
                        addNewThread(ws, "store server additional worker");
                    } else {
                        Worker w = workers.get(0);
                        workers.remove(0);
                        w.setSocket(s);
                    }
                }
            }
        } catch (IOException e) {
            handle(e);
            if (e instanceof java.net.BindException)
                setDone(true);
        }
        shutDown();
    }

    private void addNewThread(Worker w, String name) {
        Thread t = new ManagedThread(w, name);
        threads.add(t);
        workers.add(w);
        t.start();
    }

    class Worker implements Runnable {

        final static int BUF_SIZE = 2048;

        /**
         * buffer to use for requests
         */
        private final byte[] buf = new byte[BUF_SIZE];

        /**
         * Socket to client we're handling
         */
        private Socket s;

        synchronized void setSocket(Socket s) {
            this.s = s;
            notify();
        }

        public synchronized void run() {
            while (true) {
                if (s == null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        /* should not happen */
                        continue;
                    }
                }
                try {
                    handleClient();
                } catch (IOException e) {
                    LOG.debug("handleClient", e);
                }
                /*
                 * go back in wait queue if there's fewer than numHandler
                 * connections.
                 */
                s = null;
                synchronized (AbstractServer.this.workers) {
                    if (AbstractServer.this.workers.size() >= AbstractServer.NUM_WORKERS) {
                        return;
                    } else {
                        AbstractServer.this.workers.add(this);
                    }
                }
            }
        }

        private void handleClient() throws IOException {
            InputStream is = new BufferedInputStream(s.getInputStream());
            PrintStream ps = new PrintStream(s.getOutputStream(), true);
            /*
             * we will only block in read for this many milliseconds before we
             * fail with java.io.InterruptedIOException, at which point we will
             * abandon the connection.
             */
            /* zero out the buffer from last time */
            for (int i = 0; i < BUF_SIZE; i++) {
                buf[i] = 0;
            }
            try {
                /*
                 * We only support HTTP GET/HEAD, and don't support any fancy
                 * HTTP options, so we're only interested really in the first
                 * line.
                 */
                int nread = 0, r = 0;

                outerloop: while (nread < BUF_SIZE) {
                    r = is.read(buf, nread, BUF_SIZE - nread);
                    if (r == -1) {
                        /* EOF */
                        return;
                    }
                    int i = nread;
                    nread += r;
                    for (; i < nread; i++) {
                        if (buf[i] == (byte) '\n' || buf[i] == (byte) '\r') {
                            /* read one line */
                            break outerloop;
                        }
                    }
                }

                /* are we doing a GET or just a HEAD */
                /* beginning of file name */
                int index;
                if (buf[0] == (byte) 'G' && buf[1] == (byte) 'E'
                        && buf[2] == (byte) 'T' && buf[3] == (byte) ' ') {
                    index = 4;
                } else if (buf[0] == (byte) 'H' && buf[1] == (byte) 'E'
                        && buf[2] == (byte) 'A' && buf[3] == (byte) 'D'
                        && buf[4] == (byte) ' ') {
                    index = 5;
                } else {
                    /* we don't support this method */
                    ps.print("HTTP/1.1 " + HttpURLConnection.HTTP_BAD_METHOD + " unsupported method type: ");
                    ps.write(buf, 0, 5);
                    ps.write(NEWLINE);
                    ps.flush();
                    s.close();
                    return;
                }

                int i = 0;
                /*
                 * find the file name, from: GET /foo/bar.html HTTP/1.0 extract
                 * "/foo/bar.html"
                 */
                for (i = index; i < nread; i++) {
                    if (buf[i] == (byte) ' ') {
                        break;
                    }
                }
                String request = (new String(buf, 0, index, i - index)).replace('/', File.separatorChar);
                if (request.startsWith(File.separator)) {
                    request = request.substring(1);
                }
                final String ip = Util.getIPAddress(s.getInetAddress());
                final Request incoming = new Request(ip);
                String res = handle(request, ps, incoming);
                ps.print("HTTP/1.1 ");
                ps.print(HttpURLConnection.HTTP_OK);
                ps.print(" OK");
                ps.write(NEWLINE);
                ps.print("Last-modified: ");
                println(ps, Util.createCookieDate()); // todo wrong
                ps.print("Server: ");
                ps.print(getName());
                ps.write(NEWLINE);
                ps.print("Date: " + new Date());
                ps.write(NEWLINE);
                ps.print("Content-length: ");
                println(ps, String.valueOf(res.length()));
                ps.write(NEWLINE);
                ps.print(res);
            } finally {
                s.close();
            }
        }

    }

    private void println(final PrintStream o, final String line)
            throws IOException {
        o.print(line);
        o.write(NEWLINE);
    }

    /**
     * Create an instance of Handler from the top level name as well as trying a
     * static inner class and calls its {@link Handler#handle()} method.
     */
    private String handle(final String request, final PrintStream out,
            Request incoming) {
        final String req = makeFile(request);
        final Handler h = names2handlers.get(req.toLowerCase());
        if (h == null) {
            LOG.error("Couldn't create a handler for " + req);
            return report(Server.ErrorCodes.UNKNOWN_COMMAND);
        }
        note("have handler: {0}", h.name());
        final String res = h.handle(getArgs(request), incoming);
        note("response from {0}: {1}", h.name(), res);
        return res;
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




    private void stop(final Thread t, final long millis) {
        synchronized (t) {
            try {
                t.join(millis);
            } catch (InterruptedException e) {
                LOG.debug("trying to stop: " + t, e);
            }
        }
    }

    /**
     * Calls {@link #shutDown(long)} with <tt>1000</tt>.
     */
    public final void shutDown() {
        shutDown(1000);
    }

    public final void note(final String pattern, final Object... os) {
        if (LOG.isDebugEnabled()) LOG.debug(MessageFormat.format(pattern, os));
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
     * Generic base class for {@link Listener}s.
     */
    public abstract class AbstractListener extends HasName implements Listener {
        protected AbstractListener(String name) {
            super(name);
        }

        protected AbstractListener() {
            super();
        }
    }

    /**
     * Generic base class for {@link Handler}s.
     */
    public abstract class AbstractHandler extends HasName implements Handler {
        protected AbstractHandler(String name) {
            super(name);
        }

        protected AbstractHandler() {
            super();
        }
    }

    /**
     * A {@link Handler} requiring a callback specified by the parameter
     * {@link Parameters#CALLBACK}.
     */
    protected abstract class HandlerWithCallback extends AbstractHandler {

        public final String handle(final Map<String, String> args, Request req) {
            String callback = getArg(args, Server.Parameters.CALLBACK);
            if (callback == null) {
                return report(Server.ErrorCodes.MISSING_CALLBACK_PARAMETER);
            }
            return wrapCallback(callback, handleRest(args, req));
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
        abstract String handleRest(Map<String, String> args, Request req);
    }
}

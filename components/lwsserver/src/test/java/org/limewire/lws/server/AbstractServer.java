package org.limewire.lws.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ManagedThread;
import org.limewire.service.ErrorService;

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

    private final List<Thread> threads = new Vector<Thread>(NUM_WORKERS);
    private final List<Worker> workers = new Vector<Worker>(NUM_WORKERS);
    
    private LWSDispatcher dispatcher;

    private boolean hasShutDown;
    private Thread runner;

    // --------------------------------------------------------
    // Interface
    // --------------------------------------------------------

    public AbstractServer(final int port, final String name, final LWSDispatcherSupport dispatcher) {
        this.port = port;
        this.name = name;
        this.dispatcher = dispatcher;
        LOG.debug(name + " on port " + port);
    }
    
    public AbstractServer(final int port, final String name) {
        this(port, name, null);        
    }
    
    public final Thread start() {
        Thread t = new ManagedThread(this);
        t.start();
        this.runner = t;
        return t;
    }

    /**
     * Main entry point.
     */
    public final void run() {
        this.hasShutDown = false;
        noteRun();
        createWorkers();
        go();
    }
    
    public final LWSDispatcherSupport getDispatcher() {
        return (LWSDispatcherSupport)this.dispatcher;
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
    
    public final String report(String error) {
        return LWSDispatcherSupport.report(error);
    }
    
    /**
     * Attempts to join this thread and then set Done to <code>true</code>.
     * 
     * @param millis milliseconds to wait for a join
     */
    public final void shutDown(final long millis) {
        getDispatcher().note("shutting down");
        if (hasShutDown) return;
        hasShutDown = true;
        setDone(true);
        for (int i=0, N=threads.size(); i<N; i++) {
            stop(threads.get(i), millis);
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
     * If subclasses can't pass a dispatcher in to the super constructor
     * because it needs to know about the server, you <b>must</b> call this
     * in the constructor.
     * 
     * @param dispatcher new {@link LWSDispatcherSupport}
     */
    protected final void setDispatcher(LWSDispatcher dispatcher) {
        this.dispatcher = dispatcher;
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
            ServerSocket tmpSocket = null;
            int tmpPort = port;
            for (; tmpPort < port+10; tmpPort++) {
                try {
                    tmpSocket = new ServerSocket(tmpPort);
                } catch (IOException ignored) { }
                if (tmpSocket != null) break;
            }
            final ServerSocket ss = tmpSocket;
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() {
                    shutDown();
                }
            }));
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
            handle(e, getClass() + " on port " + port);
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
            final PrintStream ps = new PrintStream(s.getOutputStream(), true);
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
                final String ip = LWSServerUtil.getIPAddress(s.getInetAddress());
                //
                // add the ip address
                //
                if (request.indexOf("?") == -1) {
                    request += "?";
                } else {
                    request += "&";
                }
                request += "ip=" + ip;
                getDispatcher().handle(request, ps, new StringCallback() {

                    public void process(String res) {
                        ps.print("HTTP/1.1 ");
                        ps.print(HttpURLConnection.HTTP_OK);
                        ps.print(" OK");
                        try {
                            ps.write(NEWLINE);
                            ps.print("Last-modified: ");
                            println(ps, LWSServerUtil.createCookieDate()); // todo wrong
                            ps.print("Server: ");
                            ps.print(getName());
                            ps.write(NEWLINE);
                            ps.print("Date: " + new Date());
                            ps.write(NEWLINE);
                            ps.print("Content-length: ");
                            println(ps, String.valueOf(res.length()));
                            ps.write(NEWLINE);
                            ps.print(res);
                        } catch (IOException e) {
                            ErrorService.error(e);
                        }
                    }
                    
                });

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
     * Calls {@link #shutDown(long)} with <code>1000</code>.
     */
    public final void shutDown() {
        shutDown(100);
    }

}

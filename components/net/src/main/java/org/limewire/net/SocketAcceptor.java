package org.limewire.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.nio.SocketFactory;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.AcceptObserver;

/**
 * Listens on an HTTP port, accepts incoming connections, and dispatches threads
 * to handle requests. This allows simple HTTP requests.
 */
public class SocketAcceptor {

    /**
     * The socket that listens for incoming connections. Can be changed to
     * listen to new ports.
     * 
     * LOCKING: obtain _socketLock before modifying either. Notify _socketLock
     * when done.
     */
    private volatile ServerSocket listeningSocket = null;

    private final ConnectionDispatcher dispatcher = new ConnectionDispatcher();

    private int listeningPort = -1;

    private volatile boolean localOnly;

    public SocketAcceptor() {
    }
    
    public boolean isLocalOnly() {
        return localOnly;
    }

    public void setLocalOnly(boolean localOnly) {
        this.localOnly = localOnly;
    }

    /**
     * @requires only one thread is calling this method at a time
     * @modifies this
     * @effects sets the port on which the ConnectionManager is listening. If
     *          that fails, this is <i>not</i> modified and IOException is
     *          thrown. If port==0, tells this to stop listening to incoming
     *          connections. This is properly synchronized and can be called
     *          even while run() is being called.
     */
    public synchronized void setPort(int port) throws IOException {
        // 1. Special case: if unchanged, do nothing.
        if (this.listeningSocket != null && this.listeningPort == port) {
            return;
        }
        // 2. Special case if port==0. This ALWAYS works.
        // Note that we must close the socket BEFORE grabbing
        // the lock. Otherwise deadlock will occur since
        // the acceptor thread is listening to the socket
        // while holding the lock. Also note that port
        // will not have changed before we grab the lock.
        else if (port == 0) {
            IOUtils.close(listeningSocket);
            this.listeningSocket = null;
            this.listeningPort = 0;
        }
        // 3. Normal case. See note about locking above.
        else {
            // a) Try new port.
            ServerSocket newSocket = null;
            try {
                newSocket = SocketFactory.newServerSocket(port,
                        new SocketListener());
            } catch (IOException e) {
                throw e;
            }

            // b) Close old socket
            IOUtils.close(listeningSocket);

            // c) Replace with new sock.
            this.listeningSocket = newSocket;
            this.listeningPort = port;
        }
    }

    /**
     * Return the listening port.
     */
    public synchronized int getPort() {
        return listeningPort;
    }

    /**
     * Returns the dispatcher that dispatches incoming requests to a handler.
     */
    public ConnectionDispatcher getDispatcher() {
        return dispatcher;
    }

    /** Dispatches sockets to a thread that'll handle them. */
    private class SocketListener implements AcceptObserver {

        public void handleIOException(IOException ignored) {
        }

        public void shutdown() {
        }

        public void handleAccept(Socket client) {
            // only allow local connections
            if (isLocalOnly() && !NetworkUtils.isLocalHost(client)) {
                IOUtils.close(client);
                return;
            }

            // dispatch asynchronously if possible
            if (client instanceof NIOMultiplexor) {
                ((NIOMultiplexor) client)
                        .setReadObserver(new AsyncConnectionDispatcher(
                                dispatcher, client, null));
            } else {
                ThreadExecutor.startThread(new BlockingConnectionDispatcher(
                        dispatcher, client, null),
                        "BlockingConnectionDispatchRunner");
            }
        }
    }

}

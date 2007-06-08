package com.limegroup.gnutella.browser;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkUtils;
import org.limewire.nio.SocketFactory;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.AcceptObserver;
import org.limewire.service.MessageService;

import com.limegroup.gnutella.AsyncConnectionDispatcher;
import com.limegroup.gnutella.BlockingConnectionDispatcher;
import com.limegroup.gnutella.ConnectionAcceptor;
import com.limegroup.gnutella.ConnectionDispatcher;

/**
 * Listens on an HTTP port, accepts incoming connections, and dispatches threads
 * to handle requests. This allows simple HTTP requests.
 */
public class LocalAcceptor {

    /**
     * The socket that listens for incoming connections. Can be changed to
     * listen to new ports.
     * 
     * LOCKING: obtain _socketLock before modifying either. Notify _socketLock
     * when done.
     */
    private volatile ServerSocket listeningSocket = null;

    private int listeningPort = 45100;

    private ConnectionDispatcher dispatcher = new ConnectionDispatcher();

    /**
     * Starts listening to incoming connections.
     */
    public void start() {
        dispatcher.addConnectionAcceptor(new ConnectionAcceptor() {
            public void acceptConnection(String word, Socket socket) {
                ExternalControl.fireControlThread(socket, true);
            }
        }, new String[] { "MAGNET" }, true, true);
        
        dispatcher.addConnectionAcceptor(new ConnectionAcceptor() {
            public void acceptConnection(String word, Socket socket) {
                ExternalControl.fireControlThread(socket, false);
            }
        }, new String[] { "TORRENT" }, true, true);

        // Create the server socket, bind it to a port, and listen for
        // incoming connections. If there are problems, we can continue
        // onward.
        // 1. Try suggested port.
        try {
            setListeningPort(listeningPort);
        } catch (IOException e) {
            boolean error = true;
            // 2. Try 20 different consecutive ports from 45100 to 45119
            // (inclusive)
            // no longer random, since this listening socket is used by the
            // executable stub
            // to launch magnet downloads, so the stub needs to know what
            // (small) range of
            // ports to try...
            for (int i = 1; i < 20; i++) {
                listeningPort = i + 45100;
                try {
                    setListeningPort(listeningPort);
                    error = false;
                    break;
                } catch (IOException ignored) {
                }
            }

            if (error) {
                MessageService.showError("ERROR_NO_PORTS_AVAILABLE");
            }
        }
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
    private void setListeningPort(int port) throws IOException {
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
    public int getPort() {
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
            if (!NetworkUtils.isLocalHost(client)) {
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
                        "LocalConnectionDispatchRunner");
            }
        }
    }

}

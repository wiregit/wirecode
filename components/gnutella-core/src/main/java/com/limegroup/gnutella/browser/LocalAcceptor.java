package com.limegroup.gnutella.browser;

import java.io.IOException;
import java.net.Socket;

import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.SocketAcceptor;
import org.limewire.service.MessageService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Listens on an HTTP port, accepts incoming connections, and dispatches threads
 * to handle requests. This allows simple HTTP requests.
 */
@Singleton
public class LocalAcceptor {

    private int listeningPort = 45100;

    private final ExternalControl externalControl;

    private SocketAcceptor acceptor;

    @Inject
    public LocalAcceptor(ExternalControl externalControl, @Named("local") ConnectionDispatcher connectionDispatcher) {
        this.externalControl = externalControl;
        this.acceptor = new SocketAcceptor(connectionDispatcher);
    }
    
    /**
     * Starts listening to incoming connections.
     */
    public void start() {
        acceptor.getDispatcher().addConnectionAcceptor(new ConnectionAcceptor() {
            public void acceptConnection(String word, Socket socket) {
                externalControl.fireControlThread(socket, true);
            }
        }, true, true, "MAGNET");

        acceptor.getDispatcher().addConnectionAcceptor(new ConnectionAcceptor() {
            public void acceptConnection(String word, Socket socket) {
                externalControl.fireControlThread(socket, false);
            }
        }, true, true, "TORRENT");

        // Create the server socket, bind it to a port, and listen for
        // incoming connections. If there are problems, we can continue
        // onward.
        // 1. Try suggested port.
        try {
            acceptor.bind(listeningPort);
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
                    acceptor.bind(listeningPort);
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
     * Return the listening port.
     */
    public int getPort() {
        return acceptor.getPort();
    }

    /**
     * Returns the dispatcher that dispatches incoming requests to a handler.
     */
    public ConnectionDispatcher getDispatcher() {
        return acceptor.getDispatcher();
    }

}

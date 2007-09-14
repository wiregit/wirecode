package org.limewire.nio.observer;

import java.io.IOException;
import java.net.Socket;

/**
 * Defines the interface that allows accept events to be received.
 */
public interface AcceptObserver extends IOErrorObserver {
    
    /**
     *  Notification that a socket is ready.
     */
    void handleAccept(Socket socket) throws IOException;
}
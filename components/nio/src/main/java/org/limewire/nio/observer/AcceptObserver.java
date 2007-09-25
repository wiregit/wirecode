package org.limewire.nio.observer;

import java.io.IOException;
import java.net.Socket;

/**
 * Allows accept events to be received.
 */
public interface AcceptObserver extends IOErrorObserver {
    
    /**
     *  Notification that a socket.
     */
    void handleAccept(Socket socket) throws IOException;
}
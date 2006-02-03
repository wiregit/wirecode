package com.limegroup.gnutella.io;

import java.net.Socket;
import java.io.IOException;

/**
 * Allows accept events to be received.
 */
public interface AcceptObserver extends IOErrorObserver {
    
    /**
     *  Notification that a socket.
     */
    void handleAccept(Socket socket) throws IOException;
}
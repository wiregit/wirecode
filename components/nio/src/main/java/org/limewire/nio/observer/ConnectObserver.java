package org.limewire.nio.observer;

import java.io.IOException;
import java.net.Socket;

/**
 * Allows connect events to be received.
 */
public interface ConnectObserver extends IOErrorObserver {
    
    /** Notification that connection has finished. */
    void handleConnect(Socket socket) throws IOException;
}
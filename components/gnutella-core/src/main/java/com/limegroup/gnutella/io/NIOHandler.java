package com.limegroup.gnutella.io;

import java.nio.channels.SelectableChannel;
import java.io.IOException;

public interface NIOHandler {
    /** notification that an IOException occurred on the while dispatching NIO. */
    void handleIOException(IOException iox);
    
    /** immediately shuts down the handler & any resources it uses */
    void shutdown();
}
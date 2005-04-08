package com.limegroup.gnutella.io;

import java.nio.channels.SelectableChannel;
import java.io.IOException;

public interface NIOHandler {
    void handleIOException(IOException iox);
}
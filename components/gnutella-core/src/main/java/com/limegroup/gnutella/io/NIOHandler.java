package com.limegroup.gnutella.io;

import java.nio.channels.SelectableChannel;
import java.io.IOException;

interface NIOHandler {
    void handleIOException(IOException iox);
}
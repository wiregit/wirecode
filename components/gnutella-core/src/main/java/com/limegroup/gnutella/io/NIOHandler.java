package com.limegroup.gnutella.io;

import java.nio.channels.SelectableChannel;
import java.io.IOException;

interface NIOHandler {
    
    int interestOps();
    
    void handleIOException(IOException iox);
    
    SelectableChannel getSelectableChannel();
    
}
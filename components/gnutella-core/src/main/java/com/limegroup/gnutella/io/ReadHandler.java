package com.limegroup.gnutella.io;

import java.net.SocketException;

interface ReadHandler extends NIOHandler {
    
    void handleRead();
    
    int getSoTimeout() throws SocketException;
    
}
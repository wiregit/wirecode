package com.limegroup.gnutella.io;

import java.nio.channels.SocketChannel;

interface AcceptHandler extends NIOHandler {
    
    void handleAccept(SocketChannel channel);
    
}
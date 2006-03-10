package com.limegroup.gnutella.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.limegroup.gnutella.io.ConnectObserver;

interface SocketController {

    Socket connect(InetSocketAddress addr, int timeout, ConnectObserver observer) throws IOException;
    boolean removeConnectObserver(ConnectObserver observer);
    int getNumAllowedSockets();
    
}

package org.limewire.nio;
import java.io.IOException;
import java.net.Socket;

import javax.net.SocketFactory;

/** A SocketFactory that also allows you to create unconnected sockets. */
public abstract class ExtendedSocketFactory extends SocketFactory {
    
    /** Returns a new unconnected socket. */
    public abstract Socket createSocket() throws IOException;
    
}

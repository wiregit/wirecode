package com.limegroup.gnutella.io;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Much like SocketChannel, except specified through an interface,
 * so that third-parties can expose the functionality without requiring
 * that all the other final methods of AbstractSelectableChannel are
 * implemented the way it wants.
 */
public interface ConnectableChannel {

    /**
     *  Connects to the remote side.
     *  This will return true if it connected immediately, or false
     *  otherwise.   If it returned false, finishConnect must be called
     *  when the connection completes.
     */
    public boolean connect(SocketAddress remote) throws IOException;
    
    /**
     * Finishes the process of connecting to the remote side.
     * 
     * @return
     * @throws IOException
     */
    public boolean finishConnect() throws IOException;
    
    /** Returns true if this channel is connected. */
    public boolean isConnected();
    
    /** Returns true if the channel is in the process of connecting. */
    public boolean isConnectionPending();
    
    /** Returns the socket associated with this channel. */
    public Socket socket();
}

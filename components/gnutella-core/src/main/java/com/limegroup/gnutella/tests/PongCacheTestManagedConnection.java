package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;
import java.net.*;

/**
 * A stub class whose only purpose is to allow the creation of ManagedConnections
 * outside the Gnutella package (i.e., for testing purposes).  We do not want 
 * public constructors in ManagedConnection, as it is part of the factory 
 * methodology, where "real" instances of ManagedConnection are created by the
 * ConnectionManager.
 */
public class PongCacheTestManagedConnection extends ManagedConnection
{
    public PongCacheTestManagedConnection(String host, int port, 
                                          MessageRouter router)
    {
        super(host, port, router);
    }

    public PongCacheTestManagedConnection(Socket socket, MessageRouter router)
    {
        super(socket, router);
    }

    /**
     * ManagedConnection.loopForMessages is protected, hence we need to have a
     * public function for testing purposes.
     */
    public void loopForMessages() throws IOException
    {
        super.loopForMessages();
    }
}

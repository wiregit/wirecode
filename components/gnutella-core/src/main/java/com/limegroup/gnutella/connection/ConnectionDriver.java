package com.limegroup.gnutella.connection;

import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;


/**
 * Drives message reading and writing along a connection.  Provides the listener
 * to take appropriate actions when the connection is initialized, messages are
 * read, etc.  Typically this just involves delegating to ConnectionManager or
 * MessageRouter.  Use factory method to instantiate.
 */
public class ConnectionDriver implements ConnectionListener {  
    /** Sibling classes in the backend. */
    protected ConnectionManager _manager;
    protected MessageRouter _router;
    protected ActivityCallback _callback;

    protected ConnectionDriver() { }
    public static ConnectionDriver newConnectionDriver() {
        if (SettingsManager.instance().getUseNonBlockingIO())
            return new NonBlockingConnectionDriver();
        else
            return new BlockingConnectionDriver();
    }

    public void initialize(ConnectionManager manager,
                           MessageRouter router,
                           ActivityCallback callback) {
        this._manager=manager;
        this._router=router;
        this._callback=callback;
    }

 
    ///////////////////////////// ConnectionListener Methods //////////////////

    public void initialized(Connection c) { 
        //TODO: will call ConnectionManager when handshaking supports NIO
    }
        
    public void read(Connection c, Message m) {
        if (c instanceof ManagedConnection) {
            //TODO: inline this method.  (This isn't a performance issue; it
            //just doesn't belong in ManagedConnection.)
            ((ManagedConnection)c).handleRead(m);
        }
    }

    public void read(Connection c, BadPacketException error) {
        //ignore
    }

    public void needsWrite(Connection c) {
        //ignore
    }

    public void error(Connection c) {
        if (c instanceof ManagedConnection) {
            //No need to actually unregister the socket; just make sure it's not
            //in the broadcast list.
            ManagedConnection mc=(ManagedConnection)c;
            _manager.remove(mc);
            //_catcher.doneWithMessageLoop(e);  TODO
        }
    }
}

package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;


/**
 * A ConnectionListener that forwards all messages to a delegate.
 * Subclass to add hooks for specific events.
 */
public class DelegateConnectionListener implements ConnectionListener {
    protected final ConnectionListener delegate;

    public DelegateConnectionListener(ConnectionListener delegate) {
        this.delegate=delegate;
    }

    public void initialized(Connection c) {
        delegate.initialized(c);
    }

    public void read(Connection c, Message m) { 
        delegate.read(c, m);
    }

    public void read(Connection c, BadPacketException error) { 
        delegate.read(c, error);
    }

    public void needsWrite(Connection c) { 
        delegate.needsWrite(c);
    }

    public void error(Connection c) { 
        delegate.error(c);
    }
}

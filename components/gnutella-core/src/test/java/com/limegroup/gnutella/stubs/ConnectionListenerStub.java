package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.connection.ConnectionListener;

/**
 * A ConnectionListener does nothing.
 */
public class ConnectionListenerStub implements ConnectionListener {
    public void initialized(Connection c) { }

    public void read(Connection c, Message m) { }

    public void read(Connection c, BadPacketException error) { }

    public void needsWrite(Connection c) { }

    public void error(Connection c) { }
}

package com.limegroup.gnutella.tests.stubs;

import com.limegroup.gnutella.*;

/** A (incomplete!) stub for ConnectionManager. */
public class ConnectionManagerStub extends ConnectionManager {
    public ConnectionManagerStub() {
        super(null, null);
    }

    /** Calls c.close(). */
    public void remove(ManagedConnection c) {
        c.close();
    }
}

package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.*;

/** 
 * A stubbed-out ManagedConnection that does nothing.  Useful for testing, since
 * ManagedConnection has no public-access constructors.  ManagedConnectionStub
 * is in this package instead of com.limegroup.gnutella.stubs because it
 * requires package-access to ManagedConnection.
 */
public class ManagedConnectionStub extends ManagedConnection {
    public ManagedConnectionStub() {
        super("1.2.3.4", 6346, 
              new MessageRouterStub(), 
              new ConnectionManagerStub());
    }

    public void initialize() {
    }
}

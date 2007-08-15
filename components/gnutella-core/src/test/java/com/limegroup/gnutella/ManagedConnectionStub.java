package com.limegroup.gnutella;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.stubs.ConnectionManagerStub;

/** 
 * A stubbed-out ManagedConnection that does nothing.  Useful for testing, since
 * ManagedConnection has no public-access constructors.  ManagedConnectionStub
 * is in this package instead of com.limegroup.gnutella.stubs because it
 * requires package-access to ManagedConnection.
 */
public class ManagedConnectionStub extends ManagedConnection {
    public ManagedConnectionStub() {
        super("1.2.3.4", 6346);
		
        try {
            PrivilegedAccessor.setValue(this, "_manager", new ConnectionManagerStub());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public void initialize() {
    }
}

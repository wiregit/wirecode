package com.limegroup.gnutella;

import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.util.PrivilegedAccessor;

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
            //PrivilegedAccessor.setValue(this, "_router", new MessageRouterStub());
            PrivilegedAccessor.setValue(this, "_manager", new ConnectionManagerStub());
        } catch(Exception e) {
            ErrorService.error(e);
        }		
		//_router = new MessageRouterStub(); 
        //_manager = new ConnectionManagerStub();
    }

    public void initialize() {
    }
}

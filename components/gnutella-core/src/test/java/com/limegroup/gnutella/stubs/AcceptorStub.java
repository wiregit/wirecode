package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.Acceptor;

/** An acceptor that doesn't accept incoming connections. */
public class AcceptorStub extends Acceptor {
    public AcceptorStub() {
        super();
    }
    
    public boolean acceptedIncoming=true;
    public boolean acceptedIncoming() {
        return acceptedIncoming;
    }
}

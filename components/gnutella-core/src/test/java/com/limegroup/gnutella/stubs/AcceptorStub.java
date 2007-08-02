package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ProviderHacks;

/** An acceptor that doesn't accept incoming connections. */
public class AcceptorStub extends Acceptor {
    public AcceptorStub() {
        super(ProviderHacks.getNetworkManager());
    }
    
    public boolean acceptedIncoming=true;
    public boolean acceptedIncoming() {
        return acceptedIncoming;
    }
}

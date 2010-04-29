package com.limegroup.gnutella.dht.messages;

import org.limewire.mojito.messages.FindNodeRequest;
import org.limewire.mojito2.KUID;


public class FindNodeRequestWireImpl extends AbstractMessageWire<FindNodeRequest> 
        implements FindNodeRequest {

    FindNodeRequestWireImpl(FindNodeRequest delegate) {
        super(delegate);
    }

    public KUID getLookupID() {
        return delegate.getLookupID();
    }
}

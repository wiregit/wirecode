package com.limegroup.gnutella.dht.messages;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.FindNodeRequest;

public class FindNodeRequestWireImpl extends AbstractMessageWire<FindNodeRequest> 
        implements FindNodeRequest {

    FindNodeRequestWireImpl(FindNodeRequest delegate) {
        super(delegate);
    }

    public int getFlags() {
        return delegate.getFlags();
    }

    public KUID getLookupID() {
        return delegate.getLookupID();
    }
}

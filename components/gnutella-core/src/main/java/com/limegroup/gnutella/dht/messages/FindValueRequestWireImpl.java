package com.limegroup.gnutella.dht.messages;

import java.util.Collection;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.messages.FindValueRequest;

public class FindValueRequestWireImpl extends AbstractMessageWire<FindValueRequest> 
        implements FindValueRequest {

    FindValueRequestWireImpl(FindValueRequest delegate) {
        super(delegate);
    }

    public KUID getLookupID() {
        return delegate.getLookupID();
    }

    public Collection<KUID> getKeys() {
        return delegate.getKeys();
    }
}

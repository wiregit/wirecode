package com.limegroup.gnutella.dht.messages;

import java.util.Collection;

import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.messages.StoreRequest;
import org.limewire.security.QueryKey;


public class StoreRequestWireImpl extends AbstractMessageWire<StoreRequest> 
        implements StoreRequest {

    StoreRequestWireImpl(StoreRequest delegate) {
        super(delegate);
    }

    public Collection<? extends DHTValueEntity> getDHTValues() {
        return delegate.getDHTValues();
    }

    public QueryKey getQueryKey() {
        return delegate.getQueryKey();
    }
}

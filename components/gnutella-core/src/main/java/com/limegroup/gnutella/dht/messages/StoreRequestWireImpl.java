package com.limegroup.gnutella.dht.messages;

import java.util.Collection;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.db.DHTValueEntity;
import com.limegroup.mojito.messages.StoreRequest;

public class StoreRequestWireImpl extends AbstractMessageWire<StoreRequest> 
        implements StoreRequest {

    StoreRequestWireImpl(StoreRequest delegate) {
        super(delegate);
    }

    public Collection<DHTValueEntity> getDHTValues() {
        return delegate.getDHTValues();
    }

    public QueryKey getQueryKey() {
        return delegate.getQueryKey();
    }
}

package com.limegroup.gnutella.dht.messages;

import java.util.Collection;

import org.limewire.mojito.messages.FindValueResponse;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.storage.DHTValueEntity;


public class FindValueResponseWireImpl extends AbstractMessageWire<FindValueResponse> 
        implements FindValueResponse {

    FindValueResponseWireImpl(FindValueResponse delegate) {
        super(delegate);
    }

    public Collection<KUID> getSecondaryKeys() {
        return delegate.getSecondaryKeys();
    }

    public Collection<? extends DHTValueEntity> getDHTValueEntities() {
        return delegate.getDHTValueEntities();
    }

    public float getRequestLoad() {
        return delegate.getRequestLoad();
    }
}

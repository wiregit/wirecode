package com.limegroup.gnutella.dht.messages;

import java.util.Collection;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.messages.FindValueResponse;

public class FindValueResponseWireImpl extends AbstractMessageWire<FindValueResponse> 
        implements FindValueResponse {

    FindValueResponseWireImpl(FindValueResponse delegate) {
        super(delegate);
    }

    public Collection<KUID> getKeys() {
        return delegate.getKeys();
    }

    public Collection<DHTValue> getValues() {
        return delegate.getValues();
    }
}

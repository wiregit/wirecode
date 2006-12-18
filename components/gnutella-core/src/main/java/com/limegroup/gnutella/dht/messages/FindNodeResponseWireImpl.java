package com.limegroup.gnutella.dht.messages;

import java.util.Collection;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.messages.FindNodeResponse;
import com.limegroup.mojito.routing.Contact;

public class FindNodeResponseWireImpl extends AbstractMessageWire<FindNodeResponse> 
        implements FindNodeResponse {

    FindNodeResponseWireImpl(FindNodeResponse delegate) {
        super(delegate);
    }

    public Collection<? extends Contact> getNodes() {
        return delegate.getNodes();
    }

    public QueryKey getQueryKey() {
        return delegate.getQueryKey();
    }
}

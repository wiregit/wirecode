package com.limegroup.gnutella.dht.messages;

import java.util.Collection;

import org.limewire.mojito.messages.FindNodeResponse;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.QueryKey;


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

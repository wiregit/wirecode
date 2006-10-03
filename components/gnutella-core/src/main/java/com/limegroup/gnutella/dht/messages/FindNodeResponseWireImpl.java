package com.limegroup.gnutella.dht.messages;

import java.util.Collection;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.messages.FindNodeResponse;

public class FindNodeResponseWireImpl extends AbstractMessageWire<FindNodeResponse> 
        implements FindNodeResponse {

    FindNodeResponseWireImpl(FindNodeResponse delegate) {
        super(delegate);
    }

    public Collection<Contact> getNodes() {
        return delegate.getNodes();
    }

    public QueryKey getQueryKey() {
        return delegate.getQueryKey();
    }
}

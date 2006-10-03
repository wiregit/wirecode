package com.limegroup.gnutella.dht.messages;

import com.limegroup.mojito.messages.StatsResponse;

public class StatsResponseWireImpl extends AbstractMessageWire<StatsResponse> 
        implements StatsResponse {

    StatsResponseWireImpl(StatsResponse delegate) {
        super(delegate);
    }

    public String getStatistics() {
        return delegate.getStatistics();
    }
}

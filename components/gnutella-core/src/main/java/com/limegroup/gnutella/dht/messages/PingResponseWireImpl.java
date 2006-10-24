package com.limegroup.gnutella.dht.messages;

import java.math.BigInteger;
import java.net.SocketAddress;

import com.limegroup.mojito.messages.PingResponse;

public class PingResponseWireImpl extends AbstractMessageWire<PingResponse> 
        implements PingResponse {

    PingResponseWireImpl(PingResponse delegate) {
        super(delegate);
    }

    public BigInteger getEstimatedSize() {
        return delegate.getEstimatedSize();
    }

    public SocketAddress getExternalAddress() {
        return delegate.getExternalAddress();
    }
}

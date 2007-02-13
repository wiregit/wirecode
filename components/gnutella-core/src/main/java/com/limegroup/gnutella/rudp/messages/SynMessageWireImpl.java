package com.limegroup.gnutella.rudp.messages;

import org.limewire.rudp.messages.SynMessage;

class SynMessageWireImpl extends AbstractMessageWire<SynMessage> implements SynMessage {

    SynMessageWireImpl(SynMessage delegate) {
        super(delegate);
    }

    public int getProtocolVersionNumber() {
        return delegate.getProtocolVersionNumber();
    }

    public byte getSenderConnectionID() {
        return delegate.getSenderConnectionID();
    }

}

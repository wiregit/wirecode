package com.limegroup.gnutella.rudp.messages;

import org.limewire.rudp.messages.AckMessage;

class AckMessageWireImpl extends AbstractMessageWire<AckMessage> implements AckMessage {
    
    AckMessageWireImpl(AckMessage delegate) {
        super(delegate);
    }

    public void extendWindowStart(long wStart) {
        delegate.extendWindowStart(wStart);
    }

    public int getWindowSpace() {
        return delegate.getWindowSpace();
    }

    public long getWindowStart() {
        return delegate.getWindowStart();
    }

}

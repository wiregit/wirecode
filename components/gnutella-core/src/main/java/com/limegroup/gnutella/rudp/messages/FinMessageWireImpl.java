package com.limegroup.gnutella.rudp.messages;

import org.limewire.rudp.messages.FinMessage;

class FinMessageWireImpl extends AbstractMessageWire<FinMessage> implements FinMessage {

    FinMessageWireImpl(FinMessage delegate) {
        super(delegate);
    }

}

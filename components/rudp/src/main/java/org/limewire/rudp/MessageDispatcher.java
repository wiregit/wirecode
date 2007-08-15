package org.limewire.rudp;

import java.net.InetSocketAddress;

import org.limewire.rudp.messages.RUDPMessage;

/**
 * Dispatches incoming messages to the correct multiplexor.
 */
public interface MessageDispatcher {

    /** Dispatches a message from the given host. */
    public void dispatch(RUDPMessage message, InetSocketAddress from);
    
}

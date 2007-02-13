package com.limegroup.gnutella.rudp;

import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.TransportListener;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.messages.MessageFactory;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;

import com.limegroup.gnutella.rudp.messages.MessageFactoryWire;

/** The parts necessary for linking together LimeWire & RUDP. */
public class RUDPContextWire implements RUDPContext {

    private final MessageFactory factory = new MessageFactoryWire(new DefaultMessageFactory());
    
    public MessageFactory getMessageFactory() {
        return factory;
    }

    public TransportListener getTransportListener() {
        return NIODispatcher.instance().getTransportListener();
    }

}

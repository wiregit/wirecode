package com.limegroup.gnutella.rudp;

import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.TransportListener;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.RUDPSettings;
import org.limewire.rudp.UDPService;
import org.limewire.rudp.messages.MessageFactory;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;

import com.limegroup.gnutella.rudp.messages.MessageFactoryWire;

/** The parts necessary for linking together LimeWire & RUDP. */
public class RUDPContextWire implements RUDPContext {

    private final MessageFactory factory = new MessageFactoryWire(new DefaultMessageFactory());
    private final RUDPSettings settings = new RUDPSettingsWire();
    private final UDPService service = new UDPServiceWire();
    
    public MessageFactory getMessageFactory() {
        return factory;
    }

    public TransportListener getTransportListener() {
        return NIODispatcher.instance().getTransportListener();
    }

    public RUDPSettings getRUDPSettings() {
        return settings;
    }

    public UDPService getUDPService() {
        return service;
    }

}

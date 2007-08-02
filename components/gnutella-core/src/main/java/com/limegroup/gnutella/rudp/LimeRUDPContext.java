package com.limegroup.gnutella.rudp;

import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.TransportListener;
import org.limewire.rudp.RUDPContext;
import org.limewire.rudp.RUDPSettings;
import org.limewire.rudp.UDPService;
import org.limewire.rudp.messages.RUDPMessage;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.MessageFactory.MessageParser;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageFactory;
import com.limegroup.gnutella.rudp.messages.LimeRUDPMessageParser;

/** The parts necessary for linking together LimeWire & RUDP. */
public class LimeRUDPContext implements RUDPContext {

    private final RUDPMessageFactory factory;

    private final RUDPSettings settings;

    private final UDPService service;

    /**
     * Constructs a new LimeRUDPContext and installs it as the parser
     * for incoming messages.
     */
    public LimeRUDPContext() {
        factory = new LimeRUDPMessageFactory(new DefaultMessageFactory());
        settings = new LimeRUDPSettings();
        // DPINJ: Get rid of!
        service = new LimeUDPService(ProviderHacks.getNetworkManager());
        MessageParser parser = new LimeRUDPMessageParser(factory);
        MessageFactory.setParser(RUDPMessage.F_RUDP_MESSAGE, parser);
    }

    public RUDPMessageFactory getMessageFactory() {
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

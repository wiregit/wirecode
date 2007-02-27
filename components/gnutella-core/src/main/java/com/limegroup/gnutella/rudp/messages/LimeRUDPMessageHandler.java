package com.limegroup.gnutella.rudp.messages;

import java.net.InetSocketAddress;

import org.limewire.rudp.UDPMultiplexor;
import org.limewire.rudp.messages.RUDPMessage;

import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;

public class LimeRUDPMessageHandler implements MessageHandler {
    
    private final UDPMultiplexor plexor;
    
    public LimeRUDPMessageHandler(UDPMultiplexor plexor) {
        this.plexor = plexor;
    }
    
    /** Installs this handler on the given router. */
    public void install(MessageRouter router) {
        router.setUDPMessageHandler(LimeAckMessageImpl.class, this);
        router.setUDPMessageHandler(LimeDataMessageImpl.class, this);
        router.setUDPMessageHandler(LimeFinMessageImpl.class, this);
        router.setUDPMessageHandler(LimeKeepAliveMessageImpl.class, this);
        router.setUDPMessageHandler(LimeSynMessageImpl.class, this);
    }

    public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        plexor.routeMessage((RUDPMessage)msg, addr);
    }

}

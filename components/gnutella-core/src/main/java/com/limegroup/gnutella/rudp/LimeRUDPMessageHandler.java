package com.limegroup.gnutella.rudp;

import java.net.InetSocketAddress;

import org.limewire.rudp.UDPMultiplexor;
import org.limewire.rudp.messages.RUDPMessage;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;

public class LimeRUDPMessageHandler implements MessageHandler {
    
    private final UDPMultiplexor plexor;
    
    public LimeRUDPMessageHandler(UDPMultiplexor plexor) {
        this.plexor = plexor;
    }

    public void handleMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
        plexor.routeMessage((RUDPMessage)msg, addr);
    }

}

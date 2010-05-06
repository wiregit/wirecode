package com.limegroup.gnutella.dht2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito2.io.AbstractTransport;
import org.limewire.mojito2.io.MessageDispatcher;
import org.limewire.mojito2.routing.RemoteContact;

import com.google.inject.Provider;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.dht.messages.FindNodeRequestWireImpl;
import com.limegroup.gnutella.dht.messages.FindNodeResponseWireImpl;
import com.limegroup.gnutella.dht.messages.FindValueRequestWireImpl;
import com.limegroup.gnutella.dht.messages.FindValueResponseWireImpl;
import com.limegroup.gnutella.dht.messages.PingRequestWireImpl;
import com.limegroup.gnutella.dht.messages.PingResponseWireImpl;
import com.limegroup.gnutella.dht.messages.StoreRequestWireImpl;
import com.limegroup.gnutella.dht.messages.StoreResponseWireImpl;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;

class LimeTransport extends AbstractTransport 
        implements MessageHandler {

    private static final Log LOG 
        = LogFactory.getLog(LimeTransport.class);
    
    /**
     * An array of Messages this MessageHandler supports
     */
    private static final Class[] UDP_MESSAGE_TYPES = {
        PingRequestWireImpl.class, PingResponseWireImpl.class,
        StoreRequestWireImpl.class, StoreResponseWireImpl.class,
        FindNodeRequestWireImpl.class, FindNodeResponseWireImpl.class,
        FindValueRequestWireImpl.class, FindValueResponseWireImpl.class
    };
    
    private final Provider<UDPService> udpService;
    
    private final Provider<MessageRouter> messageRouter;
    
    private volatile MessageDispatcher messageDispatcher = null;
    
    public LimeTransport(Provider<UDPService> udpService, 
            Provider<MessageRouter> messageRouter) {
        
        this.udpService = udpService;
        this.messageRouter = messageRouter;
    }
    
    @Override
    public synchronized void bind(Callback callback) throws IOException {
        super.bind(callback);
        this.messageDispatcher = (MessageDispatcher)callback;
        
        // Install the Message handlers
        MessageRouter mr = messageRouter.get();
        for (Class<? extends Message> clazz : UDP_MESSAGE_TYPES) {
            mr.setUDPMessageHandler(clazz, this);
        }
    }
    
    @Override
    public void unbind() {
        
        // Remove the Message handlers
        MessageRouter mr = messageRouter.get();
        for (Class<? extends Message> clazz : UDP_MESSAGE_TYPES) {
            mr.setUDPMessageHandler(clazz, null);
        }
        
        this.messageDispatcher = null;
        super.unbind();
    }
    
    @Override
    public void send(SocketAddress dst, byte[] message, 
            int offset, int length) throws IOException {
        
        ByteBuffer data = ByteBuffer.wrap(message, offset, length);
        
        UDPService service  = udpService.get();
        service.send(data, (InetSocketAddress)dst, true);
    }

    @Override
    public void handleMessage(Message msg, 
            InetSocketAddress addr, 
            ReplyHandler handler) {
        
        MessageDispatcher messageDispatcher = this.messageDispatcher;
        if (messageDispatcher != null) {
            org.limewire.mojito2.message.Message message 
                = (org.limewire.mojito2.message.Message) msg;
            
            ((RemoteContact) message.getContact())
                    .fixSourceAndContactAddress(addr);
            
            try {
                messageDispatcher.handleMessage(message);
            } catch (IOException err) {
                LOG.error("IOException", err);
            }
        }
    }
}

package com.limegroup.gnutella.dht2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito2.io.AbstractTransport;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;

class MojitoTransport extends AbstractTransport {

    private static final Log LOG 
        = LogFactory.getLog(MojitoTransport.class);
    
    private final Provider<UDPService> udpService;
    
    private final Provider<MessageRouter> messageRouter;
    
    @Inject
    public MojitoTransport(Provider<UDPService> udpService, 
            Provider<MessageRouter> messageRouter) {
        
        this.udpService = udpService;
        this.messageRouter = messageRouter;
    }
    
    @Override
    public synchronized void bind(Callback callback) throws IOException {
        super.bind(callback);
        
        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void handleMessage(Message msg, 
                    InetSocketAddress addr, ReplyHandler handler) {
                
                try {
                    byte[] message = ((MojitoMessage)msg).getMessage();
                    MojitoTransport.this.handleMessage(
                            addr, message, 0, message.length);
                } catch (IOException err) {
                    LOG.error("IOException", err);
                }
            }
        };
        
        // Install the Message handlers
        MessageRouter mr = messageRouter.get();
        mr.setUDPMessageHandler(MojitoMessage.class, messageHandler);
    }
    
    @Override
    public void unbind() {
        
        // Remove the Message handlers
        MessageRouter mr = messageRouter.get();
        mr.setUDPMessageHandler(MojitoMessage.class, null);
        
        super.unbind();
    }
    
    @Override
    public void send(SocketAddress dst, byte[] message, 
            int offset, int length) throws IOException {
        
        ByteBuffer data = ByteBuffer.wrap(message, offset, length);
        
        UDPService service  = udpService.get();
        service.send(data, (InetSocketAddress)dst, true);
    }
}

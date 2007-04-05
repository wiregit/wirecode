package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.Tag;
import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.routing.impl.RemoteContact;
import org.limewire.mojito.util.CryptoUtils;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;
import org.limewire.security.SecureMessageVerifier;

import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.dht.messages.FindNodeRequestWireImpl;
import com.limegroup.gnutella.dht.messages.FindNodeResponseWireImpl;
import com.limegroup.gnutella.dht.messages.FindValueRequestWireImpl;
import com.limegroup.gnutella.dht.messages.FindValueResponseWireImpl;
import com.limegroup.gnutella.dht.messages.MessageFactoryWire;
import com.limegroup.gnutella.dht.messages.PingRequestWireImpl;
import com.limegroup.gnutella.dht.messages.PingResponseWireImpl;
import com.limegroup.gnutella.dht.messages.StatsRequestWireImpl;
import com.limegroup.gnutella.dht.messages.StatsResponseWireImpl;
import com.limegroup.gnutella.dht.messages.StoreRequestWireImpl;
import com.limegroup.gnutella.dht.messages.StoreResponseWireImpl;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;

/**
 * LimeMessageDispatcher re-routes DHTMessage(s) through the
 * LimeWire core so that all communcation can be done over
 * a single network port.
 */
public class LimeMessageDispatcherImpl extends MessageDispatcher 
        implements MessageHandler {
    
    private static final Log LOG = LogFactory.getLog(LimeMessageDispatcherImpl.class);
    
    private volatile boolean running = false;
    
    public LimeMessageDispatcherImpl(Context context) {
        super(context);
        
        // Get Context's MessageFactory and wrap it into a
        // MessageFactoryWire and set it as the MessageFactory
        context.setMessageFactory(
                new MessageFactoryWire(context.getMessageFactory()));
        
        // Register the Message type
        LimeDHTMessageParser parser = new LimeDHTMessageParser(
                context.getMessageFactory());
        
        MessageFactory.setParser((byte)DHTMessage.F_DHT_MESSAGE, parser);
        
        // Install the Message handlers
        MessageRouter messageRouter = RouterService.getMessageRouter();
        messageRouter.setUDPMessageHandler(PingRequestWireImpl.class, this);
        messageRouter.setUDPMessageHandler(PingResponseWireImpl.class, this);
        messageRouter.setUDPMessageHandler(StoreRequestWireImpl.class, this);
        messageRouter.setUDPMessageHandler(StoreResponseWireImpl.class, this);
        messageRouter.setUDPMessageHandler(FindNodeRequestWireImpl.class, this);
        messageRouter.setUDPMessageHandler(FindNodeResponseWireImpl.class, this);
        messageRouter.setUDPMessageHandler(FindValueRequestWireImpl.class, this);
        messageRouter.setUDPMessageHandler(FindValueResponseWireImpl.class, this);
        messageRouter.setUDPMessageHandler(StatsRequestWireImpl.class, this);
        messageRouter.setUDPMessageHandler(StatsResponseWireImpl.class, this);
    }

    @Override
    protected boolean allow(DHTMessage message) {
        //blocking is already done in NIODispatcher
        return true;
    }

    @Override
    public void bind(SocketAddress address) throws IOException {
    }
    
    @Override
    public boolean isBound() {
        return true;
    }

    @Override
    public void start() {
        running = true;
        super.start();
    }
    
    @Override
    public void stop() {
        running = false;
        super.stop();
    }

    /* 
     * Overwritten:
     * 
     * Takes the payload of Tag and sends it via LimeWire's 
     * UDPService
     */
    @Override
    protected boolean enqueueOutput(Tag tag) {
        InetSocketAddress dst = (InetSocketAddress)tag.getSocketAddress();
        ByteBuffer data = tag.getData();
        UDPService.instance().send(data, dst, true);
        registerInput(tag);
        SentMessageStatHandler.UDP_DHT_MSG.addMessage((Message)tag.getMessage());
        return true;
    }

    /*
     * Implements:
     * 
     * Takes the Message, fixes the source address and delegates
     * it to MessageDispatcher's back-end
     */
    public void handleMessage(Message msg, InetSocketAddress addr, 
            ReplyHandler handler) {
        
        if (!isRunning()) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping message from " + addr + " because DHT is not running");
            }
            return;
        }
        
        ReceivedMessageStatHandler.UDP_DHT_MESSAGE.addMessage(msg);
        DHTMessage dhtMessage = (DHTMessage)msg;
        ((RemoteContact)dhtMessage.getContact()).fixSourceAndContactAddress(addr);
        
        handleMessage(dhtMessage);
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    protected void process(Runnable runnable) {
        if (isRunning()) {
            ExecutorService processingQueue 
                = RouterService.getMessageDispatcher().getExecutorService();
            
            processingQueue.execute(runnable);
        }
    }
    
    @Override
    protected void verify(SecureMessage secureMessage, SecureMessageCallback smc) {
        PublicKey pubKey = context.getPublicKey();
        if (pubKey == null) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping SecureMessage " 
                        + secureMessage + " because PublicKey is not set");
            }
            return;
        }
        
        SecureMessageVerifier verifier = RouterService.getSecureMessageVerifier();
        verifier.verify(pubKey, CryptoUtils.SIGNATURE_ALGORITHM, secureMessage, smc);
    }

    @Override
    protected SocketAddress receive(ByteBuffer dst) throws IOException {
        throw new IOException("receive() is not implemented");
    }

    @Override
    protected boolean send(SocketAddress dst, ByteBuffer data) throws IOException {
        throw new IOException("send() is not implemented");
    }
}

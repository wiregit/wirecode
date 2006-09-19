/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messagehandlers.MessageHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.SecureMessage;
import com.limegroup.gnutella.messages.SecureMessageCallback;
import com.limegroup.gnutella.messages.SecureMessageVerifier;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.statistics.SentMessageStatHandler;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.io.Tag;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.messages.impl.FindNodeRequestImpl;
import com.limegroup.mojito.messages.impl.FindNodeResponseImpl;
import com.limegroup.mojito.messages.impl.FindValueRequestImpl;
import com.limegroup.mojito.messages.impl.FindValueResponseImpl;
import com.limegroup.mojito.messages.impl.PingRequestImpl;
import com.limegroup.mojito.messages.impl.PingResponseImpl;
import com.limegroup.mojito.messages.impl.StatsRequestImpl;
import com.limegroup.mojito.messages.impl.StatsResponseImpl;
import com.limegroup.mojito.messages.impl.StoreRequestImpl;
import com.limegroup.mojito.messages.impl.StoreResponseImpl;
import com.limegroup.mojito.routing.impl.RemoteContact;
import com.limegroup.mojito.security.CryptoHelper;

/**
 * LimeMessageDispatcher re-routes DHTMessage(s) through the
 * LimeWire core so that all communcation can be done over
 * a single network port.
 */
public class LimeMessageDispatcherImpl extends MessageDispatcher 
        implements MessageHandler {
    
    private static final Log LOG = LogFactory.getLog(LimeMessageDispatcherImpl.class);
    
    private ProcessingQueue processingQueue;
    
    private volatile boolean running = false;
    
    public LimeMessageDispatcherImpl(Context context) {
        super(context);
        
        processingQueue = new ProcessingQueue(
                context.getName() + "-LimeMessageDispatcherPQ");

        // Register the Message type
        LimeDHTMessageParser parser = new LimeDHTMessageParser(context.getMessageFactory());
        MessageFactory.setParser((byte)DHTMessage.F_DHT_MESSAGE, parser);
        
        // Install the Message handlers
        MessageRouter messageRouter = RouterService.getMessageRouter();
        messageRouter.setUDPMessageHandler(PingRequestImpl.class, this);
        messageRouter.setUDPMessageHandler(PingResponseImpl.class, this);
        messageRouter.setUDPMessageHandler(StoreRequestImpl.class, this);
        messageRouter.setUDPMessageHandler(StoreResponseImpl.class, this);
        messageRouter.setUDPMessageHandler(FindNodeRequestImpl.class, this);
        messageRouter.setUDPMessageHandler(FindNodeResponseImpl.class, this);
        messageRouter.setUDPMessageHandler(FindValueRequestImpl.class, this);
        messageRouter.setUDPMessageHandler(FindValueResponseImpl.class, this);
        messageRouter.setUDPMessageHandler(StatsRequestImpl.class, this);
        messageRouter.setUDPMessageHandler(StatsResponseImpl.class, this);
    }

    @Override
    protected boolean allow(DHTMessage message) {
        return true;
    }

    @Override
    public void bind(SocketAddress address) throws IOException {
        running = true;
    }

    @Override
    public boolean isOpen() {
        return running;
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
        
        processingQueue.clear();
        clear();
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
        UDPService.instance().send(data, dst.getAddress(), dst.getPort(), true);
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
        
        try {
            handleMessage(dhtMessage);
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    }
    
    @Override
    protected void interestRead(boolean on) {
        // DO NOTHING
    }

    @Override
    protected void interestWrite(boolean on) {
        // DO NOTHING
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    protected void process(Runnable runnable) {
        if (isRunning()) {
            processingQueue.add(runnable);
        }
    }
    
    @Override
    protected void verify(SecureMessage secureMessage, SecureMessageCallback smc) {
        SecureMessageVerifier verifier = RouterService.getSecureMessageVerifier();
        verifier.verify(context.getMasterKey(), CryptoHelper.SIGNATURE_ALGORITHM, secureMessage, smc);
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

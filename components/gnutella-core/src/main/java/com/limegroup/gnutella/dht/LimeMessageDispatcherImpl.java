/*
 * Mojito Distributed Hash Tabe (DHT)
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
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.io.Tag;
import com.limegroup.mojito.messages.DHTMessage;

/**
 * LimeMessageDispatcher re-routes DHTMessage(s) through the
 * LimeWire core so that all communcation can be done over
 * a single network port.
 */
public class LimeMessageDispatcherImpl extends MessageDispatcher 
        implements MessageRouter.MessageHandler {

    private static final Log LOG = LogFactory.getLog(LimeMessageDispatcherImpl.class);
    
    private ProcessingQueue processingQueue;
    
    private boolean running = false;
    
    public LimeMessageDispatcherImpl(Context context) {
        super(context);
        
        processingQueue = new ProcessingQueue(context.getName() + "-LimeMessageDispatcherPQ", true);
        
        // Set the MessageFactory
        LimeDHTMessageFactory factory = new LimeDHTMessageFactory();
        context.setMessageFactory(factory);
        
        // Register the Message type
        LimeDHTMessageParser parser = new LimeDHTMessageParser(factory);
        MessageFactory.setParser(LimeDHTMessage2.F_DHT_MESSAGE, parser);
        
        // Install the handler for LimeDHTMessage
        RouterService.getMessageRouter()
            .setUDPMessageHandler(LimeDHTMessage2.class, this);
        
        // Install cleanup task
        context.scheduleAtFixedRate(new Runnable() {
            public void run() {
                if (isRunning()) {
                    handleClenup();
                }
            }
        }, CLEANUP, CLEANUP);
    }

    protected boolean allow(DHTMessage message) {
        return true;
    }

    public void bind(SocketAddress address) throws IOException {
        running = true;
    }

    public boolean isOpen() {
        return running;
    }
    
    public void stop() {
        running = false;
        processingQueue.clear();
        clear();
    }

    /* 
     * Overwritten:
     * 
     * Takes the payload of Tag, wraps it in a LimeDHTMessage, 
     * sends it over UDPService and registers it in the input
     * map if it's a RequestMessage.
     */
    protected boolean enqueueOutput(Tag tag) {
        InetSocketAddress dst = (InetSocketAddress)tag.getSocketAddres();
        ByteBuffer data = tag.getData();
        UDPService.instance().send(data, dst.getAddress(), dst.getPort(), false);
        registerInput(tag);
        return true;
    }

    /*
     * Implements:
     * 
     * Takes the payload of LimeDHTMessage, deserializes it into
     * a DHTMessage and handles the message.
     */
    public void handleMessage(Message msg, InetSocketAddress addr, 
            ReplyHandler handler) {
        LimeDHTMessage2 dhtMessage = (LimeDHTMessage2)msg;
        dhtMessage.getContactNode().setSocketAddress(addr);
        
        try {
            handleMessage(dhtMessage);
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    }

    protected void interestRead(boolean on) {
        // DO NOTHING
    }

    protected void interestWrite(boolean on) {
        // DO NOTHING
    }

    public boolean isRunning() {
        return running;
    }

    protected void process(Runnable runnable) {
        processingQueue.add(runnable);
    }
    
    // This is not running as a Thread!
    public void run() {
        running = true;
    }
}

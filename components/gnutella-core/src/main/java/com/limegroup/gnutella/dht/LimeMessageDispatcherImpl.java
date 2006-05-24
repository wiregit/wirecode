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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.io.MessageDispatcher;
import com.limegroup.mojito.io.MessageFormatException;
import com.limegroup.mojito.io.Tag;
import com.limegroup.mojito.messages.DHTMessage;

/**
 * 
 */
public class LimeMessageDispatcherImpl extends MessageDispatcher 
        implements MessageRouter.MessageHandler {

    private static final Log LOG = LogFactory.getLog(LimeMessageDispatcherImpl.class);
    
    private ProcessingQueue processingQueue;
    
    private boolean running = false;
    
    public LimeMessageDispatcherImpl(Context context) {
        super(context);
        
        processingQueue = new ProcessingQueue(context.getName() + "-LimeMessageDispatcherPQ", true);
        
        LimeDHTMessage.registerMessage();
        
        RouterService.getMessageRouter()
            .setUDPMessageHandler(LimeDHTMessage.class, this);
        
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
    }
    
    protected boolean enqueueOutput(Tag tag) {
        try {
            InetSocketAddress dst = (InetSocketAddress)tag.getSocketAddres();
            LimeDHTMessage msg = LimeDHTMessage.createMessage(tag.getData().array());
            UDPService.instance().send(msg, dst);
            tag.sent();
            registerInput(tag);
            return true;
        } catch (BadPacketException e) {
            LOG.error("BadPacketException", e);
        } catch (IOException e) {
            LOG.error("IOException", e);
        }
        return false;
    }

    public void handleMessage(Message msg, InetSocketAddress addr, 
            ReplyHandler handler) {
        try {
            DHTMessage message = ((LimeDHTMessage)msg).getDHTMessage(addr);
            received(message);
        } catch (MessageFormatException err) {
            LOG.error("MessageFormatException", err);
        } catch (IOException err) {
            LOG.error("IOException", err);
        }
    }

    protected void interestRead(boolean on) {
    }

    protected void interestWrite(boolean on) {
    }

    public boolean isRunning() {
        return running;
    }

    protected void process(Runnable runnable) {
        processingQueue.add(runnable);
    }
    
    public void run() {
        running = true;
    }
}

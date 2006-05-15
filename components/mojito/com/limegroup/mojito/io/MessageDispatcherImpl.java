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
 
package com.limegroup.mojito.io;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.util.ProcessingQueue;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.messages.DHTMessage;


public class MessageDispatcherImpl extends MessageDispatcher implements Runnable {

    private static final Log LOG = LogFactory.getLog(MessageDispatcherImpl.class);
    
    private static final long SELECTOR_SLEEP = 50L;
    
    private Selector selector;
    private Filter filter;
    
    private ProcessingQueue processingQueue;
    
    public MessageDispatcherImpl(Context context) {
        super(context);
        
        processingQueue = new ProcessingQueue(context.getName() + "-MessageDispatcherPQ", true);
        filter = new Filter();
    }
    
    public void bind(SocketAddress address) throws IOException {
        if (isOpen()) {
            throw new IOException("Already open");
        }
        
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
        
        DatagramSocket socket = channel.socket();
        socket.setReuseAddress(false);
        socket.setReceiveBufferSize(INPUT_BUFFER_SIZE);
        socket.setSendBufferSize(OUTPUT_BUFFER_SIZE);
        
        socket.bind(address);
        
        setDatagramChannel(channel);
    }
    
    public void stop() {
        try {
            if (selector != null) {
                selector.close();
                getDatagramChannel().close();
            }
        } catch (IOException err) {
            LOG.error("An error occured during stopping", err);
        }
        
        processingQueue.clear();
    }
    
    public boolean isRunning() {
        return isOpen() && getDatagramChannel().isRegistered();
    }

    protected boolean allow(DHTMessage message) {
        return filter.allow(message.getSourceAddress());
    }
    
    protected void process(Runnable runnable) {
        processingQueue.add(runnable);
    }
    
    private void interest(int ops, boolean on) {
        try {
            DatagramChannel channel = getDatagramChannel();
            SelectionKey sk = channel.keyFor(selector);
            if (sk != null && sk.isValid()) {
                synchronized(channel.blockingLock()) {
                    if (on) {
                        sk.interestOps(sk.interestOps() | ops);
                    } else {
                        sk.interestOps(sk.interestOps() & ~ops);
                    }
                }
            }
        } catch (CancelledKeyException ignore) {}
    }

    protected void interestRead(boolean on) {
        interest(SelectionKey.OP_READ, on);
    }
    
    protected void interestWrite(boolean on) {
        interest(SelectionKey.OP_WRITE, on);
    }
    
    public void run() {
        
        long lastCleanup = System.currentTimeMillis();
        
        while(isRunning()) {
            
            try {
                selector.select(SELECTOR_SLEEP);
                
                // READ
                handleRead();
                
                // WRITE
                handleWrite();
                
                // CLEANUP
                if (System.currentTimeMillis()-lastCleanup >= CLEANUP) {
                    handleClenup();
                    lastCleanup = System.currentTimeMillis();
                }
            } catch (ClosedSelectorException err) {
                // thrown as close() is called asynchronously
                //LOG.error(err);
            } catch (ClosedChannelException err) {
                // thrown as close() is called asynchronously
                //LOG.error(err);
            } catch (IOException err) {
                LOG.fatal("MessageHandler IO exception: ",err);
            }
        }
    }
}

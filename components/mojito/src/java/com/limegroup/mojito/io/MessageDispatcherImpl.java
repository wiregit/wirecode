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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.messages.SecureMessage;
import com.limegroup.gnutella.messages.SecureMessageCallback;
import com.limegroup.gnutella.messages.SecureMessageVerifier;
import com.limegroup.mojito.Contact;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.security.CryptoHelper;

/**
 * This is a stand alone/reference implementation of MessageDispatcher
 */
public class MessageDispatcherImpl extends MessageDispatcher {

    private static final Log LOG = LogFactory.getLog(MessageDispatcherImpl.class);
    
    private static final long SELECTOR_SLEEP = 50L;
    
    private Selector selector;
    private Filter filter;
    
    private SecureMessageVerifier verifier;
    
    private ExecutorService executor;
    
    private Thread thread;
    
    public MessageDispatcherImpl(final Context context) {
        super(context);
        
        verifier = new SecureMessageVerifier(context.getName());
        filter = new Filter();
    }
    
    @Override
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
    
    @Override
    public synchronized void start() {
        ThreadFactory factory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread thread = context.getThreadFactory().newThread(r);
                thread.setName(context.getName() + "-MessageDispatcherExecutor");
                thread.setDaemon(true);
                return thread;
            }
        };
        
        executor = Executors.newFixedThreadPool(1, factory);
        
        thread = context.getThreadFactory().newThread(this);
        thread.setName(context.getName() + "-MessageDispatcherThread");
        //thread.setDaemon(true);
        thread.start();
    }
    
    @Override
    public synchronized void stop() {
        try {
            if (selector != null) {
                selector.close();
                getDatagramChannel().close();
            }
        } catch (IOException err) {
            LOG.error("An error occured during stopping", err);
        }
        
        thread.interrupt();
        executor.shutdownNow();
        clear();
    }
    
    @Override
    public synchronized boolean isRunning() {
        return isOpen() && getDatagramChannel().isRegistered();
    }

    @Override
    protected boolean allow(DHTMessage message) {
        Contact node = message.getContact();
        return filter.allow(node.getContactAddress());
    }
    
    
    @Override
    protected void process(Runnable runnable) {
        if (isRunning()) {
            executor.execute(runnable);
        }
    }
    
    @Override
    protected void verify(SecureMessage secureMessage, SecureMessageCallback smc) {
        verifier.verify(context.getMasterKey(), CryptoHelper.SIGNATURE_ALGORITHM, secureMessage, smc);
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

    @Override
    protected void interestRead(boolean on) {
        interest(SelectionKey.OP_READ, on);
    }
    
    @Override
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
                    handleCleanup();
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

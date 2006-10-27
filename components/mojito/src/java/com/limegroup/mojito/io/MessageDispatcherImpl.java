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
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.messages.SecureMessage;
import com.limegroup.gnutella.messages.SecureMessageCallback;
import com.limegroup.gnutella.messages.SecureMessageVerifier;
import com.limegroup.mojito.Context;
import com.limegroup.mojito.messages.DHTMessage;
import com.limegroup.mojito.util.CryptoUtils;

/**
 * This is a stand alone/reference implementation of MessageDispatcher
 */
public class MessageDispatcherImpl extends MessageDispatcher implements Runnable {

    private static final Log LOG = LogFactory.getLog(MessageDispatcherImpl.class);
    
    private static final long SELECTOR_SLEEP = 50L;
    
    private volatile boolean running = false;
    
    private Object channelLock = new Object();
    
    private Selector selector;
    
    private DatagramChannel channel;
    
    private SecureMessageVerifier verifier;
    
    private ExecutorService executor;
    
    private Thread thread;
    
    public MessageDispatcherImpl(Context context) {
        super(context);
        
        verifier = new SecureMessageVerifier(context.getName());
    }
    
    @Override
    public void bind(SocketAddress address) throws IOException {
        synchronized (channelLock) {
            if (isOpen()) {
                throw new IOException("Already open");
            }
            
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
            
            DatagramSocket socket = channel.socket();
            socket.setReuseAddress(false);
            socket.setReceiveBufferSize(INPUT_BUFFER_SIZE);
            socket.setSendBufferSize(OUTPUT_BUFFER_SIZE);
            
            socket.bind(address);
        }
    }
    
    @Override
    public boolean isOpen() {
        DatagramChannel c = channel;
        return c != null && c.isOpen();
    }

    /**
     * Returns the DatagramChannel
     */
    public DatagramChannel getDatagramChannel() {
        return channel;
    }
    
    /**
     * Returns the DatagramChannel Socket's local SocketAddress
     */
    public SocketAddress getLocalSocketAddress() {
        DatagramChannel c = channel;
        if (c != null) {
            return c.socket().getLocalSocketAddress();
        }
        return null;
    }
    
    @Override
    public void start() {
        synchronized (channelLock) {
            if (!running) {
                ThreadFactory factory = new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread thread = context.getThreadFactory().newThread(r);
                        thread.setName(context.getName() + "-MessageDispatcherExecutor");
                        thread.setDaemon(true);
                        return thread;
                    }
                };
                
                running = true;
                
                executor = Executors.newFixedThreadPool(1, factory);
                
                thread = context.getThreadFactory().newThread(this);
                thread.setName(context.getName() + "-MessageDispatcherThread");
                //thread.setDaemon(true);
                thread.start();
                
                super.start();
            }
        }
    }
    
    @Override
    public void stop() {
        synchronized (channelLock) {
            // Signal the MessageDispatcher Thread that we're
            // going to shutdown and wait for the MD to finish
            // whatever it's doing...
            if (running) {
                running = false;
                try {
                    channelLock.wait(10L*1000L);
                } catch (InterruptedException e) {
                    LOG.error("InterruptedException", e);
                }
            }
            
            super.stop();
            clear();
            
            if (executor != null) {
                executor.shutdown();
                try {
                    executor.awaitTermination(10L*1000L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    LOG.error("InterruptedException", e);
                }
                executor = null;
            }
            
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
            
            if (selector != null) {
                try {
                    selector.close();
                    selector = null;
                } catch (IOException err) {
                    LOG.error("IOException", err);
                }
            }
            
            if (channel != null) {
                try {
                    channel.close();
                    channel = null;
                } catch (IOException err) {
                    LOG.error("IOException", err);
                }
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    protected boolean allow(DHTMessage message) {
        //TODO: IP filter: MOJITO-92
        return true;
    }
    
    @Override
    protected void process(Runnable runnable) {
        synchronized (channelLock) {
            if (running) {
                executor.execute(runnable);
            }
        }
    }
    
    @Override
    protected void verify(SecureMessage secureMessage, SecureMessageCallback smc) {
        verifier.verify(context.getMasterKey(), 
                CryptoUtils.SIGNATURE_ALGORITHM, secureMessage, smc);
    }

    private void interest(int ops, boolean on) {
        DatagramChannel c = channel;
        if (c == null) {
            return;
        }
        
        try {
            SelectionKey sk = c.keyFor(selector);
            if (sk != null && sk.isValid()) {
                synchronized(c.blockingLock()) {
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
    
    @Override
    protected SocketAddress receive(ByteBuffer dst) throws IOException {
        return channel.receive(dst);
    }

    @Override
    protected boolean send(SocketAddress dst, ByteBuffer data) throws IOException {
        return channel.send(data, dst) > 0;
    }

    public void run() {
        try {
            while (isRunning()) {
                
                selector.select(SELECTOR_SLEEP);
                
                try {
                    // READ
                    handleRead();
                } catch (IOException err) {
                    LOG.error("IOException-READ", err);
                }
                
                try {
                    // WRITE
                    handleWrite();
                } catch (IOException err) {
                    LOG.error("IOException-WRITE", err);
                }
            }
        } catch (IOException err) {
            LOG.fatal("IOException", err);
        } finally {
            synchronized (channelLock) {
                running = false;
                channelLock.notifyAll();
            }
        }
    }
}

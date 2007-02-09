/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
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
 
package org.limewire.mojito.io;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.PublicKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.mojito.Context;
import org.limewire.mojito.util.CryptoUtils;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;
import org.limewire.security.Verifier;


/**
 * This is a stand alone/reference implementation of MessageDispatcher
 */
public class MessageDispatcherImpl extends MessageDispatcher implements Runnable {

    private static final Log LOG = LogFactory.getLog(MessageDispatcherImpl.class);
    
    /**
     * Sleep timeout of the Selector
     */
    private static final long SELECTOR_SLEEP = 50L;
    
    /**
     * A flag whether or not this MD is running
     */
    private volatile boolean running = false;
    
    /**
     * A flag whether or not this MD is accepting incoming 
     * Requests and Responses
     */
    private volatile boolean accepting = false;
    
    /**
     * The DatagramChannel's Selector
     */
    private Selector selector;
    
    /**
     * The DatagramChanel
     */
    private DatagramChannel channel;
    
    /**
     * The ExecutorService where processes are executed
     */
    private ExecutorService executor;
    
    /**
     * The Thread this MD is running on
     */
    private Thread thread;
    
    /**
     * The DatagramChannel lock Object
     */
    private final Object channelLock = new Object();

    public MessageDispatcherImpl(Context context) {
        super(context);
    }
    
    /**
     * Returns the DatagramChannel lock
     */
    protected Object getDatagramChannelLock() {
        return channelLock;
    }
    
    @Override
    public void bind(SocketAddress address) throws IOException {
        synchronized (getDatagramChannelLock()) {
            if (isOpen()) {
                throw new IOException("DatagramChannel is already open");
            }
            
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);
            
            DatagramSocket socket = channel.socket();
            socket.setReuseAddress(false);
            socket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
            socket.setSendBufferSize(SEND_BUFFER_SIZE);
            
            socket.bind(address);
        }
    }
    
    /**
     * Returns true if the DatagramChannel is open
     */
    public boolean isOpen() {
        synchronized (getDatagramChannelLock()) {
            return channel != null && channel.isOpen();
        }
    }
    
    /**
     * Returns the DatagramChannel
     */
    public DatagramChannel getDatagramChannel() {
        synchronized (getDatagramChannelLock()) {
            return channel;
        }
    }
    
    /**
     * Returns the DatagramChannel Socket's local SocketAddress
     */
    public SocketAddress getLocalSocketAddress() {
        synchronized (getDatagramChannelLock()) {
            if (channel != null && channel.isOpen()) {
                return channel.socket().getLocalSocketAddress();
            }
            return null;
        }
        
    }
    
    @Override
    public void start() {
        synchronized (getDatagramChannelLock()) {
            if (!isOpen()) {
                throw new IllegalStateException("MessageDispatcher is not bound");
            }
            
            if (!running) {
                ThreadFactory factory = new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread thread = context.getDHTExecutorService().getThreadFactory().newThread(r);
                        thread.setName(context.getName() + "-MessageDispatcherExecutor");
                        thread.setDaemon(true);
                        return thread;
                    }
                };
                
                accepting = true;
                running = true;
                
                executor = Executors.newFixedThreadPool(1, factory);
                
                thread = context.getDHTExecutorService().getThreadFactory().newThread(this);
                thread.setName(context.getName() + "-MessageDispatcherThread");
                thread.setDaemon(Boolean.getBoolean("com.limegroup.mojito.io.MessageDispatcherIsDaemon"));
                thread.start();
                
                super.start();
            }
        }
    }
    
    @Override
    public void stop() {
        synchronized (getDatagramChannelLock()) {
            // Do not accept any new incoming Requests or Responses
            accepting = false;
            
            if (running) {
                // The idea is to enqueue this fake Tag and to wait
                // for the MessageDispatcher. Once it's being processed
                // we know eveything in front of the queue was sent...
                // This is specific to MessageDispatcherImpl!
                Tag notifier = new Tag(context.getLocalNode(), null) {
                    // Called right before send
                    @Override
                    public boolean isCancelled() {
                        synchronized (getDatagramChannelLock()) {
                            getDatagramChannelLock().notifyAll();
                        }
                        return true;
                    }
                };
                
                enqueueOutput(notifier);
                
                try {
                    getDatagramChannelLock().wait(1000L);
                } catch (InterruptedException e) {
                    LOG.error("InterruptedException", e);
                }
                
                running = false;
            }
            
            super.stop();
            
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
            
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
        }
    }
    
    @Override
    public void close() {
        super.close();
        
        synchronized (getDatagramChannelLock()) {
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
    public boolean isAccepting() {
        return accepting;
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    protected void process(Runnable runnable) {
        synchronized (getDatagramChannelLock()) {
            if (isRunning()) {
                executor.execute(runnable);
            }
        }
    }
    
    @Override
    protected void verify(SecureMessage secureMessage, SecureMessageCallback smc) {
        // Verifying the signature is an expensive Task and should
        // be done on a different Thread than MessageDispatcher's
        // Executor Thread. On the other hand are the chances slim to none
        // that a Node will ever receive a SecureMessage. It's a trade off
        // at the end if it's really an issue or waste of ressources...
        // NOTE: LimeDHTMessageDispatcher is using a different implementation!
        //       This is the stand alone implementation!
        
        final PublicKey pubKey = context.getPublicKey();
        if (pubKey == null) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Dropping SecureMessage " 
                        + secureMessage + " because PublicKey is not set");
            }
            return;
        }
        
        Verifier verifier = new Verifier(secureMessage, smc) {
            @Override
            public String getAlgorithm() {
                return CryptoUtils.SIGNATURE_ALGORITHM;
            }

            @Override
            public PublicKey getPublicKey() {
                return pubKey;
            }
        };
        
        verify(verifier);
    }

    /**
     * Called by verify(SecureMessage, SecureMessageCallback) to execute
     * the Runnable that does the actual verification. You may override
     * this method to execute the Runnable on a different Thread.
     */
    protected void verify(Runnable verifier) {
        // See verify(SecureMessage, SecureMessageCallback)
        process(verifier);
    }
    
    private void interest(int ops, boolean on) {
        synchronized (getDatagramChannelLock()) {
            if (!isOpen()) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("DatagramChannel is not open");
                }
                return;
            }
            
            try {
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
        synchronized (getDatagramChannelLock()) {
            if (!isOpen()) {
                throw new IOException("DatagramChannel is not open");
            }
            
            return channel.receive(dst);            
        }
    }

    @Override
    protected boolean send(SocketAddress dst, ByteBuffer data) throws IOException {
        synchronized (getDatagramChannelLock()) {
            if (!isOpen()) {
                throw new IOException("DatagramChannel is not open");
            }
            
            return channel.send(data, dst) > 0;            
        }
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
                    /*boolean done = !handleWrite();
                    if (done && !isAccepting()) {
                        synchronized (getDatagramChannelLock()) {
                            getDatagramChannelLock().notifyAll();
                        }
                    }*/
                    handleWrite();
                } catch (IOException err) {
                    LOG.error("IOException-WRITE", err);
                }
            }
        } catch (IOException err) {
            // Pass it to the UncaughtExceptionHandler
            Thread.currentThread().getUncaughtExceptionHandler()
                .uncaughtException(Thread.currentThread(), err);
        }
    }
}

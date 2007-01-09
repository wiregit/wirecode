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
import java.util.concurrent.TimeUnit;

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
    
    private static final long SELECTOR_SLEEP = 50L;
    
    private volatile boolean running = false;
    
    private Selector selector;
    
    private DatagramChannel channel;
    
    private ExecutorService executor;
    
    private Thread thread;
    
    /**
     * The DatagramChannel lock Object
     */
    protected final Object channelLock = new Object();

    public MessageDispatcherImpl(Context context) {
        super(context);
    }
    
    @Override
    public void bind(SocketAddress address) throws IOException {
        synchronized (channelLock) {
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
        synchronized (channelLock) {
            return channel != null && channel.isOpen();
        }
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
        synchronized (channelLock) {
            if (channel != null && channel.isOpen()) {
                return channel.socket().getLocalSocketAddress();
            }
            return null;
        }
        
    }
    
    @Override
    public void start() {
        synchronized (channelLock) {
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
        synchronized (channelLock) {
            // Signal the MessageDispatcher Thread that we're
            // going to shutdown and wait for the MD to finish
            // whatever it's doing...
            if (running) {
                running = false;
                try {
                    channelLock.wait(1L*1000L);
                } catch (InterruptedException e) {
                    LOG.error("InterruptedException", e);
                }
            }
            
            super.stop();
            
            if (executor != null) {
                // Don't accept new tasks (we rely also on the fact that 
                // 'running' was set to false - that means we should not
                // see any RejectedExecutionExceptions that are related
                // to this Executor) 
                executor.shutdown(); 
                
                // Give the running tasks a bit time to finish
                try {
                    executor.awaitTermination(10L*1000L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    LOG.error("InterruptedException", e);
                }
                
                // And if they don't then kill 'em if possible (it's a
                // good faith effort and if it doesn't work we'll maybe
                // see some IOExceptions related to the fact that the
                // DatagramChannel is not open).
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
        
        synchronized (channelLock) {
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
    protected void process(Runnable runnable) {
        synchronized (channelLock) {
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
        
        final PublicKey pubKey = context.getMasterKey();
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
        synchronized (channelLock) {
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
        synchronized (channelLock) {
            if (!isOpen()) {
                throw new IOException("DatagramChannel is not open");
            }
            
            return channel.receive(dst);            
        }
    }

    @Override
    protected boolean send(SocketAddress dst, ByteBuffer data) throws IOException {
        synchronized (channelLock) {
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

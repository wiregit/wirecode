package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.util.ManagedThread;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Dispatcher for NIO.
 */
public class NIODispatcher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(NIODispatcher.class);
    
    private static final NIODispatcher INSTANCE = new NIODispatcher();
    public static final NIODispatcher instance() { return INSTANCE; }
    
    /**
     * Constructs the sole NIODispatcher, starting its thread.
     */
    private NIODispatcher() {
        try {
            selector = Selector.open();
        } catch(IOException iox) {
            throw new RuntimeException(iox);
        }
        
        Thread t = new ManagedThread(this, "NIODispatcher");
        t.start();
    }
    
    /** The selector this uses. */
    private Selector selector = null;
    
    /** Pending queue. */
    private final List PENDING = new LinkedList();
    
    /** Register interest in accepting */
    public void registerAccept(SelectableChannel channel, NIOHandler attachment) {
        register(channel, attachment, SelectionKey.OP_ACCEPT);
    }
    
    /** Register interest in connecting */
    public void registerConnect(SelectableChannel channel, NIOHandler attachment) {
        register(channel, attachment, SelectionKey.OP_CONNECT);
    }
    
    /** Register interest in reading */
    public void registerRead(SelectableChannel channel, NIOHandler attachment) {
        register(channel, attachment, SelectionKey.OP_READ);
    }
    
    /** Register interest in writing */
    public void registerWrite(SelectableChannel channel, NIOHandler attachment) {
        register(channel, attachment, SelectionKey.OP_WRITE);
    }
    
    /** Register interest in both reading & writing */
    public void registerReadWrite(SelectableChannel channel, NIOHandler attachment) {
        register(channel, attachment, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
    
    /** Register interest */
    private void register(SelectableChannel channel, NIOHandler attachment, int op) {
        synchronized(PENDING) {
            PENDING.add(new PendingOp(channel, attachment, op));
        }
        
        // Technically, it is possible (and recommended) to do a selector.wakeup() here,
        // and have selector.select() without a timeout.  Unfortunately, due to bugs
        // with Selector on various OS's, specifically bugs with wakeup() causing 
        // select() to always return immediately forever, this isn't possible.
    }
    
    /** Registers a SelectableChannel as being interested in a write again. */
    public void interestWrite(SelectableChannel channel, boolean on) {
        interest(channel, SelectionKey.OP_WRITE, on);
    }
    
    /** Registers a SelectableChannel as being interested in a read again. */
    public void interestRead(SelectableChannel channel, boolean on) {
        interest(channel, SelectionKey.OP_READ, on);
    }    
    
    /** Registers interest on the channel for the given op */
    private void interest(SelectableChannel channel, int op, boolean on) {
        try {
            SelectionKey sk = channel.keyFor(selector);
            if(sk != null && sk.isValid()) {
                synchronized(channel.blockingLock()) {
                    if(on)
                        sk.interestOps(sk.interestOps() | op);
                    else
                        sk.interestOps(sk.interestOps() & ~op);
                }
            }
        } catch(CancelledKeyException cke) {
            // It is possible to register interest on any thread, which means
            // that the key could have been cancelled at any time.
            // Despite checking for isValid above, it may become invalid.
            // It's a harmless exception, so ignore it.
       }
    }
    
    /**
     * Cancel SelectionKey, close Channel and "free" the attachment
     */
    private void cancel(SelectionKey sk, NIOHandler handler) {
        sk.cancel();
        SelectableChannel channel = (SelectableChannel)sk.channel();
        try {
            channel.close();
        } catch (IOException err) {
            LOG.error("Channel.close()", err);
            handler.handleIOException(err);
        }
    }
    
        
    /**
     * Accept an icoming connection
     * 
     * @throws IOException
     */
    private void processAccept(SelectionKey sk, AcceptHandler handler) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Handling accept: " + handler);
        
        if (!sk.isValid())
            return;
        
        ServerSocketChannel ssc = (ServerSocketChannel)sk.channel();
        SocketChannel channel = ssc.accept();
        
        if (channel == null)
            return;
        
        if (channel.isOpen()) {
            channel.configureBlocking(false);
            handler.handleAccept(channel);
        } else {
            try {
                channel.close();
            } catch (IOException err) {
                LOG.error("SocketChannel.close()", err);
            }
        }
    }
    
    /**
     * Process a connected channel.
     */
    private void processConnect(SelectionKey sk, ConnectHandler handler) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Handling connect: " + handler);        
        
        if(!sk.isValid())
            return;
            
        SocketChannel channel = (SocketChannel)sk.channel();
        
        boolean finished = channel.finishConnect();
        if(finished) {
            sk.interestOps(0); // interested in nothing just yet.
            handler.handleConnect();
        } else
            cancel(sk, handler);
    }
    
    /**
     * Read data
     * 
     * @throws IOException
     */
    private void processRead(SelectionKey sk, ReadHandler handler) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Handling read: " + handler);
        
        if (!sk.isValid())
            return;
        
        handler.handleRead();
    }
    
    /**
     * Write data
     * 
     * @throws IOException
     */
    private void processWrite(SelectionKey sk, WriteHandler handler) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Handling write: " + handler);
            
        if (!sk.isValid())
            return;
        
        handler.handleWrite();
    }
    
    /**
     * Adds any pending registrations.
     */
    private void addPendingItems() {
        synchronized(PENDING) {
            for(Iterator i = PENDING.iterator(); i.hasNext(); ) {
                PendingOp next = (PendingOp)i.next();
                try {
                    next.channel.register(selector, next.op, next.handler);
                } catch(IOException iox) {
                    next.handler.handleIOException(iox);
                }
            }
            PENDING.clear();
        }
    }
    
    /**
     * The actual NIO run loop
     */
    private void process() {
        while(true) {
            // This sleep is technically not necessary, however occasionally selector
            // begins to wakeup with nothing selected.  This happens very frequently on Linux,
            // and sometimes on Windows (bugs, etc..).  The sleep prevents busy-looping.
            // It also allows pending registrations & network events to queue up so that
            // selection can handle more things in one round.
            // This is unrelated to the wakeup()-causing-busy-looping.  There's other bugs
            // that cause this.
            try {
               Thread.sleep(50);
            } catch(InterruptedException ix) {
               LOG.warn("Selector interrupted", ix);
            }
            
            addPendingItems();

            LOG.debug("Selecting");
            try {
                // see register(...) for why this has a timeout
                selector.select(100);
            } catch (NullPointerException err) {
                LOG.warn("npe", err);
                continue;
            } catch (CancelledKeyException err) {
                LOG.warn("cancelled", err);
                continue;
            } catch (IOException iox) {
                throw new RuntimeException(iox);
            }
            
            Collection keys = selector.selectedKeys();
            if(LOG.isDebugEnabled())
                LOG.debug("Selected (" + keys.size() + ") keys.");
            
            for(Iterator it = keys.iterator(); it.hasNext(); ) {
                SelectionKey sk = (SelectionKey)it.next();
                NIOHandler handler = (NIOHandler)sk.attachment();
                try {
                    if (sk.isAcceptable())
                        processAccept(sk, (AcceptHandler)handler);
                    else if(sk.isConnectable())
                        processConnect(sk, (ConnectHandler)handler);
                    else {
                        if (sk.isReadable())
                            processRead(sk, (ReadHandler)handler);
                        if (sk.isWritable())
                            processWrite(sk, (WriteHandler)handler);
                    }
                } catch (CancelledKeyException err) {
                    LOG.warn("Ignoring cancelled key", err);
                } catch(IOException iox) {
                    LOG.warn("IOX processing", iox);
                    cancel(sk, handler);
                    handler.handleIOException(iox);
                }
            }
            
            keys.clear();
        }
    }
    
    /**
     * The run loop
     */
    public void run() {
        while(true) {
            try {
                if(selector == null)
                    selector = Selector.open();
                process();
            } catch(Throwable err) {
                selector = null;
                LOG.error("Error in Selector!", err);
                ErrorService.error(err);
            } 
        }
    }
    
    /** Encapsulates a pending op. */
    private static class PendingOp {
        private final SelectableChannel channel;
        private final NIOHandler handler;
        private final int op;
    
        PendingOp(SelectableChannel channel, NIOHandler handler, int op) {
            this.channel = channel;
            this.handler = handler;
            this.op = op;
        }
    }
    
}


package com.limegroup.gnutella.io;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import com.limegroup.gnutella.util.ManagedThread;
import org.apache.commons.logging.*;

/**
 * Dispatcher for NIO.
 */
class NIODispatcher implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(NIODispatcher.class);
    
    private static final NIODispatcher INSTANCE = new NIODispatcher();
    public static final NIODispatcher instance() { return INSTANCE; }
    
    private NIODispatcher() {
        try {
            selector = Selector.open();
        } catch(IOException iox) {
            throw new RuntimeException(iox);
        }
        
        Thread t = new ManagedThread(this, "NIODispatcher");
        t.start();
    }
    
    
    private Selector selector = null;
    
    /**
     * A set of items pending registration for selection.
     */
    private final Set PENDING = new HashSet();
    
    /**
     * Register interest.
     */
    public void register(NIOHandler handler) {
        LOG.debug("Wanting to register: " + handler);
        
        SelectableChannel channel = handler.getSelectableChannel();
        SelectionKey sk = channel.keyFor(selector);
        if(sk == null || sk.interestOps() != handler.interestOps()) {
            synchronized(PENDING) {
                PENDING.add(handler);
            }
            
            selector.wakeup();
        }
    }
    
    /**
     * Cancel SelesctionKey, close Channel and "free" the attachment
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
        if(finished)
            handler.handleConnect();
        else
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
        
        SocketChannel channel = (SocketChannel)sk.channel();
        handler.handleRead();
        sk.interestOps(handler.interestOps());
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
        
        SocketChannel channel = (SocketChannel)sk.channel();
        handler.handleWrite();
        sk.interestOps(handler.interestOps());
    }
    
    /**
     * Adds any pending registrations.
     */
    private void addPendingItems() {
        synchronized(PENDING) {
            for(Iterator i = PENDING.iterator(); i.hasNext(); ) {
                NIOHandler next = (NIOHandler)i.next();
                if(LOG.isDebugEnabled())
                    LOG.debug("Adding pending: " + next);
                SelectableChannel channel = next.getSelectableChannel();
                try {
                    channel.register(selector, next.interestOps(), next);
                } catch(ClosedChannelException cce) {
                    LOG.warn("Closed while registering", cce);
                    next.handleIOException(cce);
                }
            }
            
            PENDING.clear();
        }
    }
    
    /**
     * The actual NIO run loop
     */
    private void process() {
       
        int n = -1;
                
        while(true) {
            
            try {
                Thread.sleep(50);
            } catch(InterruptedException ix) {
                LOG.warn("IX", ix);
            }
            
            addPendingItems();

            LOG.debug("Selecting");
            try {
                n = selector.select();
            } catch (NullPointerException err) {
                LOG.warn("npe", err);
                continue;
            } catch (CancelledKeyException err) {
                LOG.warn("cancelled", err);
                continue;
            } catch (IOException iox) {
                selector = null;
                throw new RuntimeException(iox);
            }
            
            if (n == 0) {
                LOG.debug("Nothing selected, continuing");
                continue;
            }
                
            Collection keys = selector.selectedKeys();
            if(LOG.isDebugEnabled())
                LOG.debug("Selected (" + n + ") keys.");
            
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
                LOG.error("Error in Selector!", err);
            } 
        }
    }
}


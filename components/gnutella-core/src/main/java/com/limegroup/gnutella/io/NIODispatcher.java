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
    
    /** Lock for pending items. */
    private final Object PENDING_LOCK = new Object();
    /** List of pending channels/attachments wanting OP_ACCEPT */
    private final List PENDING_ACCEPT = new LinkedList();
    /** List of pending channels/attachments wanting OP_CONNECT */
    private final List PENDING_CONNECT = new LinkedList();
    /** List of pending channels/attachments wanting OP_READ */
    private final List PENDING_READ = new LinkedList();
    /** List of pending channels/attachments wanting OP_WRITE */
    private final List PENDING_WRITE = new LinkedList();
    /** List of pending channels/attachments wanting OP_WRITE & OP_READ */
    private final List PENDING_RW = new LinkedList();
    
    /** Register interest in accepting */
    void registerAccept(SelectableChannel channel, NIOHandler attachment) {
        register(PENDING_ACCEPT, channel, attachment);
    }
    
    /** Register interest in connecting */
    void registerConnect(SelectableChannel channel, NIOHandler attachment) {
        register(PENDING_CONNECT, channel, attachment);
    }
    
    /** Register interest in reading */
    void registerRead(SelectableChannel channel, NIOHandler attachment) {
        register(PENDING_READ, channel, attachment);
    }
    
    /** Register interest in writing */
    void registerWrite(SelectableChannel channel, NIOHandler attachment) {
        register(PENDING_WRITE, channel, attachment);
    }
    
    /** Register interest in both reading & writing */
    void registerReadWrite(SelectableChannel channel, NIOHandler attachment) {
        register(PENDING_RW, channel, attachment);
    }
    
    /** Register interest */
    private void register(List list, SelectableChannel channel, NIOHandler attachment) {
        synchronized(PENDING_LOCK) {
            list.add(new Object[] { channel, attachment });
        }
    }
    
    /** Registers a SelectableChannel as being interested in a write again. */
    void interestWrite(SelectableChannel channel) {
        SelectionKey sk = channel.keyFor(selector);
        sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
    }
    
    /** Registers a SelectableChannel as being interested in a read again. */
    void interestRead(SelectableChannel channel) {
        SelectionKey sk = channel.keyFor(selector);
        sk.interestOps(sk.interestOps() | SelectionKey.OP_READ);
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
        if(finished) {
            handler.handleConnect();
            sk.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
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
        
        SocketChannel channel = (SocketChannel)sk.channel();
        boolean more = handler.handleRead();
        if(!more)
            sk.interestOps(sk.interestOps() & ~SelectionKey.OP_READ);
        // else it's already set.
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
        boolean more  = handler.handleWrite();
        if(!more)
            sk.interestOps(sk.interestOps() & ~SelectionKey.OP_WRITE);
        // else it's already set.
    }
    
    /**
     * Adds any pending registrations.
     */
    private void addPendingItems() {
        synchronized(PENDING_LOCK) {
            doRegister(PENDING_ACCEPT, SelectionKey.OP_ACCEPT);
            doRegister(PENDING_CONNECT, SelectionKey.OP_CONNECT);
            doRegister(PENDING_READ, SelectionKey.OP_READ);
            doRegister(PENDING_WRITE, SelectionKey.OP_WRITE);
            doRegister(PENDING_RW, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
    }
    
    /** Register pending items from a list with the given op */
    private void doRegister(List l, int op) {
        for(Iterator i = l.iterator(); i.hasNext(); ) {
            Object[] next = (Object[])i.next();
            SelectableChannel channel = (SelectableChannel)next[0];
            NIOHandler attachment = (NIOHandler)next[1];
            try {
                channel.register(selector, op, attachment);
            } catch(IOException iox) {
                attachment.handleIOException(iox);
            }
        }
    }
    
    /**
     * The actual NIO run loop
     */
    private void process() {
       
        int n = -1;
                
        while(true) {
            addPendingItems();

            LOG.debug("Selecting");
            try {
                n = selector.select(50);
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
            
            if (n == 0)
                continue;
                
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


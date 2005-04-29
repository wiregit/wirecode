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
import java.util.HashSet;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.util.ManagedThread;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Dispatcher for NIO.
 *
 * To register interest initially in either reading, writing, accepting, or connecting,
 * use registerRead, registerWrite, registerReadWrite, registerAccept, or registerConnect.
 *
 * When handling events, interest is done different ways.  A channel registered for accepting
 * will remain registered for accepting until that channel is closed.  There is no way to 
 * turn off interest in accepting.  A channel registered for connecting will turn off all
 * interest (for any operation) once the connect event has been handled.  Channels registered
 * for reading or writing must manually change their interest when they no longer want to
 * receive events (and must turn it back on when events are wanted).
 *
 * To change interest in reading or writing, use interestRead(SelectableChannel, boolean) or
 * interestWrite(SelectableChannel, boolean) with the appropriate boolean parameter.  The
 * channel must have already been registered with the dispatcher.  If it was not registered,
 * changing interest is a no-op.  The attachment the channel was registered with must also
 * implement the appropriate Observer to handle read or write events.  If interest in an event
 * is turned on but the attachment does not implement that Observer, a ClassCastException will
 * be thrown while attempting to handle that event.
 *
 * If any unhandled events occur while processing an event for a specific Observer, that Observer
 * will be shutdown and will no longer receive events.  If any IOExceptions occur while handling
 * events for an Observer, handleIOException is called on that Observer.
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
        
        dispatchThread = new ManagedThread(this, "NIODispatcher");
        dispatchThread.start();
    }
    
    /** The thread this is being run on. */
    private final Thread dispatchThread;
    
    /** The selector this uses. */
    private Selector selector = null;
	
	/** Queue lock. */
	private final Object Q_LOCK = new Object();
    
    /** Pending queue. */
    private final Collection /* of PendingOp */ PENDING = new LinkedList();
    
    /** Closing queue. */
    private final Collection /* of Shutdownable */ CLOSING = new HashSet();
	
	/** Interest queue. */
	private final Collection /* of InterestOp */ INTEREST = new LinkedList();
	
	/** The invokeLater queue. */
    private final Collection /* of Runnable */ LATER = new LinkedList();
	
	/** Determine if this is the dispatch thread. */
	public boolean isDispatchThread() {
	    return Thread.currentThread() == dispatchThread;
	}
	    
    /** Register interest in accepting */
    public void registerAccept(SelectableChannel channel, AcceptObserver attachment) {
        register(channel, attachment, SelectionKey.OP_ACCEPT);
    }
    
    /** Register interest in connecting */
    public void registerConnect(SelectableChannel channel, ConnectObserver attachment) {
        register(channel, attachment, SelectionKey.OP_CONNECT);
    }
    
    /** Register interest in reading */
    public void registerRead(SelectableChannel channel, ReadObserver attachment) {
        register(channel, attachment, SelectionKey.OP_READ);
    }
    
    /** Register interest in writing */
    public void registerWrite(SelectableChannel channel, WriteObserver attachment) {
        register(channel, attachment, SelectionKey.OP_WRITE);
    }
    
    /** Register interest in both reading & writing */
    public void registerReadWrite(SelectableChannel channel, ReadWriteObserver attachment) {
        register(channel, attachment, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
    
    /** Register interest */
    private void register(SelectableChannel channel, IOErrorObserver handler, int op) {
		if(Thread.currentThread() == dispatchThread) {
			try {
				channel.register(selector, op, handler);
			} catch(IOException iox) {
                handler.handleIOException(iox);
            }
		} else {
	        synchronized(Q_LOCK) {
				PENDING.add(new PendingOp(channel, handler, op));
			}
        }
    }
    
    /**
     * Registers a SelectableChannel as being interested in a write again.
     *
     * You must ensure that the attachment that handles events for this channel
     * implements WriteObserver.  If not, a ClassCastException will be thrown
     * while handling write events.
     */
    public void interestWrite(SelectableChannel channel, boolean on) {
        interest(channel, SelectionKey.OP_WRITE, on);
    }
    
    /**
     * Registers a SelectableChannel as being interested in a read again.
     *
     * You must ensure that the attachment that handles events for this channel
     * implements ReadObserver.  If not, a ClassCastException will be thrown
     * while handling read events.
     */
    public void interestRead(SelectableChannel channel, boolean on) {
        interest(channel, SelectionKey.OP_READ, on);
    }    
    
    /** Registers interest on the channel for the given op */
    private void interest(SelectableChannel channel, int op, boolean on) {
		if(Thread.currentThread() == dispatchThread) {		
			SelectionKey sk = channel.keyFor(selector);
			if(sk != null && sk.isValid()) {
				if(on)
					sk.interestOps(sk.interestOps() | op);
				else
					sk.interestOps(sk.interestOps() & ~op);
			}
		} else {
			synchronized(Q_LOCK) {
				INTEREST.add(new InterestOp(channel, op, on));
			}
		}
    }
    
    /** Shuts down the handler, possibly scheduling it for shutdown in the NIODispatch thread. */
    public void shutdown(Shutdownable handler) {
        if(Thread.currentThread() == dispatchThread) {
            handler.shutdown();
        } else {
            synchronized(Q_LOCK) {
                CLOSING.add(handler);
            }
        }
    }    
    
    /** Invokes the method in the NIODispatch thread. */
   public void invokeLater(Runnable runner) {
        if(Thread.currentThread() == dispatchThread) {
            runner.run();
        } else {
            synchronized(Q_LOCK) {
                LATER.add(runner);
            }
        }
    }
    
    /**
     * Cancel SelectionKey & shuts down the handler.
     */
    private void cancel(SelectionKey sk, Shutdownable handler) {
        sk.cancel();
        if(handler != null)
            handler.shutdown();
    }
    
        
    /**
     * Accept an icoming connection
     * 
     * @throws IOException
     */
    private void processAccept(SelectionKey sk, AcceptObserver handler) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Handling accept: " + handler);
        
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
    private void processConnect(SelectionKey sk, ConnectObserver handler) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Handling connect: " + handler);        
            
        SocketChannel channel = (SocketChannel)sk.channel();
        
        boolean finished = channel.finishConnect();
        if(finished) {
            sk.interestOps(0); // interested in nothing just yet.
            handler.handleConnect();
        } else {
            cancel(sk, handler);
        }
    }
    
    /**
     * Adds any pending registrations.
     */
    private void addPendingItems() {
        synchronized(Q_LOCK) {
            if(!PENDING.isEmpty()) {
                for(Iterator i = PENDING.iterator(); i.hasNext(); ) {
                    PendingOp next = (PendingOp)i.next();
                    try {
                        next.channel.register(selector, next.op, next.handler);
                    } catch(IOException iox) {
                        try {
                            next.handler.handleIOException(iox);
                        } catch(Throwable t) {
                            ErrorService.error(t);
                        }
                    }
                }
                PENDING.clear();
            }
			
			if(!INTEREST.isEmpty()) {
				for(Iterator i = INTEREST.iterator(); i.hasNext(); ) {
					InterestOp next = (InterestOp)i.next();
					SelectionKey sk = next.channel.keyFor(selector);
					if(sk != null && sk.isValid()) {
						if(next.on)
							sk.interestOps(sk.interestOps() | next.op);
						else
							sk.interestOps(sk.interestOps() & ~next.op);
					}
				}
				INTEREST.clear();
			}
			
            if(!CLOSING.isEmpty()) {
                for(Iterator i = CLOSING.iterator(); i.hasNext(); ) {
                    try {
                        ((Shutdownable)i.next()).shutdown();
                    } catch(Throwable t) {
                        ErrorService.error(t);
                    }
                }
                CLOSING.clear();
            }
            
            if(!LATER.isEmpty()) {
                for(Iterator i = LATER.iterator(); i.hasNext(); ) {
                    try {
                        ((Runnable)i.next()).run();
                    } catch(Throwable t) {
                        ErrorService.error(t);
                    }
                }
                LATER.clear();
            }
			
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
            if(keys.size() == 0)
                continue;
            
            if(LOG.isDebugEnabled())
                LOG.debug("Selected (" + keys.size() + ") keys.");
            
            for(Iterator it = keys.iterator(); it.hasNext(); ) {
                SelectionKey sk = (SelectionKey)it.next();
				if(!sk.isValid())
					continue;
				
                Object attachment = sk.attachment();
				try {
                    try {
                        if (sk.isAcceptable())
                            processAccept(sk, (AcceptObserver)attachment);
                        else if(sk.isConnectable())
                            processConnect(sk, (ConnectObserver)attachment);
                        else {
                            if (sk.isReadable())
                                ((ReadObserver)attachment).handleRead();
                            if (sk.isWritable())
                                ((WriteObserver)attachment).handleWrite();
                        }
                    } catch (CancelledKeyException err) {
                        LOG.warn("Ignoring cancelled key", err);
                    } catch(IOException iox) {
                        LOG.warn("IOX processing", iox);
                        ((IOErrorObserver)attachment).handleIOException(iox);
                    }
                } catch(Throwable t) {
                    ErrorService.error(t, "Unhandled exception while dispatching");

                    try {
                        if(attachment instanceof Shutdownable)
                            cancel(sk, (Shutdownable)attachment);
                        else
                            cancel(sk, null);
                    } catch(Throwable ignored) {}
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
        private final IOErrorObserver handler;
        private final int op;
    
        PendingOp(SelectableChannel channel, IOErrorObserver handler, int op) {
            this.channel = channel;
            this.handler = handler;
            this.op = op;
        }
    }
	
	/** Encapsulate an interest op. */
	private static class InterestOp {
		private final SelectableChannel channel;
		private final int op;
		private final boolean on;
		InterestOp(SelectableChannel channel, int op, boolean on) {
			this.channel = channel;
			this.op = op;
			this.on = on;
		}
	}
    
}


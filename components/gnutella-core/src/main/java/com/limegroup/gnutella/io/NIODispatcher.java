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
import java.util.ArrayList;
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
    
    /** Register queue. */
    private final Collection /* of RegisterOp */ REGISTER = new LinkedList();
	
	/** The invokeLater queue. */
    private final Collection /* of Runnable */ LATER = new LinkedList();
    
    /** The throttle queue. */
    private final List /* of Throttle */ THROTTLE = new ArrayList();
    
    /**
     * Temporary list used where REGISTER & LATER are combined, so that
     * handling IOException or running arbitrary code can't deadlock.
     * Otherwise, it could be possible that one thread locked some arbitrary
     * Object and then tried to acquire Q_LOCK by registering or invokeLatering.
     * Meanwhile, the NIODispatch thread may be running pending items and holding
     * Q_LOCK.  If while running those items it tries to lock that arbitrary
     * Object, deadlock would occur.
     */
    private final ArrayList UNLOCKED = new ArrayList();
     
	
	/** Determine if this is the dispatch thread. */
	public boolean isDispatchThread() {
	    return Thread.currentThread() == dispatchThread;
	}
	
	/** Adds a Throttle into the throttle requesting loop. */
	public void addThrottle(Throttle t) {
        synchronized(Q_LOCK) {
            THROTTLE.add(t);
        }
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
				REGISTER.add(new RegisterOp(channel, handler, op));
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
        try {
			SelectionKey sk = channel.keyFor(selector);
			if(sk != null && sk.isValid()) {
			    // We must synchronize on something unique to each key,
			    // (but not the key itself, 'cause that'll interfere with Selector.select)
                // so that multiple threads calling interest(..) will be atomic with
                // respect to each other.  Otherwise, one thread can preempt another's
                // interest setting, and one of the interested ops may be lost.
			    synchronized(channel.blockingLock()) {
    				if(on)
    					sk.interestOps(sk.interestOps() | op);
    				else
    					sk.interestOps(sk.interestOps() & ~op);
                }
			}
        } catch(CancelledKeyException ignored) {
            // Because closing can happen in any thread, the key may be cancelled
            // between the time we check isValid & the time that interestOps are
            // set or gotten.
        }
    }
    
    /** Shuts down the handler, possibly scheduling it for shutdown in the NIODispatch thread. */
    public void shutdown(Shutdownable handler) {
        handler.shutdown();
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
     * Adds any pending actions.
     *
     * This works by adding any pending actions into a temporary list so that actions
     * to the outside world don't need to hold Q_LOCK.
     *
     * Interaction with UNLOCKED doesn't need to hold a lock, because it's only used
     * in the NIODispatch thread.
     *
     * Throttle is not moved to UNLOCKED because it is not cleared, and because the
     * actions are all within this package, so we can guarantee that it doesn't
     * deadlock.
     */
    private void addPendingItems() {
        synchronized(Q_LOCK) {
            long now = System.currentTimeMillis();
            for(int i = 0; i < THROTTLE.size(); i++)
                ((Throttle)THROTTLE.get(i)).tick(now);

            UNLOCKED.ensureCapacity(REGISTER.size() + LATER.size());
            UNLOCKED.addAll(REGISTER);
            UNLOCKED.addAll(LATER);
            REGISTER.clear();
            LATER.clear();
        }
        
        if(!UNLOCKED.isEmpty()) {
            for(Iterator i = UNLOCKED.iterator(); i.hasNext(); ) {
                Object item = i.next();
                try {
                    if(item instanceof RegisterOp) {
                        RegisterOp next = (RegisterOp)item;
                        try {
                            next.channel.register(selector, next.op, next.handler);
                        } catch(IOException iox) {
                            next.handler.handleIOException(iox);
                        }
                    } else if(item instanceof Runnable) {
                        ((Runnable)item).run();
                    } 
                } catch(Throwable t) {
                    LOG.error(t);
                    ErrorService.error(t);
                }
            }
            UNLOCKED.clear();
        }
    }
    
    /**
     * Loops through all Throttles and gives them the ready keys.
     */
    private void readyThrottles(Collection keys) {
        synchronized(Q_LOCK) {
            for(int i = 0; i < THROTTLE.size(); i++)
                ((Throttle)THROTTLE.get(i)).selectableKeys(keys);
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
            
            readyThrottles(keys);
            
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
    
    /** Encapsulates a register op. */
    private static class RegisterOp {
        private final SelectableChannel channel;
        private final IOErrorObserver handler;
        private final int op;
    
        RegisterOp(SelectableChannel channel, IOErrorObserver handler, int op) {
            this.channel = channel;
            this.handler = handler;
            this.op = op;
        }
    }
    
}


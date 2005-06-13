package com.limegroup.gnutella.io;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

import java.util.Collection;
import java.util.Collections;
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
 * A channel registering for a connect can specify a timeout.  If the timeout is greater than
 * 0 and a connect event hasn't happened in that length of time, the channel will be cancelled 
 * and handleIOException will be called on the Observer. 
 *
 * When handling events, future interest is done different ways.  A channel registered for accepting
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
    private final List /* of NBThrottle */ THROTTLE = new ArrayList();
    
    /** The timeout queue. */
    private final List /* of Timeout */ TIMEOUTS = new ArrayList();
    
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
	// TODO: have some way to remove Throttles, or make these use WeakReferences
	public void addThrottle(NBThrottle t) {
        synchronized(Q_LOCK) {
            THROTTLE.add(t);
        }
    }
	    
    /** Register interest in accepting */
    public void registerAccept(SelectableChannel channel, AcceptObserver attachment) {
        register(channel, attachment, SelectionKey.OP_ACCEPT, 0);
    }
    
    /** Register interest in connecting */
    public void registerConnect(SelectableChannel channel, ConnectObserver attachment, int timeout) {
        register(channel, attachment, SelectionKey.OP_CONNECT, timeout);
    }
    
    /** Register interest in reading */
    public void registerRead(SelectableChannel channel, ReadObserver attachment) {
        register(channel, attachment, SelectionKey.OP_READ, 0);
    }
    
    /** Register interest in writing */
    public void registerWrite(SelectableChannel channel, WriteObserver attachment) {
        register(channel, attachment, SelectionKey.OP_WRITE, 0);
    }
    
    /** Register interest in both reading & writing */
    public void registerReadWrite(SelectableChannel channel, ReadWriteObserver attachment) {
        register(channel, attachment, SelectionKey.OP_READ | SelectionKey.OP_WRITE, 0);
    }
    
    /** Register interest */
    private void register(SelectableChannel channel, IOErrorObserver handler, int op, int timeout) {
		if(Thread.currentThread() == dispatchThread) {
			try {
				SelectionKey key = channel.register(selector, op, handler);
                if(timeout != 0)
		            addNotify(key, handler, timeout, System.currentTimeMillis());
			} catch(IOException iox) {
                handler.handleIOException(iox);
            }
		} else {
	        synchronized(Q_LOCK) {
				REGISTER.add(new RegisterOp(channel, handler, op, timeout));
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
        removeNotify(sk, handler);

        SocketChannel channel = (SocketChannel)sk.channel();
        
        boolean finished = channel.finishConnect();
        if(finished) {
            sk.interestOps(0); // interested in nothing just yet.
            handler.handleConnect(channel.socket());
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
        long now = System.currentTimeMillis();
        synchronized(Q_LOCK) {
            for(int i = 0; i < THROTTLE.size(); i++)
                ((NBThrottle)THROTTLE.get(i)).tick(now);

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
                            SelectionKey key = next.channel.register(selector, next.op, next.handler);
                            if(next.timeout != 0)
                                addNotify(key, next.handler, next.timeout, now);
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
                ((NBThrottle)THROTTLE.get(i)).selectableKeys(keys);
        }
    }
    
    /**
     * Processes all the items that can be timed out.  If the current time
     * is beyond the timeout time, cancels the item & generates a handleIOException
     * on the item.
     */
    private void processTimeouts() {
        if(!TIMEOUTS.isEmpty()) {
            long now = System.currentTimeMillis();
            for(int i = TIMEOUTS.size() - 1; i >= 0; i--) {
                Timeout next = (Timeout)TIMEOUTS.get(i);
                if(now >= next.dead) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Killing timed out connect: " + next.handler);
                    cancel(next.key, next.handler); 
                    next.handler.handleIOException(new SocketTimeoutException("no connection in: " + next.timeout));
                    TIMEOUTS.remove(i);
                } else {
                    break; // TIMEOUTS is sorted.
                }
            }
        }
    }
    
    /**
     * Adds a single item into TIMEOUTs, in order.
     */
    private void addNotify(SelectionKey key, IOErrorObserver handler, int timeout, long now) {
        long then = now + timeout;
        Timeout t = new Timeout(key, handler, timeout, then);
        if(TIMEOUTS.isEmpty()) {
            // Common case.
            TIMEOUTS.add(t);
        } else if(then >= ((Timeout)TIMEOUTS.get(TIMEOUTS.size() - 1)).dead) {
            // Another common case.
            TIMEOUTS.add(t);
        } else {
            // Quick lookup.
            int insertion = Collections.binarySearch(TIMEOUTS, t);
            if(insertion < 0)
                insertion = (insertion + 1) * -1;
            TIMEOUTS.add(insertion, t);
        }
    }
    
    /**
     * Removes a single ConnectObserver & SelectionKey from TIMEOUTS.
     */
    private void removeNotify(SelectionKey key, IOErrorObserver handler) {
        for(int i = TIMEOUTS.size() - 1; i >= 0; i--) {
            Timeout next = (Timeout)TIMEOUTS.get(i);
            if(next.key == key && next.handler == handler) {
                TIMEOUTS.remove(i);
                break;
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
            
           // if(LOG.isDebugEnabled())
           //     LOG.debug("Selected (" + keys.size() + ") keys.");
            
            readyThrottles(keys);
            
            for(Iterator it = keys.iterator(); it.hasNext(); ) {
                SelectionKey sk = (SelectionKey)it.next();
				if(sk.isValid())
                    process(sk, sk.attachment(), 0xFFFF);
            }
            
            keys.clear();
            
            processTimeouts();
        }
    }
    
    /**
     * Processes a single SelectionKey & attachment, processing only
     * ops that are in allowedOps.
     */
    void process(SelectionKey sk, Object attachment, int allowedOps) {
		try {
            try {
                if ((allowedOps & SelectionKey.OP_ACCEPT) != 0 && sk.isAcceptable())
                    processAccept(sk, (AcceptObserver)attachment);
                else if((allowedOps & SelectionKey.OP_CONNECT)!= 0 && sk.isConnectable())
                    processConnect(sk, (ConnectObserver)attachment);
                else {
                    if ((allowedOps & SelectionKey.OP_READ) != 0 && sk.isReadable())
                        ((ReadObserver)attachment).handleRead();
                    if ((allowedOps & SelectionKey.OP_WRITE) != 0 && sk.isWritable())
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
        private final int timeout;
    
        RegisterOp(SelectableChannel channel, IOErrorObserver handler, int op, int timeout) {
            this.channel = channel;
            this.handler = handler;
            this.op = op;
            this.timeout = timeout;
        }
    }
    
    /** Encapsulates an IOErrorObserver on a timing-out operation. */
    private static class Timeout implements Comparable {
        private final SelectionKey key;
        private final IOErrorObserver handler;
        private final int timeout;
        private final long dead;
        
        Timeout(SelectionKey key, IOErrorObserver handler, int timeout, long dead) {
            this.key = key;
            this.handler = handler;
            this.timeout = timeout;
            this.dead = dead;
        }
        
        public int compareTo(Object a) {
            Timeout o = (Timeout)a;
            return dead > o.dead ? 1 : dead < o.dead ? -1 : 0;
        }
    }
        
    
}


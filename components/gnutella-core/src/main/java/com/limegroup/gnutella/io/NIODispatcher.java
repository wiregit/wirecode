package com.limegroup.gnutella.io;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.SchedulingThreadPool;

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
    
    private final SchedulingThreadPool SCHEDULER =
    	new MyThreadPool();
    
    /**
     * Constructs the sole NIODispatcher, starting its thread.
     */
    private NIODispatcher() {
        boolean failed = false;
        try {
            primarySelector = Selector.open();
        } catch(IOException iox) {
            failed = true;
        }
        
        if(!failed) {        
            dispatchThread = new ManagedThread(this, "NIODispatcher");
            dispatchThread.start();
        } else {
            dispatchThread = null;
        }
    }
    
    /**
     * Maximum number of times Selector can return quickly without having anything
     * selected.
     */
    private static final long SPIN_AMOUNT = 5000;
    
    /** Ignore up to this many non-zero selects when suspecting selector is broken */
    private static final int MAX_IGNORES = 5;
    
    /** The length of time between clearing intervals for the cache. */
    private static final long CACHE_CLEAR_INTERVAL = 30000;
    
    /** The thread this is being run on. */
    private final Thread dispatchThread;
    
    /** Queue lock. */
    private final Object Q_LOCK = new Object();
    
    /**
     * A map of classes of SelectableChannels to the Selector that should
     * be used to register that channel with.
     */
    private final Map <Class,Selector>  OTHER_SELECTORS = 
    	new HashMap<Class,Selector>();
    
    /** A list of other Selectors that should be polled. */
    private final List <Selector> POLLERS = new ArrayList<Selector>();
    
    /** The invokeLater queue. */
    private Collection <Runnable> LATER = new LinkedList<Runnable>();
    
    private final Queue<DelayedRunnable> DELAYED = new DelayQueue<DelayedRunnable>();
    
    /** The throttle queue. */
    private final List <NBThrottle> THROTTLE = new ArrayList<NBThrottle>();
    
    /** The timeout manager. */
    private final TimeoutController TIMEOUTER = new TimeoutController();
    
    /**
     * A common ByteBufferCache that classes can use.
     * TODO: Move somewhere else.
     */
    private final ByteBufferCache BUFFER_CACHE = new ByteBufferCache();
    
    /** The selector this uses. */
    private Selector primarySelector = null;
    
    /** The current iteration of selection. */
    private long iteration = 0;
    
    /** Whether or not we've tried to wake up the selector. */
    private volatile boolean wokeup = false;
    
    /** The last time the ByteBufferCache was cleared. */
    private long lastCacheClearTime;
    
    /** Returns true if the NIODispatcher is merrily chugging along. */
    public boolean isRunning() {
        return dispatchThread != null;
    }
	
	/** Determine if this is the dispatch thread. */
	public boolean isDispatchThread() {
	    return Thread.currentThread() == dispatchThread;
	}
    
    /** Gets the common ByteBufferCache */
    public ByteBufferCache getBufferCache() {
        return BUFFER_CACHE;
    }
    
    /** Returns the number of timeouts that are pending. */
    public int getNumPendingTimeouts() {
        return TIMEOUTER.getNumPendingTimeouts();
    }
	
	/** Adds a Throttle into the throttle requesting loop. */
	// TODO: have some way to remove Throttles, or make these use WeakReferences
	public void addThrottle(final NBThrottle t) {
        if(Thread.currentThread() == dispatchThread)
            THROTTLE.add(t);
        else {
            invokeLater(new Runnable() {
                public void run() {
                    THROTTLE.add(t);
                }
            });
        }
    }
    
    /** Registers a channel for nothing. */
    public void register(SelectableChannel channel, IOErrorObserver attachment) {
        register(channel, attachment, 0, 0);
    }
	    
    /** Register interest in accepting */
    public void registerAccept(SelectableChannel channel, AcceptChannelObserver attachment) {
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
		    registerImpl(getSelectorFor(channel), channel, op, handler, timeout);
		} else {
	        synchronized(Q_LOCK) {
				LATER.add(new RegisterOp(channel, handler, op, timeout));
			}
            wakeup();
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
            Selector sel = getSelectorFor(channel);
			SelectionKey sk = channel.keyFor(sel);
			if(sk != null && sk.isValid()) {
			    // We must synchronize on something unique to each key,
			    // (but not the key itself, 'cause that'll interfere with Selector.select)
                // so that multiple threads calling interest(..) will be atomic with
                // respect to each other.  Otherwise, one thread can preempt another's
                // interest setting, and one of the interested ops may be lost.
                int oldOps;
			    synchronized(sk.attachment()) {
                    if((op & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                        ((Attachment)sk.attachment()).changeReadStatus(on);
                    }
                    
                    oldOps = sk.interestOps();
                    
    				if(on)
    					sk.interestOps(oldOps | op);
    				else
    					sk.interestOps(oldOps & ~op);
                }
                
                // if we're turning it on and it wasn't on before...
                if(on && (oldOps & op) != op)
                    wakeup();
			}
        } catch(CancelledKeyException ignored) {
            // Because closing can happen in any thread, the key may be cancelled
            // between the time we check isValid & the time that interestOps are
            // set or gotten.
        }
    }
    
    /** Returns the Selector that should be used for the given channel. */
    private Selector getSelectorFor(SelectableChannel channel) {
        Selector sel = OTHER_SELECTORS.get(channel.getClass());
        if(sel == null)
            return primarySelector; // default selector
        else
            return sel;      // custom selector
    }
    
    /** Shuts down the handler, possibly scheduling it for shutdown in the NIODispatch thread. */
    public void shutdown(Shutdownable handler) {
        handler.shutdown();
    }
    
    /** Attaches the given attachment to the SelectionKey. */
    public void attach(SelectionKey key, IOErrorObserver attachment) {
        key.attach(new Attachment(attachment));
    }
    
    /**
     * Registers a new Selector that should be used when SelectableChannels
     * assignable from the given class are registered.
     */
    public void registerSelector(final Selector newSelector, final Class channelClass) {
        if(Thread.currentThread() == dispatchThread) {
            POLLERS.add(newSelector);
            OTHER_SELECTORS.put(channelClass, newSelector);
        } else {
            invokeLater(new Runnable() {
                public void run() {
                    POLLERS.add(newSelector);
                    OTHER_SELECTORS.put(channelClass, newSelector);
                }
            });
        }
    }
    
    /**
     * Removes a registered Selector.
     */
    public void removeSelector(final Selector selector) {
        if(Thread.currentThread() == dispatchThread) {
            POLLERS.remove(selector);
            OTHER_SELECTORS.remove(selector);
        } else {
            invokeLater(new Runnable() {
                public void run() {
                    POLLERS.remove(selector);
                    OTHER_SELECTORS.remove(selector);
                }
            });
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
            wakeup();
        }
    }
   
   /** Same as invokeLater, except forces the Runnable to be done later on. */
   public void invokeReallyLater(Runnable runner) {
        synchronized(Q_LOCK) {
            LATER.add(runner);
        }
        wakeup();
    }
   

   public java.util.concurrent.Future invokeLater(Runnable later, long delay) {
	   DelayedRunnable ret = new DelayedRunnable(later, delay);
	   synchronized(Q_LOCK) {
		   DELAYED.add(ret);
	   }
	   return ret;
   }
   
   /** Invokes the method in the NIODispatcher thread & returns after it ran. */
   public void invokeAndWait(final Runnable future) throws InterruptedException {
       if(Thread.currentThread() == dispatchThread) {
           future.run();
       } else {
           Runnable waiter = new Runnable() {
               public void run() {
                   future.run();
                   synchronized(this) {
                       notify();
                   }
               }
           };
           
           synchronized(waiter) {
               synchronized(Q_LOCK) {
                   LATER.add(waiter);
               }
               wakeup();
               waiter.wait();
           }
       }
   }
    
    /** Gets the underlying attachment for the given SelectionKey's attachment. */
    public IOErrorObserver attachment(Object proxyAttachment) {
        return ((Attachment)proxyAttachment).attachment;
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
    private void processAccept(long now, SelectionKey sk, AcceptChannelObserver handler, Attachment proxy) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug("Handling accept: " + handler);
        
        ServerSocketChannel ssc = (ServerSocketChannel)sk.channel();
        SocketChannel channel = ssc.accept();
        
        if (channel == null)
            return;
        
        if (channel.isOpen()) {
            channel.configureBlocking(false);
            handler.handleAcceptChannel(channel);
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
    private void processConnect(long now, SelectionKey sk, ConnectObserver handler, Attachment proxy)
      throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Handling connect: " + handler);

        SocketChannel channel = (SocketChannel) sk.channel();
        proxy.clearTimeout();

        boolean finished = channel.finishConnect();
        if (finished) {
            sk.interestOps(0); // interested in nothing just yet.
            handler.handleConnect(channel.socket());
        } else {
            cancel(sk, handler);
        }
    }
    
    /** Process a channel read operation. */
    private void processRead(long now, ReadObserver handler, Attachment proxy) throws IOException {
        proxy.updateReadTimeout(now);
        handler.handleRead();
    }
    
    /** Process a channel write operation. */
    private void processWrite(long now, WriteObserver handler, Attachment proxy) throws IOException {
        handler.handleWrite();
    }
    
    /**
     * Does a real registration.
     */
    private void registerImpl(Selector selector, SelectableChannel channel, int op,
                              IOErrorObserver attachment, int timeout) {
        try {
            Attachment guard = new Attachment(attachment);
            SelectionKey key = channel.register(selector, op, guard);
            guard.setKey(key);
            if(timeout != 0) 
                guard.addTimeout(System.currentTimeMillis(), timeout);
            else if((op & SelectionKey.OP_READ) != 0)
                guard.changeReadStatus(true);
        } catch(IOException iox) {
            attachment.handleIOException(iox);
        }
    }
    
    /**
     * Adds any pending actions.
     *
     * This works by adding any pending actions into a local list and then replacing
     * LATER with a new list.  This is done so that actions to the outside world
     * don't need to hold Q_LOCK.
     *
     * Throttle is ticked outside the lock because ticking only hits items in this
     * package and we can ensure it doesn't deadlock.
     */
    private void runPendingTasks() {
        long now = System.currentTimeMillis();
        Collection<Runnable> localLater;
        synchronized(Q_LOCK) {
            localLater = LATER;
            LATER = new LinkedList<Runnable>();

            DelayedRunnable delayed = null;
            while ((delayed = DELAYED.poll()) != null) {
            	if (!delayed.isCancelled() && !delayed.isDone())
            		localLater.add(delayed);
            }
        }
        
        if(now > lastCacheClearTime + CACHE_CLEAR_INTERVAL) {
            BUFFER_CACHE.clearCache();
            lastCacheClearTime = now;
        }
        
        if(!localLater.isEmpty()) {
            for(Runnable item : localLater ) {
                try {
                    item.run();
                } catch(Throwable t) {
                    LOG.error(t);
                    ErrorService.error(t);
                }
            }
        }
        
        now = System.currentTimeMillis();
        for(NBThrottle t: THROTTLE)
            t.tick(now);
    }
    
    /**
     * Runs through all secondary Selectors and returns a 
     * Collection of SelectionKeys that they selected.
     */
    private Collection <SelectionKey> pollOtherSelectors() {
        Collection<SelectionKey> ret = null;
        boolean growable = false;
        
        // Optimized to not create collection objects unless absolutely
        // necessary.
        for (Selector sel: POLLERS) {
            int n = 0;
            try {
                n = sel.selectNow();
            } catch(IOException iox) {
                LOG.error("Error performing secondary select", iox);
            }
            
            if(n != 0) {
                Collection<SelectionKey> selected = sel.selectedKeys();
                if(!selected.isEmpty()) {
                    if(ret == null) {
                        ret = selected;
                    } else if(!growable) {
                        growable = true;
                        ret = new HashSet<SelectionKey>(ret);
                        ret.addAll(selected);
                    } else {
                        ret.addAll(selected);
                    }
                }
            }
        }
        
        return ret == null ? Collections.EMPTY_SET : ret;
    }
    
    /**
     * Loops through all Throttles and gives them the ready keys.
     */
    private void readyThrottles(Collection<SelectionKey> keys) {
    	for (NBThrottle t : THROTTLE)
            t.selectableKeys(keys);
    }
    
    /**
     * Wakes up the primary selector if it wasn't already woken up,
     * and the current thread is not the dispatch thread.
     */
    void wakeup() {
        if(!wokeup && Thread.currentThread() != dispatchThread) {
            wokeup = true;
            primarySelector.wakeup();
        }
    }
    
    /**
     * The actual NIO run loop
     */
    private void process() throws ProcessingException, SpinningException {
        boolean checkTime = false;
        long startSelect = -1;
        int zeroes = 0;
        int ignores = 0;
        
        while(true) {
            runPendingTasks();
            
            Collection<SelectionKey> polled = pollOtherSelectors();
            long nextTickDelay = nextTickDelay();
            boolean immediate = !polled.isEmpty() || nextTickDelay == 0;
            try {
                if(!immediate && checkTime)
                    startSelect = System.currentTimeMillis();
                
                if(!immediate)
                    primarySelector.select(Math.min(100,nextTickDelay));
                else
                    primarySelector.selectNow();
            } catch (NullPointerException err) {
                LOG.warn("npe", err);
                continue;
            } catch (CancelledKeyException err) {
                LOG.warn("cancelled", err);
                continue;
            } catch (IOException iox) {
                throw new ProcessingException(iox);
            }
            
            Collection<SelectionKey> keys = primarySelector.selectedKeys();
            if(!immediate && !wokeup) {
                if(keys.isEmpty()) {
                    long now = System.currentTimeMillis();
                    if(startSelect == -1) {
                        LOG.trace("No keys selected, starting spin check.");
                        checkTime = true;
                    } else if(startSelect + 30 >= now) {
                        if(LOG.isWarnEnabled())
                            LOG.warn("Spinning detected, current spins: " + zeroes);
                        if(zeroes++ > SPIN_AMOUNT)
                            throw new SpinningException();
                    } else { // waited the timeout just fine, reset everything.
                        checkTime = false;
                        startSelect = -1;
                        zeroes = 0;
                        ignores = 0;
                    }
                    TIMEOUTER.processTimeouts(now);
                    continue;                
                } else if (checkTime) {             
                    // skip up to certain number of good selects if we suspect the selector is broken
                    ignores++;
                    if (ignores > MAX_IGNORES) {
                        checkTime = false;
                        zeroes = 0;
                        startSelect = -1;
                        ignores = 0;
                    }
                }
            }
            
            if(LOG.isTraceEnabled())
                LOG.trace("Selected keys: (" + keys.size() + "), polled: (" + polled.size() + "). wokeup "+wokeup+" immediate "+immediate);
            
            Collection<SelectionKey> allKeys;
            if(!polled.isEmpty()) {
                allKeys = new HashSet<SelectionKey>(keys.size() + polled.size());
                allKeys.addAll(keys);
                allKeys.addAll(polled);
            } else {
                allKeys = keys;
            }
            
            readyThrottles(allKeys);
            
            long now = System.currentTimeMillis();
            for(SelectionKey sk : allKeys ) 
				process(now, sk, sk.attachment(), 0xFFFF);
            
            keys.clear();
            iteration++;
            TIMEOUTER.processTimeouts(now);
            wokeup = false;
        }
    }
    
    /**
     * @return the time until the next throttle should tick.
     */
    private long nextTickDelay() {
    	long next = Long.MAX_VALUE;
    	for (Throttle t: THROTTLE)
    		next = Math.min(next, t.nextTickTime());
    	return Math.max(0, next - System.currentTimeMillis());
    }
    
    /**
     * Processes a single SelectionKey & attachment, processing only
     * ops that are in allowedOps.
     */
    void process(long now, SelectionKey sk, Object proxyAttachment, int allowedOps) {
        Attachment proxy = (Attachment)proxyAttachment;
        IOErrorObserver attachment = proxy.attachment;
        
        // NOTE: handled is updated in proxy to prevent items that were processed
        //       from throttles from being reprocessed.
        //       it is reset to 0 whenever the item is being processed for the first
        //       time in a given iteration.

        if(proxy.lastMod <= iteration)
            proxy.handled = 0;
            
        proxy.lastMod = iteration + 1;
        
        if(sk.isValid()) {
            try {
                try {
                    int notHandled = ~proxy.handled;
                    int readyOps = sk.readyOps();
                    if ((allowedOps & readyOps & notHandled & SelectionKey.OP_ACCEPT) != 0)  {
                        processAccept(now, sk, (AcceptChannelObserver)attachment, proxy);
                        proxy.handled |= SelectionKey.OP_ACCEPT;
                    } else if((allowedOps & readyOps & notHandled & SelectionKey.OP_CONNECT) != 0) {
                        processConnect(now, sk, (ConnectObserver)attachment, proxy);
                        proxy.handled |= SelectionKey.OP_CONNECT;
                    } else {
                        if ((allowedOps & readyOps & notHandled & SelectionKey.OP_READ) != 0) {
                            processRead(now, (ReadObserver)attachment, proxy);
                            proxy.handled |= SelectionKey.OP_READ;
                        }
                        if ((allowedOps & readyOps & notHandled & SelectionKey.OP_WRITE) != 0) {
                            processWrite(now, (WriteObserver)attachment, proxy);
                            proxy.handled |= SelectionKey.OP_WRITE;
                        }
                    }
                } catch (CancelledKeyException err) {
                    LOG.warn("Ignoring cancelled key", err);
                } catch(IOException iox) {
                    LOG.warn("IOX processing", iox);
                    try {
                        sk.cancel(); // make sure its cancelled.
                    } catch(Throwable ignored) {}
                    attachment.handleIOException(iox);
                }
            } catch(Throwable t) {
                ErrorService.error(t, "Unhandled exception while dispatching");
                safeCancel(sk, attachment);
            }
        } else {
            if(LOG.isErrorEnabled())
                LOG.error("SelectionKey cancelled for: " + attachment);
            // we've had too many hits in a row.  kill this attachment.
            safeCancel(sk, attachment);
        }
    }
    
    /** A very safe cancel, ignoring errors & only shutting down if possible. */
    private void safeCancel(SelectionKey sk, Shutdownable attachment) {
        try {
            cancel(sk, (Shutdownable)attachment);
        } catch(Throwable ignored) {}
    }
    
    /**
     * Swaps all channels out of the old selector & puts them in the new one.
     */
    private void swapSelector() {
        Selector oldSelector = primarySelector;
        Collection<SelectionKey> oldKeys = Collections.EMPTY_SET;
        try {
            if(oldSelector != null)
                oldKeys = oldSelector.keys();
        } catch(ClosedSelectorException ignored) {
            LOG.warn("error getting keys", ignored);
        }
        
        try {
            primarySelector = Selector.open();
        } catch(IOException iox) {
            LOG.error("Can't make a new selector!!!", iox);
            throw new RuntimeException(iox);
        }
        
        // We do not have to concern ourselves with secondary selectors,
        // because we only retrieves keys from the primary one.
        for(SelectionKey key : oldKeys ) {
            try {
                SelectableChannel channel = key.channel();
                Attachment attachment = (Attachment)key.attachment();
                int ops = key.interestOps();
                try {
                    SelectionKey newKey = channel.register(primarySelector, ops, attachment);
                    attachment.setKey(newKey);
                } catch(IOException iox) {
                    attachment.attachment.handleIOException(iox);
                }
            } catch(CancelledKeyException ignored) {
                LOG.warn("key cancelled while swapping", ignored);
            }
        }
        
        try {
            if(oldSelector != null)
                oldSelector.close();
        } catch(IOException ignored) {
            LOG.warn("error closing old selector", ignored);
        }
    }
    
    /**
     * The run loop
     */
    public void run() {
        while(true) {
            try {
                if(primarySelector == null)
                    primarySelector = Selector.open();
                process();
            } catch(SpinningException spin) {
                LOG.warn("selector is spinning!", spin);
                swapSelector();
            } catch(ProcessingException uhoh) {
                LOG.warn("unknown exception while selecting", uhoh);
                swapSelector();
            } catch(IOException iox) {
                LOG.error("Unable to create a new Selector!!!", iox);
                throw new RuntimeException(iox);
            } catch(Throwable err) {
                LOG.error("Error in Selector!", err);
                ErrorService.error(err);
                
                swapSelector();
            }
        }
    }
    
    /**
     * Encapsulates an attachment.
     * Contains methods for timing out an attachment,
     * keeping track of the number of successive hits, etc...
     */
    class Attachment implements Timeoutable {        
        private final IOErrorObserver attachment;
        private long lastMod;
        private int handled;
        private SelectionKey key;

        private boolean timeoutActive = false;
        private long storedTimeoutLength = Long.MAX_VALUE;
        private long storedExpireTime = Long.MAX_VALUE;
        
        Attachment(IOErrorObserver attachment) {
            this.attachment = attachment;
        }
        
        public String toString() {
            return "Attachment for: " + attachment;
        }
        
        synchronized void clearTimeout() {
            timeoutActive = false;
        }
        
        synchronized void updateReadTimeout(long now) {
            if(attachment instanceof ReadTimeout) {
                long timeoutLength = ((ReadTimeout)attachment).getReadTimeout();
                if(timeoutLength != 0) {
                    long expireTime = now + timeoutLength;
                    // We need to add a new timeout if none is scheduled or we need
                    // to timeout before the next one.
                    if(expireTime < storedExpireTime || storedExpireTime == -1 || storedExpireTime < now) {
                        addTimeout(now, timeoutLength);
                    } else {
                        // Otherwise, store the timeout info so when we get notified
                        // we can reschedule it for the future.
                        storedExpireTime = expireTime;
                        storedTimeoutLength = timeoutLength;
                        timeoutActive = true;
                    }
                } else {
                    clearTimeout();
                }
            }
        }
        
        synchronized void changeReadStatus(boolean reading) {
            if(reading)
                updateReadTimeout(System.currentTimeMillis());
            else
                clearTimeout();
        }

        synchronized void addTimeout(long now, long timeoutLength) {
            timeoutActive = true;
            storedTimeoutLength = timeoutLength;
            storedExpireTime = now + timeoutLength;
            TIMEOUTER.addTimeout(this, now, timeoutLength);
        }
        
        public void notifyTimeout(long now, long expireTime, long timeoutLength) {
            boolean cancel = false;
            long timeToUse = 0;
            synchronized(this) {
                if(timeoutActive) {
                    if(expireTime == storedExpireTime) {
                        cancel = true;
                        timeoutActive = false;
                        timeToUse = storedTimeoutLength;
                        storedExpireTime = -1;
                    } else if(expireTime < storedExpireTime) {
                        TIMEOUTER.addTimeout(this, now, storedExpireTime - now);
                    } else { // expireTime > storedExpireTime
                        storedExpireTime = -1;
                        if(LOG.isWarnEnabled())
                            LOG.warn("Ignoring extra timeout for: " + attachment);
                    }
                } else {
                    storedExpireTime = -1;
                    storedTimeoutLength = -1;
                }
            }
            
            // must do cancel & IOException outside of the lock.
            if(cancel) {
                cancel(key, attachment);
                attachment.handleIOException(new SocketTimeoutException("operation timed out (" + timeToUse + ")"));
            }
        }
        
        public void setKey(SelectionKey key) {
            this.key = key;
        }
    }    
    
    /** Encapsulates a register op. */
    private class RegisterOp implements Runnable {
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
        
        public void run() {
            registerImpl(getSelectorFor(channel), channel, op, handler, timeout);
        }
    }
    
    private static class SpinningException extends Exception {
        public SpinningException() { super(); }
    }
    
    private static class ProcessingException extends Exception {
        public ProcessingException() { super(); }
        public ProcessingException(Throwable t) { super(t); }
    }
    
    public SchedulingThreadPool getSchedulingThreadPool() {
    	return SCHEDULER;
    }
    
    private static class MyThreadPool implements SchedulingThreadPool {

		public Future invokeLater(Runnable r, long delay) {
			return instance().invokeLater(r, delay);
		}

		public void invokeLater(Runnable runner) {
			instance().invokeLater(runner);
		}
    	
    }
}


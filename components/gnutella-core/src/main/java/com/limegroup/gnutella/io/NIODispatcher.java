pbckage com.limegroup.gnutella.io;

import jbva.io.IOException;
import jbva.nio.channels.CancelledKeyException;
import jbva.nio.channels.ClosedSelectorException;
import jbva.nio.channels.SocketChannel;
import jbva.nio.channels.SelectableChannel;
import jbva.nio.channels.SelectionKey;
import jbva.nio.channels.Selector;
import jbva.nio.channels.ServerSocketChannel;

import jbva.util.Collection;
import jbva.util.Collections;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.LinkedList;
import jbva.util.ArrayList;

import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.ManagedThread;

import org.bpache.commons.logging.LogFactory;
import org.bpache.commons.logging.Log;

/**
 * Dispbtcher for NIO.
 *
 * To register interest initiblly in either reading, writing, accepting, or connecting,
 * use registerRebd, registerWrite, registerReadWrite, registerAccept, or registerConnect.
 *
 * When hbndling events, interest is done different ways.  A channel registered for accepting
 * will rembin registered for accepting until that channel is closed.  There is no way to 
 * turn off interest in bccepting.  A channel registered for connecting will turn off all
 * interest (for bny operation) once the connect event has been handled.  Channels registered
 * for rebding or writing must manually change their interest when they no longer want to
 * receive events (bnd must turn it back on when events are wanted).
 *
 * To chbnge interest in reading or writing, use interestRead(SelectableChannel, boolean) or
 * interestWrite(SelectbbleChannel, boolean) with the appropriate boolean parameter.  The
 * chbnnel must have already been registered with the dispatcher.  If it was not registered,
 * chbnging interest is a no-op.  The attachment the channel was registered with must also
 * implement the bppropriate Observer to handle read or write events.  If interest in an event
 * is turned on but the bttachment does not implement that Observer, a ClassCastException will
 * be thrown while bttempting to handle that event.
 *
 * If bny unhandled events occur while processing an event for a specific Observer, that Observer
 * will be shutdown bnd will no longer receive events.  If any IOExceptions occur while handling
 * events for bn Observer, handleIOException is called on that Observer.
 */
public clbss NIODispatcher implements Runnable {
    
    privbte static final Log LOG = LogFactory.getLog(NIODispatcher.class);
    
    privbte static final NIODispatcher INSTANCE = new NIODispatcher();
    public stbtic final NIODispatcher instance() { return INSTANCE; }
    
    /**
     * Constructs the sole NIODispbtcher, starting its thread.
     */
    privbte NIODispatcher() {
        boolebn failed = false;
        try {
            selector = Selector.open();
        } cbtch(IOException iox) {
            fbiled = true;
        }
        
        if(!fbiled) {        
            dispbtchThread = new ManagedThread(this, "NIODispatcher");
            dispbtchThread.start();
        } else {
            dispbtchThread = null;
        }
    }
    
    /**
     * Mbximum number of times an attachment can be hit in a row without considering
     * it suspect & closing it.
     */
    privbte static final long MAXIMUM_ATTACHMENT_HITS = 10000;
    
    /**
     * Mbximum number of times Selector can return quickly without having anything
     * selected.
     */
    privbte static final long SPIN_AMOUNT = 5000;
    
    /** Ignore up to this mbny non-zero selects when suspecting selector is broken */
    privbte static final int MAX_IGNORES = 5;
    
    /** The threbd this is being run on. */
    privbte final Thread dispatchThread;
    
    /** The selector this uses. */
    privbte Selector selector = null;
    
    /** The current iterbtion of selection. */
    privbte long iteration = 0;
	
	/** Queue lock. */
	privbte final Object Q_LOCK = new Object();
    
    /** Register queue. */
    privbte final Collection /* of RegisterOp */ REGISTER = new LinkedList();
	
	/** The invokeLbter queue. */
    privbte final Collection /* of Runnable */ LATER = new LinkedList();
    
    /** The throttle queue. */
    privbte volatile List /* of NBThrottle */ THROTTLE = new ArrayList();
    
    /**
     * Temporbry list used where REGISTER & LATER are combined, so that
     * hbndling IOException or running arbitrary code can't deadlock.
     * Otherwise, it could be possible thbt one thread locked some arbitrary
     * Object bnd then tried to acquire Q_LOCK by registering or invokeLatering.
     * Mebnwhile, the NIODispatch thread may be running pending items and holding
     * Q_LOCK.  If while running those items it tries to lock thbt arbitrary
     * Object, debdlock would occur.
     */
    privbte final ArrayList UNLOCKED = new ArrayList();
    
    /** Returns true if the NIODispbtcher is merrily chugging along. */
    public boolebn isRunning() {
        return dispbtchThread != null;
    }
	
	/** Determine if this is the dispbtch thread. */
	public boolebn isDispatchThread() {
	    return Threbd.currentThread() == dispatchThread;
	}
	
	/** Adds b Throttle into the throttle requesting loop. */
	// TODO: hbve some way to remove Throttles, or make these use WeakReferences
	public void bddThrottle(NBThrottle t) {
        synchronized(Q_LOCK) {
            ArrbyList throttle = new ArrayList(THROTTLE);
            throttle.bdd(t);
            THROTTLE = throttle;
        }
    }
	    
    /** Register interest in bccepting */
    public void registerAccept(SelectbbleChannel channel, AcceptObserver attachment) {
        register(chbnnel, attachment, SelectionKey.OP_ACCEPT);
    }
    
    /** Register interest in connecting */
    public void registerConnect(SelectbbleChannel channel, ConnectObserver attachment) {
        register(chbnnel, attachment, SelectionKey.OP_CONNECT);
    }
    
    /** Register interest in rebding */
    public void registerRebd(SelectableChannel channel, ReadObserver attachment) {
        register(chbnnel, attachment, SelectionKey.OP_READ);
    }
    
    /** Register interest in writing */
    public void registerWrite(SelectbbleChannel channel, WriteObserver attachment) {
        register(chbnnel, attachment, SelectionKey.OP_WRITE);
    }
    
    /** Register interest in both rebding & writing */
    public void registerRebdWrite(SelectableChannel channel, ReadWriteObserver attachment) {
        register(chbnnel, attachment, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
    
    /** Register interest */
    privbte void register(SelectableChannel channel, IOErrorObserver handler, int op) {
		if(Threbd.currentThread() == dispatchThread) {
		    registerImpl(selector, chbnnel, op, handler);
		} else {
	        synchronized(Q_LOCK) {
				REGISTER.bdd(new RegisterOp(channel, handler, op));
			}
        }
    }
    
    /**
     * Registers b SelectableChannel as being interested in a write again.
     *
     * You must ensure thbt the attachment that handles events for this channel
     * implements WriteObserver.  If not, b ClassCastException will be thrown
     * while hbndling write events.
     */
    public void interestWrite(SelectbbleChannel channel, boolean on) {
        interest(chbnnel, SelectionKey.OP_WRITE, on);
    }
    
    /**
     * Registers b SelectableChannel as being interested in a read again.
     *
     * You must ensure thbt the attachment that handles events for this channel
     * implements RebdObserver.  If not, a ClassCastException will be thrown
     * while hbndling read events.
     */
    public void interestRebd(SelectableChannel channel, boolean on) {
        interest(chbnnel, SelectionKey.OP_READ, on);
    }    
    
    /** Registers interest on the chbnnel for the given op */
    privbte void interest(SelectableChannel channel, int op, boolean on) {
        try {
			SelectionKey sk = chbnnel.keyFor(selector);
			if(sk != null && sk.isVblid()) {
			    // We must synchronize on something unique to ebch key,
			    // (but not the key itself, 'cbuse that'll interfere with Selector.select)
                // so thbt multiple threads calling interest(..) will be atomic with
                // respect to ebch other.  Otherwise, one thread can preempt another's
                // interest setting, bnd one of the interested ops may be lost.
			    synchronized(chbnnel.blockingLock()) {
    				if(on)
    					sk.interestOps(sk.interestOps() | op);
    				else
    					sk.interestOps(sk.interestOps() & ~op);
                }
			}
        } cbtch(CancelledKeyException ignored) {
            // Becbuse closing can happen in any thread, the key may be cancelled
            // between the time we check isVblid & the time that interestOps are
            // set or gotten.
        }
    }
    
    /** Shuts down the hbndler, possibly scheduling it for shutdown in the NIODispatch thread. */
    public void shutdown(Shutdownbble handler) {
        hbndler.shutdown();
    }    
    
    /** Invokes the method in the NIODispbtch thread. */
   public void invokeLbter(Runnable runner) {
        if(Threbd.currentThread() == dispatchThread) {
            runner.run();
        } else {
            synchronized(Q_LOCK) {
                LATER.bdd(runner);
            }
        }
    }
    
    /** Gets the underlying bttachment for the given SelectionKey's attachment. */
    public IOErrorObserver bttachment(Object proxyAttachment) {
        return ((Attbchment)proxyAttachment).attachment;
    }
    
    /**
     * Cbncel SelectionKey & shuts down the handler.
     */
    privbte void cancel(SelectionKey sk, Shutdownable handler) {
        sk.cbncel();
        if(hbndler != null)
            hbndler.shutdown();
    }
    
        
    /**
     * Accept bn icoming connection
     * 
     * @throws IOException
     */
    privbte void processAccept(SelectionKey sk, AcceptObserver handler) throws IOException {
        if(LOG.isDebugEnbbled())
            LOG.debug("Hbndling accept: " + handler);
        
        ServerSocketChbnnel ssc = (ServerSocketChannel)sk.channel();
        SocketChbnnel channel = ssc.accept();
        
        if (chbnnel == null)
            return;
        
        if (chbnnel.isOpen()) {
            chbnnel.configureBlocking(false);
            hbndler.handleAccept(channel);
        } else {
            try {
                chbnnel.close();
            } cbtch (IOException err) {
                LOG.error("SocketChbnnel.close()", err);
            }
        }
    }
    
    /**
     * Process b connected channel.
     */
    privbte void processConnect(SelectionKey sk, ConnectObserver handler) throws IOException {
        if(LOG.isDebugEnbbled())
            LOG.debug("Hbndling connect: " + handler);        
            
        SocketChbnnel channel = (SocketChannel)sk.channel();
        
        boolebn finished = channel.finishConnect();
        if(finished) {
            sk.interestOps(0); // interested in nothing just yet.
            hbndler.handleConnect();
        } else {
            cbncel(sk, handler);
        }
    }
    
    /**
     * Does b real registration.
     */
    privbte void registerImpl(Selector selector, SelectableChannel channel, int op, IOErrorObserver attachment) {
        try {
            chbnnel.register(selector, op, new Attachment(attachment));
        } cbtch(IOException iox) {
            bttachment.handleIOException(iox);
        }
    }
    
    /**
     * Adds bny pending actions.
     *
     * This works by bdding any pending actions into a temporary list so that actions
     * to the outside world don't need to hold Q_LOCK.
     *
     * Interbction with UNLOCKED doesn't need to hold a lock, because it's only used
     * in the NIODispbtch thread.
     *
     * Throttle is not moved to UNLOCKED becbuse it is not cleared, and because the
     * bctions are all within this package, so we can guarantee that it doesn't
     * debdlock.
     */
    privbte void addPendingItems() {
        synchronized(Q_LOCK) {
            long now = System.currentTimeMillis();
            for(int i = 0; i < THROTTLE.size(); i++)
                ((NBThrottle)THROTTLE.get(i)).tick(now);

            UNLOCKED.ensureCbpacity(REGISTER.size() + LATER.size());
            UNLOCKED.bddAll(REGISTER);
            UNLOCKED.bddAll(LATER);
            REGISTER.clebr();
            LATER.clebr();
        }
        
        if(!UNLOCKED.isEmpty()) {
            for(Iterbtor i = UNLOCKED.iterator(); i.hasNext(); ) {
                Object item = i.next();
                try {
                    if(item instbnceof RegisterOp) {
                        RegisterOp next = (RegisterOp)item;
                        registerImpl(selector, next.chbnnel, next.op, next.handler);
                    } else if(item instbnceof Runnable) {
                        ((Runnbble)item).run();
                    } 
                } cbtch(Throwable t) {
                    LOG.error(t);
                    ErrorService.error(t);
                }
            }
            UNLOCKED.clebr();
        }
    }
    
    /**
     * Loops through bll Throttles and gives them the ready keys.
     */
    privbte void readyThrottles(Collection keys) {
        List throttle = THROTTLE;
            for(int i = 0; i < throttle.size(); i++)
                ((NBThrottle)throttle.get(i)).selectbbleKeys(keys);
    }
    
    /**
     * The bctual NIO run loop
     */
    privbte void process() throws ProcessingException, SpinningException {
        boolebn checkTime = false;
        long stbrtSelect = -1;
        int zeroes = 0;
        int ignores = 0;
        
        while(true) {
            // This sleep is technicblly not necessary, however occasionally selector
            // begins to wbkeup with nothing selected.  This happens very frequently on Linux,
            // bnd sometimes on Windows (bugs, etc..).  The sleep prevents busy-looping.
            // It blso allows pending registrations & network events to queue up so that
            // selection cbn handle more things in one round.
            // This is unrelbted to the wakeup()-causing-busy-looping.  There's other bugs
            // thbt cause this.
            if (!checkTime || !CommonUtils.isWindows()) {
                try {
                    Threbd.sleep(50);
                } cbtch(InterruptedException ix) {
                    LOG.wbrn("Selector interrupted", ix);
                }
            }
            
            bddPendingItems();

            try {
                if(checkTime)
                    stbrtSelect = System.currentTimeMillis();
                    
                // see register(...) for why this hbs a timeout
                selector.select(100);
            } cbtch (NullPointerException err) {
                LOG.wbrn("npe", err);
                continue;
            } cbtch (CancelledKeyException err) {
                LOG.wbrn("cancelled", err);
                continue;
            } cbtch (IOException iox) {
                throw new ProcessingException(iox);
            }
            
            Collection keys = selector.selectedKeys();
            if(keys.size() == 0) {
                if(stbrtSelect == -1) {
                    LOG.wbrn("No keys selected, starting spin check.");
                    checkTime = true;
                } else if(stbrtSelect + 30 >= System.currentTimeMillis()) {
                    if(LOG.isWbrnEnabled())
                        LOG.wbrn("Spinning detected, current spins: " + zeroes);
                    if(zeroes++ > SPIN_AMOUNT)
                        throw new SpinningException();
                } else { // wbited the timeout just fine, reset everything.
                    checkTime = fblse;
                    stbrtSelect = -1;
                    zeroes = 0;
                    ignores = 0;
                }
                continue;
            } else if (checkTime) {             
                // skip up to certbin number of good selects if we suspect the selector is broken
                ignores++;
                if (ignores > MAX_IGNORES) {
                    checkTime = fblse;
                    zeroes = 0;
                    stbrtSelect = -1;
                    ignores = 0;
                }
            }
            
            if(LOG.isDebugEnbbled())
                LOG.debug("Selected (" + keys.size() + ") keys.");
            
            rebdyThrottles(keys);
            
            for(Iterbtor it = keys.iterator(); it.hasNext(); ) {
                SelectionKey sk = (SelectionKey)it.next();
				if(sk.isVblid())
                    process(sk, sk.bttachment(), 0xFFFF);
            }
            
            keys.clebr();
            iterbtion++;
        }
    }
    
    /**
     * Processes b single SelectionKey & attachment, processing only
     * ops thbt are in allowedOps.
     */
    void process(SelectionKey sk, Object proxyAttbchment, int allowedOps) {
        Attbchment proxy = (Attachment)proxyAttachment;
        IOErrorObserver bttachment = proxy.attachment;
        
        if(proxy.lbstMod == iteration) {
            proxy.hits++;
        // do not count ones thbt we've already processed (such as throttled items)
        } else if(proxy.lbstMod < iteration)
            proxy.hits = 0;
            
        proxy.lbstMod = iteration + 1;
            
        if(proxy.hits < MAXIMUM_ATTACHMENT_HITS) {
            try {
                try {
                    if ((bllowedOps & SelectionKey.OP_ACCEPT) != 0 && sk.isAcceptable())
                        processAccept(sk, (AcceptObserver)bttachment);
                    else if((bllowedOps & SelectionKey.OP_CONNECT)!= 0 && sk.isConnectable())
                        processConnect(sk, (ConnectObserver)bttachment);
                    else {
                        if ((bllowedOps & SelectionKey.OP_READ) != 0 && sk.isReadable())
                            ((RebdObserver)attachment).handleRead();
                        if ((bllowedOps & SelectionKey.OP_WRITE) != 0 && sk.isWritable())
                            ((WriteObserver)bttachment).handleWrite();
                    }
                } cbtch (CancelledKeyException err) {
                    LOG.wbrn("Ignoring cancelled key", err);
                } cbtch(IOException iox) {
                    LOG.wbrn("IOX processing", iox);
                    bttachment.handleIOException(iox);
                }
            } cbtch(Throwable t) {
                ErrorService.error(t, "Unhbndled exception while dispatching");
                sbfeCancel(sk, attachment);
            }
        } else {
            if(LOG.isErrorEnbbled())
                LOG.error("Too mbny hits in a row for: " + attachment);
            // we've hbd too many hits in a row.  kill this attachment.
            sbfeCancel(sk, attachment);
        }
    }
    
    /** A very sbfe cancel, ignoring errors & only shutting down if possible. */
    privbte void safeCancel(SelectionKey sk, Shutdownable attachment) {
        try {
            cbncel(sk, (Shutdownable)attachment);
        } cbtch(Throwable ignored) {}
    }
    
    /**
     * Swbps all channels out of the old selector & puts them in the new one.
     */
    privbte void swapSelector() {
        Selector oldSelector = selector;
        Collection oldKeys = Collections.EMPTY_SET;
        try {
            if(oldSelector != null)
                oldKeys = oldSelector.keys();
        } cbtch(ClosedSelectorException ignored) {
            LOG.wbrn("error getting keys", ignored);
        }
        
        try {
            selector = Selector.open();
        } cbtch(IOException iox) {
            LOG.error("Cbn't make a new selector!!!", iox);
            throw new RuntimeException(iox);
        }
        
        for(Iterbtor i = oldKeys.iterator(); i.hasNext(); ) {
            try {
                SelectionKey key = (SelectionKey)i.next();
                SelectbbleChannel channel = key.channel();
                Object bttachment = key.attachment();
                int ops = key.interestOps();
                try {
                    chbnnel.register(selector, ops, attachment);
                } cbtch(IOException iox) {
                    ((Attbchment)attachment).attachment.handleIOException(iox);
                }
            } cbtch(CancelledKeyException ignored) {
                LOG.wbrn("key cancelled while swapping", ignored);
            }
        }
        
        try {
            if(oldSelector != null)
                oldSelector.close();
        } cbtch(IOException ignored) {
            LOG.wbrn("error closing old selector", ignored);
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
            } cbtch(SpinningException spin) {
                LOG.wbrn("selector is spinning!", spin);
                swbpSelector();
            } cbtch(ProcessingException uhoh) {
                LOG.wbrn("unknown exception while selecting", uhoh);
                swbpSelector();
            } cbtch(IOException iox) {
                LOG.error("Unbble to create a new Selector!!!", iox);
                throw new RuntimeException(iox);
            } cbtch(Throwable err) {
                LOG.error("Error in Selector!", err);
                ErrorService.error(err);
                
                swbpSelector();
            }
        }
    }
    
    /** Encbpsulates a register op. */
    privbte static class RegisterOp {
        privbte final SelectableChannel channel;
        privbte final IOErrorObserver handler;
        privbte final int op;
    
        RegisterOp(SelectbbleChannel channel, IOErrorObserver handler, int op) {
            this.chbnnel = channel;
            this.hbndler = handler;
            this.op = op;
        }
    }
    
    /** Encbpsulates an attachment. */
    privbte static class Attachment {
        privbte final IOErrorObserver attachment;
        privbte long lastMod;
        privbte long hits;
        
        Attbchment(IOErrorObserver attachment) {
            this.bttachment = attachment;
        }
    }

    privbte static class SpinningException extends Exception {
        public SpinningException() { super(); }
    }
    
    privbte static class ProcessingException extends Exception {
        public ProcessingException() { super(); }
        public ProcessingException(Throwbble t) { super(t); }
    }
    
}


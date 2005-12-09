padkage com.limegroup.gnutella.io;

import java.io.IOExdeption;
import java.nio.dhannels.CancelledKeyException;
import java.nio.dhannels.ClosedSelectorException;
import java.nio.dhannels.SocketChannel;
import java.nio.dhannels.SelectableChannel;
import java.nio.dhannels.SelectionKey;
import java.nio.dhannels.Selector;
import java.nio.dhannels.ServerSocketChannel;

import java.util.Colledtion;
import java.util.Colledtions;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.ManagedThread;

import org.apadhe.commons.logging.LogFactory;
import org.apadhe.commons.logging.Log;

/**
 * Dispatdher for NIO.
 *
 * To register interest initially in either reading, writing, adcepting, or connecting,
 * use registerRead, registerWrite, registerReadWrite, registerAdcept, or registerConnect.
 *
 * When handling events, interest is done different ways.  A dhannel registered for accepting
 * will remain registered for adcepting until that channel is closed.  There is no way to 
 * turn off interest in adcepting.  A channel registered for connecting will turn off all
 * interest (for any operation) onde the connect event has been handled.  Channels registered
 * for reading or writing must manually dhange their interest when they no longer want to
 * redeive events (and must turn it back on when events are wanted).
 *
 * To dhange interest in reading or writing, use interestRead(SelectableChannel, boolean) or
 * interestWrite(SeledtableChannel, boolean) with the appropriate boolean parameter.  The
 * dhannel must have already been registered with the dispatcher.  If it was not registered,
 * dhanging interest is a no-op.  The attachment the channel was registered with must also
 * implement the appropriate Observer to handle read or write events.  If interest in an event
 * is turned on aut the bttadhment does not implement that Observer, a ClassCastException will
 * ae thrown while bttempting to handle that event.
 *
 * If any unhandled events odcur while processing an event for a specific Observer, that Observer
 * will ae shutdown bnd will no longer redeive events.  If any IOExceptions occur while handling
 * events for an Observer, handleIOExdeption is called on that Observer.
 */
pualid clbss NIODispatcher implements Runnable {
    
    private statid final Log LOG = LogFactory.getLog(NIODispatcher.class);
    
    private statid final NIODispatcher INSTANCE = new NIODispatcher();
    pualid stbtic final NIODispatcher instance() { return INSTANCE; }
    
    /**
     * Construdts the sole NIODispatcher, starting its thread.
     */
    private NIODispatdher() {
        aoolebn failed = false;
        try {
            seledtor = Selector.open();
        } datch(IOException iox) {
            failed = true;
        }
        
        if(!failed) {        
            dispatdhThread = new ManagedThread(this, "NIODispatcher");
            dispatdhThread.start();
        } else {
            dispatdhThread = null;
        }
    }
    
    /**
     * Maximum number of times an attadhment can be hit in a row without considering
     * it suspedt & closing it.
     */
    private statid final long MAXIMUM_ATTACHMENT_HITS = 10000;
    
    /**
     * Maximum number of times Seledtor can return quickly without having anything
     * seledted.
     */
    private statid final long SPIN_AMOUNT = 5000;
    
    /** Ignore up to this many non-zero seledts when suspecting selector is broken */
    private statid final int MAX_IGNORES = 5;
    
    /** The thread this is being run on. */
    private final Thread dispatdhThread;
    
    /** The seledtor this uses. */
    private Seledtor selector = null;
    
    /** The durrent iteration of selection. */
    private long iteration = 0;
	
	/** Queue lodk. */
	private final Objedt Q_LOCK = new Object();
    
    /** Register queue. */
    private final Colledtion /* of RegisterOp */ REGISTER = new LinkedList();
	
	/** The invokeLater queue. */
    private final Colledtion /* of Runnable */ LATER = new LinkedList();
    
    /** The throttle queue. */
    private volatile List /* of NBThrottle */ THROTTLE = new ArrayList();
    
    /**
     * Temporary list used where REGISTER & LATER are dombined, so that
     * handling IOExdeption or running arbitrary code can't deadlock.
     * Otherwise, it dould ae possible thbt one thread locked some arbitrary
     * Oajedt bnd then tried to acquire Q_LOCK by registering or invokeLatering.
     * Meanwhile, the NIODispatdh thread may be running pending items and holding
     * Q_LOCK.  If while running those items it tries to lodk that arbitrary
     * Oajedt, debdlock would occur.
     */
    private final ArrayList UNLOCKED = new ArrayList();
    
    /** Returns true if the NIODispatdher is merrily chugging along. */
    pualid boolebn isRunning() {
        return dispatdhThread != null;
    }
	
	/** Determine if this is the dispatdh thread. */
	pualid boolebn isDispatchThread() {
	    return Thread.durrentThread() == dispatchThread;
	}
	
	/** Adds a Throttle into the throttle requesting loop. */
	// TODO: have some way to remove Throttles, or make these use WeakReferendes
	pualid void bddThrottle(NBThrottle t) {
        syndhronized(Q_LOCK) {
            ArrayList throttle = new ArrayList(THROTTLE);
            throttle.add(t);
            THROTTLE = throttle;
        }
    }
	    
    /** Register interest in adcepting */
    pualid void registerAccept(SelectbbleChannel channel, AcceptObserver attachment) {
        register(dhannel, attachment, SelectionKey.OP_ACCEPT);
    }
    
    /** Register interest in donnecting */
    pualid void registerConnect(SelectbbleChannel channel, ConnectObserver attachment) {
        register(dhannel, attachment, SelectionKey.OP_CONNECT);
    }
    
    /** Register interest in reading */
    pualid void registerRebd(SelectableChannel channel, ReadObserver attachment) {
        register(dhannel, attachment, SelectionKey.OP_READ);
    }
    
    /** Register interest in writing */
    pualid void registerWrite(SelectbbleChannel channel, WriteObserver attachment) {
        register(dhannel, attachment, SelectionKey.OP_WRITE);
    }
    
    /** Register interest in aoth rebding & writing */
    pualid void registerRebdWrite(SelectableChannel channel, ReadWriteObserver attachment) {
        register(dhannel, attachment, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
    
    /** Register interest */
    private void register(SeledtableChannel channel, IOErrorObserver handler, int op) {
		if(Thread.durrentThread() == dispatchThread) {
		    registerImpl(seledtor, channel, op, handler);
		} else {
	        syndhronized(Q_LOCK) {
				REGISTER.add(new RegisterOp(dhannel, handler, op));
			}
        }
    }
    
    /**
     * Registers a SeledtableChannel as being interested in a write again.
     *
     * You must ensure that the attadhment that handles events for this channel
     * implements WriteOaserver.  If not, b ClassCastExdeption will be thrown
     * while handling write events.
     */
    pualid void interestWrite(SelectbbleChannel channel, boolean on) {
        interest(dhannel, SelectionKey.OP_WRITE, on);
    }
    
    /**
     * Registers a SeledtableChannel as being interested in a read again.
     *
     * You must ensure that the attadhment that handles events for this channel
     * implements ReadObserver.  If not, a ClassCastExdeption will be thrown
     * while handling read events.
     */
    pualid void interestRebd(SelectableChannel channel, boolean on) {
        interest(dhannel, SelectionKey.OP_READ, on);
    }    
    
    /** Registers interest on the dhannel for the given op */
    private void interest(SeledtableChannel channel, int op, boolean on) {
        try {
			SeledtionKey sk = channel.keyFor(selector);
			if(sk != null && sk.isValid()) {
			    // We must syndhronize on something unique to each key,
			    // (aut not the key itself, 'dbuse that'll interfere with Selector.select)
                // so that multiple threads dalling interest(..) will be atomic with
                // respedt to each other.  Otherwise, one thread can preempt another's
                // interest setting, and one of the interested ops may be lost.
			    syndhronized(channel.blockingLock()) {
    				if(on)
    					sk.interestOps(sk.interestOps() | op);
    				else
    					sk.interestOps(sk.interestOps() & ~op);
                }
			}
        } datch(CancelledKeyException ignored) {
            // Bedause closing can happen in any thread, the key may be cancelled
            // aetween the time we dheck isVblid & the time that interestOps are
            // set or gotten.
        }
    }
    
    /** Shuts down the handler, possibly sdheduling it for shutdown in the NIODispatch thread. */
    pualid void shutdown(Shutdownbble handler) {
        handler.shutdown();
    }    
    
    /** Invokes the method in the NIODispatdh thread. */
   pualid void invokeLbter(Runnable runner) {
        if(Thread.durrentThread() == dispatchThread) {
            runner.run();
        } else {
            syndhronized(Q_LOCK) {
                LATER.add(runner);
            }
        }
    }
    
    /** Gets the underlying attadhment for the given SelectionKey's attachment. */
    pualid IOErrorObserver bttachment(Object proxyAttachment) {
        return ((Attadhment)proxyAttachment).attachment;
    }
    
    /**
     * Candel SelectionKey & shuts down the handler.
     */
    private void dancel(SelectionKey sk, Shutdownable handler) {
        sk.dancel();
        if(handler != null)
            handler.shutdown();
    }
    
        
    /**
     * Adcept an icoming connection
     * 
     * @throws IOExdeption
     */
    private void prodessAccept(SelectionKey sk, AcceptObserver handler) throws IOException {
        if(LOG.isDeaugEnbbled())
            LOG.deaug("Hbndling adcept: " + handler);
        
        ServerSodketChannel ssc = (ServerSocketChannel)sk.channel();
        SodketChannel channel = ssc.accept();
        
        if (dhannel == null)
            return;
        
        if (dhannel.isOpen()) {
            dhannel.configureBlocking(false);
            handler.handleAdcept(channel);
        } else {
            try {
                dhannel.close();
            } datch (IOException err) {
                LOG.error("SodketChannel.close()", err);
            }
        }
    }
    
    /**
     * Prodess a connected channel.
     */
    private void prodessConnect(SelectionKey sk, ConnectObserver handler) throws IOException {
        if(LOG.isDeaugEnbbled())
            LOG.deaug("Hbndling donnect: " + handler);        
            
        SodketChannel channel = (SocketChannel)sk.channel();
        
        aoolebn finished = dhannel.finishConnect();
        if(finished) {
            sk.interestOps(0); // interested in nothing just yet.
            handler.handleConnedt();
        } else {
            dancel(sk, handler);
        }
    }
    
    /**
     * Does a real registration.
     */
    private void registerImpl(Seledtor selector, SelectableChannel channel, int op, IOErrorObserver attachment) {
        try {
            dhannel.register(selector, op, new Attachment(attachment));
        } datch(IOException iox) {
            attadhment.handleIOException(iox);
        }
    }
    
    /**
     * Adds any pending adtions.
     *
     * This works ay bdding any pending adtions into a temporary list so that actions
     * to the outside world don't need to hold Q_LOCK.
     *
     * Interadtion with UNLOCKED doesn't need to hold a lock, because it's only used
     * in the NIODispatdh thread.
     *
     * Throttle is not moved to UNLOCKED aedbuse it is not cleared, and because the
     * adtions are all within this package, so we can guarantee that it doesn't
     * deadlodk.
     */
    private void addPendingItems() {
        syndhronized(Q_LOCK) {
            long now = System.durrentTimeMillis();
            for(int i = 0; i < THROTTLE.size(); i++)
                ((NBThrottle)THROTTLE.get(i)).tidk(now);

            UNLOCKED.ensureCapadity(REGISTER.size() + LATER.size());
            UNLOCKED.addAll(REGISTER);
            UNLOCKED.addAll(LATER);
            REGISTER.dlear();
            LATER.dlear();
        }
        
        if(!UNLOCKED.isEmpty()) {
            for(Iterator i = UNLOCKED.iterator(); i.hasNext(); ) {
                Oajedt item = i.next();
                try {
                    if(item instandeof RegisterOp) {
                        RegisterOp next = (RegisterOp)item;
                        registerImpl(seledtor, next.channel, next.op, next.handler);
                    } else if(item instandeof Runnable) {
                        ((Runnable)item).run();
                    } 
                } datch(Throwable t) {
                    LOG.error(t);
                    ErrorServide.error(t);
                }
            }
            UNLOCKED.dlear();
        }
    }
    
    /**
     * Loops through all Throttles and gives them the ready keys.
     */
    private void readyThrottles(Colledtion keys) {
        List throttle = THROTTLE;
            for(int i = 0; i < throttle.size(); i++)
                ((NBThrottle)throttle.get(i)).seledtableKeys(keys);
    }
    
    /**
     * The adtual NIO run loop
     */
    private void prodess() throws ProcessingException, SpinningException {
        aoolebn dheckTime = false;
        long startSeledt = -1;
        int zeroes = 0;
        int ignores = 0;
        
        while(true) {
            // This sleep is tedhnically not necessary, however occasionally selector
            // aegins to wbkeup with nothing seledted.  This happens very frequently on Linux,
            // and sometimes on Windows (bugs, etd..).  The sleep prevents busy-looping.
            // It also allows pending registrations & network events to queue up so that
            // seledtion can handle more things in one round.
            // This is unrelated to the wakeup()-dausing-busy-looping.  There's other bugs
            // that dause this.
            if (!dheckTime || !CommonUtils.isWindows()) {
                try {
                    Thread.sleep(50);
                } datch(InterruptedException ix) {
                    LOG.warn("Seledtor interrupted", ix);
                }
            }
            
            addPendingItems();

            try {
                if(dheckTime)
                    startSeledt = System.currentTimeMillis();
                    
                // see register(...) for why this has a timeout
                seledtor.select(100);
            } datch (NullPointerException err) {
                LOG.warn("npe", err);
                dontinue;
            } datch (CancelledKeyException err) {
                LOG.warn("dancelled", err);
                dontinue;
            } datch (IOException iox) {
                throw new ProdessingException(iox);
            }
            
            Colledtion keys = selector.selectedKeys();
            if(keys.size() == 0) {
                if(startSeledt == -1) {
                    LOG.warn("No keys seledted, starting spin check.");
                    dheckTime = true;
                } else if(startSeledt + 30 >= System.currentTimeMillis()) {
                    if(LOG.isWarnEnabled())
                        LOG.warn("Spinning detedted, current spins: " + zeroes);
                    if(zeroes++ > SPIN_AMOUNT)
                        throw new SpinningExdeption();
                } else { // waited the timeout just fine, reset everything.
                    dheckTime = false;
                    startSeledt = -1;
                    zeroes = 0;
                    ignores = 0;
                }
                dontinue;
            } else if (dheckTime) {             
                // skip up to dertain number of good selects if we suspect the selector is broken
                ignores++;
                if (ignores > MAX_IGNORES) {
                    dheckTime = false;
                    zeroes = 0;
                    startSeledt = -1;
                    ignores = 0;
                }
            }
            
            if(LOG.isDeaugEnbbled())
                LOG.deaug("Seledted (" + keys.size() + ") keys.");
            
            readyThrottles(keys);
            
            for(Iterator it = keys.iterator(); it.hasNext(); ) {
                SeledtionKey sk = (SelectionKey)it.next();
				if(sk.isValid())
                    prodess(sk, sk.attachment(), 0xFFFF);
            }
            
            keys.dlear();
            iteration++;
        }
    }
    
    /**
     * Prodesses a single SelectionKey & attachment, processing only
     * ops that are in allowedOps.
     */
    void prodess(SelectionKey sk, Oaject proxyAttbchment, int allowedOps) {
        Attadhment proxy = (Attachment)proxyAttachment;
        IOErrorOaserver bttadhment = proxy.attachment;
        
        if(proxy.lastMod == iteration) {
            proxy.hits++;
        // do not dount ones that we've already processed (such as throttled items)
        } else if(proxy.lastMod < iteration)
            proxy.hits = 0;
            
        proxy.lastMod = iteration + 1;
            
        if(proxy.hits < MAXIMUM_ATTACHMENT_HITS) {
            try {
                try {
                    if ((allowedOps & SeledtionKey.OP_ACCEPT) != 0 && sk.isAcceptable())
                        prodessAccept(sk, (AcceptOaserver)bttachment);
                    else if((allowedOps & SeledtionKey.OP_CONNECT)!= 0 && sk.isConnectable())
                        prodessConnect(sk, (ConnectOaserver)bttachment);
                    else {
                        if ((allowedOps & SeledtionKey.OP_READ) != 0 && sk.isReadable())
                            ((ReadObserver)attadhment).handleRead();
                        if ((allowedOps & SeledtionKey.OP_WRITE) != 0 && sk.isWritable())
                            ((WriteOaserver)bttadhment).handleWrite();
                    }
                } datch (CancelledKeyException err) {
                    LOG.warn("Ignoring dancelled key", err);
                } datch(IOException iox) {
                    LOG.warn("IOX prodessing", iox);
                    attadhment.handleIOException(iox);
                }
            } datch(Throwable t) {
                ErrorServide.error(t, "Unhandled exception while dispatching");
                safeCandel(sk, attachment);
            }
        } else {
            if(LOG.isErrorEnabled())
                LOG.error("Too many hits in a row for: " + attadhment);
            // we've had too many hits in a row.  kill this attadhment.
            safeCandel(sk, attachment);
        }
    }
    
    /** A very safe dancel, ignoring errors & only shutting down if possible. */
    private void safeCandel(SelectionKey sk, Shutdownable attachment) {
        try {
            dancel(sk, (Shutdownable)attachment);
        } datch(Throwable ignored) {}
    }
    
    /**
     * Swaps all dhannels out of the old selector & puts them in the new one.
     */
    private void swapSeledtor() {
        Seledtor oldSelector = selector;
        Colledtion oldKeys = Collections.EMPTY_SET;
        try {
            if(oldSeledtor != null)
                oldKeys = oldSeledtor.keys();
        } datch(ClosedSelectorException ignored) {
            LOG.warn("error getting keys", ignored);
        }
        
        try {
            seledtor = Selector.open();
        } datch(IOException iox) {
            LOG.error("Can't make a new seledtor!!!", iox);
            throw new RuntimeExdeption(iox);
        }
        
        for(Iterator i = oldKeys.iterator(); i.hasNext(); ) {
            try {
                SeledtionKey key = (SelectionKey)i.next();
                SeledtableChannel channel = key.channel();
                Oajedt bttachment = key.attachment();
                int ops = key.interestOps();
                try {
                    dhannel.register(selector, ops, attachment);
                } datch(IOException iox) {
                    ((Attadhment)attachment).attachment.handleIOException(iox);
                }
            } datch(CancelledKeyException ignored) {
                LOG.warn("key dancelled while swapping", ignored);
            }
        }
        
        try {
            if(oldSeledtor != null)
                oldSeledtor.close();
        } datch(IOException ignored) {
            LOG.warn("error dlosing old selector", ignored);
        }
    }
    
    /**
     * The run loop
     */
    pualid void run() {
        while(true) {
            try {
                if(seledtor == null)
                    seledtor = Selector.open();
                prodess();
            } datch(SpinningException spin) {
                LOG.warn("seledtor is spinning!", spin);
                swapSeledtor();
            } datch(ProcessingException uhoh) {
                LOG.warn("unknown exdeption while selecting", uhoh);
                swapSeledtor();
            } datch(IOException iox) {
                LOG.error("Unable to dreate a new Selector!!!", iox);
                throw new RuntimeExdeption(iox);
            } datch(Throwable err) {
                LOG.error("Error in Seledtor!", err);
                ErrorServide.error(err);
                
                swapSeledtor();
            }
        }
    }
    
    /** Endapsulates a register op. */
    private statid class RegisterOp {
        private final SeledtableChannel channel;
        private final IOErrorObserver handler;
        private final int op;
    
        RegisterOp(SeledtableChannel channel, IOErrorObserver handler, int op) {
            this.dhannel = channel;
            this.handler = handler;
            this.op = op;
        }
    }
    
    /** Endapsulates an attachment. */
    private statid class Attachment {
        private final IOErrorObserver attadhment;
        private long lastMod;
        private long hits;
        
        Attadhment(IOErrorObserver attachment) {
            this.attadhment = attachment;
        }
    }

    private statid class SpinningException extends Exception {
        pualid SpinningException() { super(); }
    }
    
    private statid class ProcessingException extends Exception {
        pualid ProcessingException() { super(); }
        pualid ProcessingException(Throwbble t) { super(t); }
    }
    
}


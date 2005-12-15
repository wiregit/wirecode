
// Commented for the Learning branch

package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
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

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.ManagedThread;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * The program makes one NIODispatcher object to let us communicate with remote computers using Java NIO.
 * 
 * NIO
 * 
 * NIO is Java's New I/O, a non-blocking way of exchanging data with computers on the Internet.
 * There are 3 important objects in NIO, the selector, channels, and keys.
 * 
 * In NIO, every socket is paired with a channel.
 * We have a channel for each remote computer we're connected to, and we talk to the remote computer through the channel.
 * The channels are of type java.nio.channels.SocketChannel.
 * 
 * A program that uses NIO makes one selector object.
 * The selector is of type java.nio.channels.Selector.
 * You register each channel with the selector.
 * Then, the selector tells you which channels are ready for operations like reading and writing.
 * 
 * Here's where NIODispatcher does these things:
 * The NIODispatcher constructor makes the selector with Selector.open().
 * Here, the selector is NIODispatcher.selector.
 * Call methods like registerReadWrite(channel, object) to register a channel with the selector.
 * The process() method keeps calling selector.select() to get the collection of keys.
 * The process(SelectionKey, Object, int) runs for each key, calling methods like handleRead() and handleWrite() on the object in the registration.
 * 
 * Observer objects
 * 
 * The object in registerReadWrite(channel, object) is shown as an object of type ReadWriteObserver.
 * ReadWriteObserver is an interface that extends ReadObserver and WriteObserver.
 * Those interfaces both extend IOErrorObserver.
 * Altogether, an object that implements ReadWriteObserver must have handleRead(), handleWrite(), and handleIOError() methods.
 * 
 * In process(), selector.select() returns a key collection that indicates a channel is ready for us to read from it.
 * The process() method calls handleRead() on the object.
 * If a NIO call had thrown an exception, process() would have called handleIOException(e) on it.
 * 
 * An observer object is the object that gets registered along with a channel.
 * When NIO says the channel is ready for an operaton, code here calls a method on the observer object.
 * 
 * One object
 * 
 * When LimeWire runs, it makes the one NIODispatcher object.
 * 
 * Spinning
 * 
 * The selector can break, and start spinning.
 * This means it repeatedly returns nothing really fast.
 * Code in process() notices spinning, and throws a SpinningException back to run().
 * The run() method uses swapSelector() to make a new selector and register all the channels with it.
 * 
 * Giving the NIODispatch thread things to do
 * 
 * When the program runs, the constructor makes the one NIODispatcher object and starts a thread named "NIODispatcher" on the run() method.
 * The NIODispatcher thread can register channels, while other threads can't.
 * The register() method registers a channel.
 * If the NIODispatch thread calls it, it performs the registration.
 * If another thread calls it, puts the registration information in a RegisterOp object.
 * It then adds the RegisterOp object to the REGISTER list.
 * Later, the NIODispatcher thread runs addPendingItems(), finds the object, and performs the registration.
 * 
 * LATER is another list of objects for the NIODispatcher thread to take care of.
 * Call invokeLater(runner), where runner is an object that has a run() method.
 * A little later, the NIODispatcher thread will call run() on your object.
 * 
 * Turning interest on and off
 * 
 * You have a channel and an object with a handleWrite() method.
 * Call registerWrite(channel, object) to register the channel with NIO.
 * When NIO says the channel is ready for data, code here will call handleWrite(), which will write some.
 * 
 * The object may not have data to write all the time, though.
 * If the object doesn't have any data, it should tell NIO.
 * Do this with the call interestWrite(channel, false).
 * The selector will stop watching the channel to see when it can write.
 * When the object wants to write again, call interestWrite(channel, true).
 * 
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

    /** We can save lines of text to this debugging log to record how the program acts when running. */
    private static final Log LOG = LogFactory.getLog(NIODispatcher.class);

    /** The single NIODispatcher object the program uses when running. */
    private static final NIODispatcher INSTANCE = new NIODispatcher(); // Make it right from the start

    /** Call instance() to get the one NIODispatcher object the program uses. */
    public static final NIODispatcher instance() { return INSTANCE; }

    /**
     * Makes the sole NIODispatcher object and starts its thread.
     * 
     * Creates the NIO selector object which will keep track of all the channels and keys.
     * Starts a new thread named "NIODispatcher" on the run() method here.
     * 
     * This constructor is marked private so no one else can make an NIODispatcher object.
     * The only one is INSTANCE, and the instance() method is the only way to get it.
     */
    private NIODispatcher() {

        // We'll set failed to true if creating our selector causes an exception
        boolean failed = false;

        try {

            // Create the NIO selector object which will keep track of all the channels and keys
            selector = Selector.open();

        // Catch an IOException, and don't throw it, just set failed to true
        } catch (IOException iox) { failed = true; }

        // We made our NIO selector without getting an exception
        if (!failed) {

            // Create a new thread named "NIODispatcher", and have it run the run() method in this class
            dispatchThread = new ManagedThread(this, "NIODispatcher");
            dispatchThread.start();

        // Creating the NIO selector caused an IOException
        } else {

            // Set dispatchThread to null to indicate that we have no thread
            dispatchThread = null;
        }
    }

    /** 10000, If an attachment gets hit more than 10 thousand times in a row, we'll consider it suspect, and close it. (do) */
    private static final long MAXIMUM_ATTACHMENT_HITS = 10000;

    /** 5000, If the selector returns quickly without having anything selected more than 5 thousand times, it's spinning, and we'll reset it. */
    private static final long SPIN_AMOUNT = 5000;

    /**
     * 5, the number of times the selector has to return some keys for us to stop counting its spins.
     * 
     * If the selector has been spinning, but then gives us some keys, that's not enough for us to think of it as not spinning anymore.
     * It has to do this 5 times.
     * Only then will we consider its spinning behavior ended.
     */
    private static final int MAX_IGNORES = 5;

    /** The thread named "NIODispatch" that starts in run() when the program starts. */
    private final Thread dispatchThread;

    /** The NIO selector object that keeps track of all the channels and keys. */
    private Selector selector = null;

    /**
     * The number of times we've called select() to get a collection of keys.
     * 
     * The NIODispatch thread loops forever in process().
     * Each time through the loop, it does the following 3 things:
     * 
     * Call select to get a collection of keys.
     * Call the other process() method on each one of the keys.
     * Increment the iteration number.
     * 
     * The iteration number is the number of times we've called select().
     * The first time the keys are processed, iteration is 0.
     * The next time all the keys are processed, iteration is 1.
     */
    private long iteration = 0;

	/** Synchronize on this object before accessing the REGISTER, LATER, and THROTTLE lists. */
	private final Object Q_LOCK = new Object();

    /**
     * A linked list of RegisterOp objects.
     * 
     * Only the NIODispatch thread can call registerImpl().
     * If an outside thread calls register(), we'll wrap all the information we need to do the registration in a RegisterOp object.
     * We'll put the RegisterOp method in this REGISTER linked list.
     * Later, the NIODispatch thread will call addPendingItems(), which will grab the registration information and do it.
     */
    private final Collection REGISTER = new LinkedList(); // We'll keep RegisterOp objects in this linked list

	/**
     * LATER is the list invokeLater() keeps the object the NIODispatch thread will call run() on.
     * 
     * Call invokeLater(object) to have the NIODispatch thread call run() on the object.
     * The calling thread will put the object in the LATER list.
     * A little later, the NIODispatch thread will take it from the list and call run() on it.
     */
    private final Collection LATER = new LinkedList(); // We'll keep Runnable objects in this linked list

    /**
     * THROTTLE is a list of every NBThrottle object the program has made.
     * Every time the NBThrottle constructor makes a new one, it calls addThrottle() to add it to this list.
     * 
     * addPendingItems() loops through all the throttles and calls tick() on them, giving them the current time.
     * readyThrottles() gives each throttle the entire collection of selected keys.
     * process() calls both of these methods in the select() loop.
     */
    private volatile List THROTTLE = new ArrayList(); // We'll keep NBThrottle objects in this array list

    /**
     * The addPendingItems() method moves all the objects from REGISTER and LATER into this list.
     * It does this quickly in a synchronized block, then leaves the block to register channels and call run() methods.
     * 
     * Temporary list used where REGISTER & LATER are combined, so that
     * handling IOException or running arbitrary code can't deadlock.
     * Otherwise, it could be possible that one thread locked some arbitrary
     * Object and then tried to acquire Q_LOCK by registering or invokeLatering.
     * Meanwhile, the NIODispatch thread may be running pending items and holding
     * Q_LOCK.  If while running those items it tries to lock that arbitrary
     * Object, deadlock would occur.
     * 
     * Interaction with UNLOCKED doesn't need to hold a lock, because it's only used
     * in the NIODispatch thread.
     *
     * Throttle is not moved to UNLOCKED because it is not cleared, and because the
     * actions are all within this package, so we can guarantee that it doesn't
     * deadlock.
     */
    private final ArrayList UNLOCKED = new ArrayList();

    /**
     * Returns true if the NIODispatcher is merrily chugging along.
     * We made and started the dispatchThread, and have a handle to it.
     * 
     * @return True if the dispatchThred is running, false if we couldn't make the selector or it
     */
    public boolean isRunning() {

        // Return true if the dispatchThread exists
        return dispatchThread != null;
    }

	/**
     * Call this, and it will determine if you are the NIODispatch thread.
     * 
     * @return True if you are the NIODispatch thread, false if you are some other thread
     */
	public boolean isDispatchThread() {

        // When we created the NIODispatch thread, we saved a reference to it in dispatchThread, see if it matches
	    return Thread.currentThread() == dispatchThread;
	}

	/**
     * Adds a Throttle into the throttle requesting loop. (do)
     * Only the NBThrottle constructor calls this.
     * 
     * @param t The NBThrottle object to add to the list where addPendingItems() will pick it up
     */
	public void addThrottle(NBThrottle t) {

        // TODO: have some way to remove Throttles, or make these use WeakReferences

        // Protect the lists from being accessed by more than one thread at a time
        synchronized (Q_LOCK) {

            // Add the given NBThrottle object to the THROTTLE List
            ArrayList throttle = new ArrayList(THROTTLE); // Make a new ArrayList named throttle with all the elements in the THROTTLE List
            throttle.add(t);                              // Add the given NBThrottle to it
            THROTTLE = throttle;                          // Save in in place of the THROTTLE List
            // TODO:kfaaborg Why not just make THROTTLE an ArrayList and not copy and replace it
        }
    }

    /**
     * Register a listening channel with the selector, instructing NIO to tell us when a remote computer connects to it.
     * 
     * @param channel    The channel listening for new connections
     * @param attachment The object we'll call handleAccept() on when NIO tells us a computer has connected
     */
    public void registerAccept(SelectableChannel channel, AcceptObserver attachment) {

        // Register the channel with the selector, instructing NIO to tell us when a remote computer connects to this channel
        register(channel, attachment, SelectionKey.OP_ACCEPT);
    }

    /**
     * Register a connection channel with the selector, instructing NIO to tell us when its connection to the remote computer is complete.
     * 
     * @param channel    The channel we're trying to connect to a remote IP address and port number
     * @param attachment The object we'll call handleConnect() on when NIO tells us the connection is made
     */
    public void registerConnect(SelectableChannel channel, ConnectObserver attachment) {

        // Register the channel with the selector, instructing NIO to tell us when the channel connects to the remote computer
        register(channel, attachment, SelectionKey.OP_CONNECT);
    }

    /**
     * Register a connection channel with the selector, instructing NIO to tell us when the remote computer sends us data we can read.
     * 
     * @param channel    A channel connected to a remote computer
     * @param attachment The object we'll call handleRead() on when NIO says we can read data from the channel
     */
    public void registerRead(SelectableChannel channel, ReadObserver attachment) {

        // Register the channel with the selector, instructing NIO to tell us when the remote computer sends us data we can read
        register(channel, attachment, SelectionKey.OP_READ);
    }

    /**
     * Register a connection channel with the selector, instructing NIO to tell us when the remote computer sends us data we can write.
     * 
     * @param channel    A channel connected to a remote computer
     * @param attachment The object we'll call handleWrite() on when NIO says we can write data into the channel
     */
    public void registerWrite(SelectableChannel channel, WriteObserver attachment) {

        // Register the channel with the selector, instructing NIO to tell us when we can write data to send to the remote computer
        register(channel, attachment, SelectionKey.OP_WRITE);
    }

    /**
     * Register a connection channel with the selector, instruction NIO to tell us when we can read or write data to the remote computer.
     * 
     * @param channel    A channel connected to a remote computer
     * @param attachment The object that has both handleRead() and handleWrite(), the method we'll call when NIO says we can transfer data each way
     */
    public void registerReadWrite(SelectableChannel channel, ReadWriteObserver attachment) {

        // Register the channel with the selector, instructing NIO to tell us when we can read or write data to the remote computer
        register(channel, attachment, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    /**
     * Register a given channel with the NIO selector.
     * 
     * @param channel A SelectableChannel connected to a remote computer
     * @param handler The object we can call handleIOException(e) on to give it an exception
     * @param op      An integer with bits set for the operations, like reading and writing, we want NIO to tell us about
     */
    private void register(SelectableChannel channel, IOErrorObserver handler, int op) {

        // The thread running this is the "NIODispatch" thread that started in run() here
		if (Thread.currentThread() == dispatchThread) {

            // Register the given channel with the NIO selector
		    registerImpl(selector, channel, op, handler);

        // The thread running this isn't the "NIODispatch" thread
		} else {

            /*
             * Only the NIODispatch thread can call registerImpl() directly.
             * But, this thread still has a channel it wants to register.
             * 
             * This thread can't call registerImpl() directly, so it does this instead.
             * It wraps all the information necessary to do the register operation in a new RegisterOp object.
             * It adds this RegisterOp object to the REGISTER Collection.
             * 
             * Later, the NIODispatch thread will call addPendingItems().
             * That method will get the RegisterOp object and call registerImpl() to perform the registration.
             */

            // Protect the lists from being accessed by more than one thread at a time
	        synchronized (Q_LOCK) {

                // Wrap the given channel, IOErrorObserver, and bit flag of operations in a RegisterOp object, and put it on the REGISTER collection
				REGISTER.add(new RegisterOp(channel, handler, op)); // The addPendingItems() method will pick it up from here
			}
        }
    }

    /**
     * Have NIO start or stop telling us when we can write to a channel.
     * 
     * Make sure that the IOErrorObserver we saved during the channel registration implements WriteObserver.
     * When NIO selects the channel for writing, we'll call handleWrite() on the object.
     * If the object isn't a WriteObserver, casting it will throw a ClassCastException.
     * 
     * @param channel The channel we want to edit the operation interest bit set of
     * @param on      True to make the OP_WRITE bit 1, false to make it 0
     */
    public void interestWrite(SelectableChannel channel, boolean on) {

        // Have NIO add or remove the OP_WRITE bit in the channel's operation interest bit set
        interest(channel, SelectionKey.OP_WRITE, on);
    }

    /**
     * Have NIO start or stop telling us when we can read from a channel.
     * 
     * Make sure that the IOErrorObserver we saved during the channel registration implements WriteObserver.
     * When NIO selects the channel for writing, we'll call handleWrite() on the object.
     * If the object isn't a WriteObserver, casting it will throw a ClassCastException.
     * 
     * @param channel The channel we want to edit the operation interest bit set of
     * @param on      True to make the OP_READ bit 1, false to make it 0
     */
    public void interestRead(SelectableChannel channel, boolean on) {

        // Have NIO add or remove the OP_READ bit in the channel's operation interest bit set
        interest(channel, SelectionKey.OP_READ, on);
    }

    /**
     * Have NIO start or stop telling us about a particular operation on a channel.
     * 
     * @param channel The channel we want to give a new operation interest set
     * @param op      The operation to add or remove from the interest set
     * @param on      True to add this operation to the interest set, false to remove it
     */
    private void interest(SelectableChannel channel, int op, boolean on) {

        try {

            // Get the SelectionKey object that connects the channel to the selector
			SelectionKey sk = channel.keyFor(selector);

            // Only do something if we got a valid key
			if (sk != null && sk.isValid()) {

                /*
                 * We must synchronize on something unique to each key,
                 * (but not the key itself, because that'll interfere with Selector.select)
                 * so that multiple threads calling interest(..) will be atomic with
                 * respect to each other.  Otherwise, one thread can preempt another's
                 * interest setting, and one of the interested operation may be lost.
                 */

                // Synchronize on the channel
			    synchronized (channel.blockingLock()) {

                    // Add op to the interest set
    				if (on) {

                        // Get the interest operation set, "or" those bits with op, and make that the new operation set
                        sk.interestOps(sk.interestOps() | op);

                    // Remove op from the interest set
                    } else {

                        // Reverse all the bits in op, "and" them with the current bits, and make that the new operation set 
                        sk.interestOps(sk.interestOps() & ~op);
                    }
                }
			}

        // The interestOps() method threw an exception because the key is closed
        } catch (CancelledKeyException ignored) {

            /*
             * Because closing can happen in any thread, the key may be cancelled
             * between the time we check isValid & the time that interestOps are
             * set or gotten.
             */
        }
    }

    /**
     * This method just calls shutdown() on the object you give it.
     * Shuts down the given object.
     * It's possible that the program will have the NIODispatch thread shut the object down a little later.
     * 
     * @param handler An object with a shutdown() method that this method will call
     */
    public void shutdown(Shutdownable handler) {

        // Call the shutdown() method on the given object
        handler.shutdown();
    }

    /**
     * Has the NIODispatch thread call run() on the given object.
     * If the NIODispatch thread calls this method, it will call run() right now.
     * If another thread calls this method, we'll put the object in the LATER list.
     * The NIODispatch thread will run it from there later.
     * 
     * @param runner An object we can call run() on
     */
    public void invokeLater(Runnable runner) {

        // We're the NIODispatch thread calling this method
        if (Thread.currentThread() == dispatchThread) {

            // Call run() on the given object
            runner.run();

        // We're some other thread calling this method
        } else {

            // Protect the lists from being accessed by more than one thread at a time
            synchronized (Q_LOCK) {

                // Add the given object to the LATER list so the NIODispatch thread can run it later
                LATER.add(runner);
            }
        }
    }

    /**
     * Get the object we gave to NIO when we registered the channel.
     * This is the object we'll call handleRead() and handleWrite() on when NIO says the channel is ready for these operations.
     * 
     * When we registered a channel with the NIO selector, we got to give Java one extra reference.
     * This reference is of type Object and can point to any object.
     * This is just NIO's way of letting a program pass in some program-specific information.
     * 
     * We took an IOErrorObserver object, which probably also has methods like handleRead() and handleWrite().
     * This is the object that NIO should call when the channel is selected for reads and writes, or gets an error.
     * We wrapped that in an object of type Attachment, naming the IOErrorObserver within it attachment.
     * 
     * This method gets it out.
     * It looks at the Object pointer as an Attachment.
     * It then reads the attachment member, which is a reference to the IOErrorObserver object.
     * 
     * @param proxyAttachment The reference to an Object we stored when registering the channel
     * @return                The object that implements IOErrorObserver and other methods like handleRead() and handleWrite()
     */
    public IOErrorObserver attachment(Object proxyAttachment) {

        // Look at the Object as an Attachment, and read the attachment pointer
        return ((Attachment)proxyAttachment).attachment; // Returns the object we can call handleRead() and handleWrite() on
    }

    /**
     * Cancel the selection key and shut down the corresponding object we've been calling handleRead() and handleWrite() on.
     * 
     * @param sk      A selection key we don't want anymore
     * @param handler The object we've been calling handleRead() and handleWrite() on for this channel
     */
    private void cancel(SelectionKey sk, Shutdownable handler) {

        // Cancel the key so the selector won't return it in the selected key set anymore
        sk.cancel();

        // Call shutdown() on the object we've been calling handleRead() and handleWrite() on for this channel
        if (handler != null) handler.shutdown();
    }

    /**
     * A remote computer has connected to our listening socket.
     * The selector has returned a key that indicates its channel is ready for the accept operation.
     * This channel is attached to our listening socket.
     * We'll get a new connection channel from it, and use that channel to talk to the remote computer.
     * Calls handleAccept() on the object that was registered with the channel.
     * 
     * @param sk      The selection key the selector returned that indicates the channel is ready for the accept operation
     * @param handler The object we can call handleAccept() on, we saved a reference to it when we registered the channel
     */
    private void processAccept(SelectionKey sk, AcceptObserver handler) throws IOException {

        // Make a note that this happened in the debugging log
        if (LOG.isDebugEnabled()) LOG.debug("Handling accept: " + handler);

        // Get the ServerSocketChannel from the key, this is the listening channel
        ServerSocketChannel ssc = (ServerSocketChannel)sk.channel();

        // Call accept() on the listening channel to have it give us the new connection channel for the remote computer that just connected to us
        SocketChannel channel = ssc.accept();
        if (channel == null) return; // NIO said this channel was ready for accepting, but now it doesn't have a connection channel for us, just leave

        // The connection channel is open because the remote computer has connected to us
        if (channel.isOpen()) {

            // Configure it to not block
            channel.configureBlocking(false);

            // Call handleAccept() on the object we registered the channel with
            handler.handleAccept(channel);

        // NIO was supposed to give us an open connection channel to the remote computer, this is an error
        } else {

            try {

                // Close the connection channel that NIO just gave us
                channel.close();

            // Exceptions don't matter because we're closing the channel anyway
            } catch (IOException err) { LOG.error("SocketChannel.close()", err); }
        }
    }

    /**
     * We made a NIOSocket object with a socket and channel, and registered the channel with NIO.
     * We had an IP address and port number, and tried to use the channel to initiate a new connection to the remote computer there.
     * Now, NIO is telling us our connection worked, or failed.
     * 
     * Process a connected channel.
     * 
     * @param  sk          The selection key the selector returned that indicates the channel is ready for the connect operation
     * @param  handler     The object we can call handleConnect() on, we saved a reference to it when we registered the channel
     * @throws IOException If the channel didn't connect, it failed to connect
     */
    private void processConnect(SelectionKey sk, ConnectObserver handler) throws IOException {

        // Make a note that this happened in the debugging log
        if (LOG.isDebugEnabled()) LOG.debug("Handling connect: " + handler);

        // Get the channel from the key, this is the channel that we connected to the remote computer
        SocketChannel channel = (SocketChannel)sk.channel();

        /*
         * We did an non-blocking connection operation.
         * We placed a channel in non-blocking mode, and called connect() on it.
         * 
         * Now, the connection has been established, or the attempt has failed.
         * The channel has become connectable and NIO returned it in the set of selected keys.
         * 
         * Call channel.finishConnect() to complete the connection sequence.
         * If the connection operation failed, finishConnect() will throw an IOException. (ask)
         */

        // Find out if our connection attempt ended because we connected or if it ended because we weren't able to connect
        boolean finished = channel.finishConnect();

        // Our connection attempt worked, the channel is now connected to the remote computer
        if (finished) {

            // Set the bit mask of interested operations to all 0s, we're not interested in any operations yet
            sk.interestOps(0);

            // Call handleConnect() on the object we saved when we registered the channel
            handler.handleConnect();

        // The finishConnect() method returned false (do)
        } else {

            // Cancel the key, and call shutdown() on the object we've been calling handleRead() and handleWrite() on
            cancel(sk, handler);
        }
    }

    /**
     * Registers a given channel with the NIO selector.
     * 
     * @param selector   The NIO selector object that ties everything together
     * @param channel    A SelectableChannel that connects us to a remote computer
     * @param op         An ingeger with bits set for the operations, like reading and writing, that we want NIO to tell us about
     * @param attachment The object NIO can keep a reference to, we'll give exceptions to it
     */
    private void registerImpl(Selector selector, SelectableChannel channel, int op, IOErrorObserver attachment) {

        try {

            // Register the channel with the selector
            channel.register(
                selector,                    // Our one NIO selector object that ties everything together
                op,                          // An integer with bits set for the operations, like reading and writing, we want NIO to tell us about
                new Attachment(attachment)); // A reference to an Object we can use for whatever we want, wrap the IOErrorObserver in an Attachment and save it there

            /*
             * channel.register() returns the SelectionKey object that NIO makes to associate this channel with the selector.
             * We don't need to keep the key here.
             */

        // Registering the channel with the selector caused an exception
        } catch (IOException iox) {

            // Have the given IOErrorObserver handle it
            attachment.handleIOException(iox);
        }
    }

    /**
     * The NIODispatch thread does the actions that other threads called register() and invokeLater() to have it do.
     * 
     * Other threads called register(), putting RegisterOp objects in the REGISTER list.
     * Now, the NIODispatch thread will do the channel registrations.
     * 
     * Other threads called invokeLater(), putting Runnable objects in the LATER list
     * Now, the NIODispatch thread will call run() on them.
     */
    private void addPendingItems() {

        // Protect the lists from being accessed by more than one thread at a time
        synchronized (Q_LOCK) {

            // Loop for each NBThrottle object the program has made
            long now = System.currentTimeMillis(); // Get the time right now
            for (int i = 0; i < THROTTLE.size(); i++) {

                // Tell this NBThrottle object what time it is now by calling tick(now) on it
                ((NBThrottle)THROTTLE.get(i)).tick(now);
            }

            // Move everything from the REGISTER and LATER lists into the UNLOCKED list
            UNLOCKED.ensureCapacity(REGISTER.size() + LATER.size()); // Make sure the UNLOCKED list is big enough to hold the REGISTER and LATER lists combined
            UNLOCKED.addAll(REGISTER); // Add the contents of the REGISTER and LATER lists
            UNLOCKED.addAll(LATER);
            REGISTER.clear(); // Clear the REGISTER and LATER lists
            LATER.clear();
        }

        /*
         * It wouldn't be safe to call run() on all these objects inside the synchronized block.
         * One of the run() methods might call something that eventually dose some thread synchronization itself.
         * This could cause a deadlock.
         */

        // Loop through the UNLOCKED list, pointing item at each object
        if (!UNLOCKED.isEmpty()) {
            for (Iterator i = UNLOCKED.iterator(); i.hasNext(); ) {
                Object item = i.next();

                try {

                    // This is a RegisterOp object from the REGISTER list
                    if (item instanceof RegisterOp) {

                        // Register the channel
                        RegisterOp next = (RegisterOp)item;
                        registerImpl(selector, next.channel, next.op, next.handler);

                    // This is a Runnable object from the LATER list
                    } else if (item instanceof Runnable) {

                        // Call run() on it
                        ((Runnable)item).run();
                    }

                } catch (Throwable t) {

                    // Catch errors and record them, but just keep going
                    LOG.error(t);
                    ErrorService.error(t);
                }
            }

            // Now that we've looped through all the objects, empty the list
            UNLOCKED.clear();
        }
    }

    /**
     * Give every NBThrottle the entire key collection. (do)
     * 
     * Every time the NBThrottle constructor made a NBThrottle object, it added it to the THROTTLE list here.
     * process() calls this after getting the key collection, but before calling handleRead() and handleWrite() for each key.
     * This method loops through all the NBThrottle objects in our list, and gives each one the key collection by calling selectableKeys(keys).
     */
    private void readyThrottles(Collection keys) {

        // Loop for each NBThrottle object the program has
        List throttle = THROTTLE; // They're all in this list
        for (int i = 0; i < throttle.size(); i++) {

            // Give this NBThrottle object the entire selected key collection (do)
            ((NBThrottle)throttle.get(i)).selectableKeys(keys);
        }
    }

    /**
     * The NIODispatch thread loops forever here, calling select() and then iterating through the collection of selected keys.
     * This is the actual NIO run loop.
     * 
     * The NIODispatch thread starts in run() and then loops forever in this process() message.
     * If this method throws an exception, run() catches it, handles it, and the thread comes back here.
     * 
     * Sometimes, the selector will return really quickly with no keys.
     * This is called a spin.
     * The selector can break, and spin over and over again without ever giving us any keys.
     * This method has code that will notice if this happens.
     * 
     * @throws ProcessingException If the select() call caused an IOException
     * @throws SpinningException   If the selector keeps returning quickly with nothing
     */
    private void process() throws ProcessingException, SpinningException {

        /*
         * The call selector.select(100) is supposed to return a set of keys, or wait a tenth of a second before returning with nothing.
         * Sometimes, the call returns really quickly without giving us any keys.
         * If the selector is broken, it will start doing this over and over.
         * This is called spinning.
         * 
         * This method looks for this behavior, and throws a SpinningException if it happens.
         * 
         * The method uses 4 variables to detect spinning.
         * checkTime is a boolean and startSelect is a time.
         * The first time the selector returns nothing, we'll set checkTime to true.
         * The next time we call select(), we'll set startSelect beforehand to see how long select() blocks.
         * If select() returns really quickly without giving us any keys, that's a spin.
         * This spin of returning zero keys is counted by the zeroes integer.
         * 
         * While spinning, the selector may return some keys sometimes.
         * If it's been spinning a lot, we don't want it to return some keys, convince us that it's OK, and then just start spinning again.
         * This is what the ignores count is for.
         * If the selector has been spinning, and gives us some keys, we won't take it off the watch list yet.
         * We'll just count that as an ignore.
         * When it returns 5 good keysets, then we'll decide it's not spinning anymore.
         */

        // Initialize the variables we'll use to detect spinning
        boolean checkTime   = false; // If the selector gives us no keys, we'll set checkTime to true to start looking for spins
        long    startSelect = -1;    // When we're looking for spins, startSelect will tell us how long selector.select() takes before returning
        int     zeroes      = 0;     // When we're looking for spins, zeroes will count how many times the selector gave us nothing
        int     ignores     = 0;     // When we're looking for spins, ignores will count how many times the selector gave us some keys

        while (true) {

            /*
             * This sleep is technically not necessary, however occasionally selector
             * begins to wakeup with nothing selected.  This happens very frequently on Linux,
             * and sometimes on Windows (bugs, etc..).  The sleep prevents busy-looping.
             * It also allows pending registrations & network events to queue up so that
             * selection can handle more things in one round.
             * This is unrelated to the wakeup()-causing-busy-looping.  There's other bugs
             * that cause this.
             */

            // If the selector isn't spinning or the program isn't running on Windows
            if (!checkTime || !CommonUtils.isWindows()) {

                try {

                    // Pause the NIODispatch thread here for 1/20th of a second
                    Thread.sleep(50);

                // If another thread calls interrupt(), just write it into the log and keep going
                } catch (InterruptedException ix) { LOG.warn("Selector interrupted", ix); }
            }

            // Register all the channels and call run() on all the objects that other threads gave us
            addPendingItems();

            try {

                // Wait for one or more channels to become ready for an operation like reading or writing
                if (checkTime) startSelect = System.currentTimeMillis(); // If the selector is spinning, we need to know how long selector.select() will take
                selector.select(100); // This is supposed to returns when a channel is selected, or 1/10th of a second passes

            // Make a note about this exception in the debugging log, and go back to the start of the loop
            } catch (NullPointerException err) {

                LOG.warn("npe", err);
                continue;

            // Make a note about this exception in the debugging log, and go back to the start of the loop
            } catch (CancelledKeyException err) {

                LOG.warn("cancelled", err);
                continue;

            // An IOException is serious, wrap it in a ProcessingException and throw it
            } catch (IOException iox) {

                throw new ProcessingException(iox);
            }

            // Get the collection of keys that we can perform an operation on
            Collection keys = selector.selectedKeys();

            // The selector gave us nothing
            if (keys.size() == 0) {

                // It hasn't been spinning until now
                if (startSelect == -1) {

                    LOG.warn("No keys selected, starting spin check.");
                    checkTime = true;

                // It has been spinning, and it returned with nothing in less than 0.03 seconds
                } else if (startSelect + 30 >= System.currentTimeMillis()) {
                    
                    /*
                     * This is a spin, and shouldn't happen.
                     */

                    // This is a spin, and shouldn't happen
                    if (LOG.isWarnEnabled()) LOG.warn("Spinning detected, current spins: " + zeroes); // Write in the log about it
                    if (zeroes++ > SPIN_AMOUNT) throw new SpinningException(); // If it's happened more than 5000 times, throw an exception

                // It has been spinning, but it blocked for more than 0.03 seconds before returning nothing 
                } else {
                    
                    /*
                     * There are two ways the selector can convince us it's not spinning anymore.
                     * It can return some keys 5 times.
                     * Or, it can return nothing, but take longer than 0.03 seconds to do that.
                     */

                    // The selector returned nothing, but not right away, we'll stop accusing it of spinning
                    checkTime   = false; // We're not watching the selector for spinning anymore
                    startSelect = -1;
                    zeroes      = 0;
                    ignores     = 0;
                }

                // Go back to the top of the loop, we don't need to iterate through the keys and don't want to count this trip through the loop
                continue;

            // The selector has been spinning, but now it gave us some keys
            } else if (checkTime) {

                // This is good, but we don't trust it quite yet, record this good behavior
                ignores++;

                // It's returned keys more than 5 times since spinning, it's not spinning anymore
                if (ignores > MAX_IGNORES) {

                    // Reset the spinning detection variables
                    checkTime   = false; // We're not watching the selector for spinning anymore
                    zeroes      = 0;
                    startSelect = -1;
                    ignores     = 0;
                }
            }

            // Record how many keys the selector returned in the log
            if (LOG.isDebugEnabled()) LOG.debug("Selected (" + keys.size() + ") keys.");

            // Give every NBThrottle object the entire collection of selected keys (do)
            readyThrottles(keys);

            // Loop for each key in the collection the selector gave us
            for (Iterator it = keys.iterator(); it.hasNext(); /* Nothing here */) {

                // Get the next key in the collection, and if it's valid, process it
                SelectionKey sk = (SelectionKey)it.next();
				if (sk.isValid()) process(sk, sk.attachment(), 0xFFFF); // For allowed operations, pass a bit mask of all 1s
            }

            // Empty the collection of keys
            keys.clear();

            // Count this loop
            iteration++;
        }
    }

    /**
     * Calls methods like handleRead() and handleWrite() on the object that goes with a channel that NIO selected.
     * 
     * The process() method above gets a collection of selected keys, and then calls this one for each key.
     * NBThrottle.selectableKeys() also calls this method. (do)
     * 
     * @param sk A key from the collection that select() returned in process() above
     * @param proxyAttachment The Object reference we registered the channel with that NIO has been keeping for us
     * @param allowedOps      The operations to look for, process() calls this with all 1s
     */
    void process(SelectionKey sk, Object proxyAttachment, int allowedOps) {

        /*
         * We wrapped an IOErrorObserver in an Attachment object.
         * We had NIO save a reference to it when we registered the channel.
         * Now, NIO is giving us theat object reference back, it's proxyAttachment.
         * 
         * Cast it back to an Attachment, and get the IOErrorObserver object inside it.
         * This is the object we'll call handleRead() or handleWrite() on if the channel is selected for those operations.
         */

        // Get the IOErrorObserver we had NIO save when we registered the channel
        Attachment proxy = (Attachment)proxyAttachment;
        IOErrorObserver attachment = proxy.attachment;

        // This is the first time we've seen this key in this set of them
        if (proxy.lastMod == iteration) {

            // Record that we are going to hit it now (do)
            proxy.hits++;

        // We last modified this key on a previous iteration (do)
        } else if (proxy.lastMod < iteration) {

            /*
             * do not count ones that we've already processed (such as throttled items)
             */

            // Reset its hit count (do)
            proxy.hits = 0;
        }

        // Set proxy.lastMod to one more than iteration so neither of the two cases above will enter on this iteration again (do)
        proxy.lastMod = iteration + 1;

        // This key hasn't appeared more than 10,000 times in the same key collection (do)
        if (proxy.hits < MAXIMUM_ATTACHMENT_HITS) {

            try {

                try {

                    // If we're looking for the accept operation and this key says the channel is ready to accept
                    if ((allowedOps & SelectionKey.OP_ACCEPT) != 0 && sk.isAcceptable()) {

                        // Call the handleAccept() method on the object we had NIO keep when we registered the channel
                        processAccept(sk, (AcceptObserver)attachment);

                    // If we're looking for the connect operation and the key says the channel is ready to connect
                    } else if ((allowedOps & SelectionKey.OP_CONNECT)!= 0 && sk.isConnectable()) {

                        // Call the handleConnect() method on the object we had NIO keep when we registered the channel
                        processConnect(sk, (ConnectObserver)attachment);

                    } else {

                        // If we're looking for the read or write operations and the channel is ready for them, call handleRead() and handleWrite()
                        if ((allowedOps & SelectionKey.OP_READ)  != 0 && sk.isReadable()) ((ReadObserver)attachment).handleRead();
                        if ((allowedOps & SelectionKey.OP_WRITE) != 0 && sk.isWritable()) ((WriteObserver)attachment).handleWrite();
                    }

                // The key is cancelled
                } catch (CancelledKeyException err) {

                    // Log it and ignore it
                    LOG.warn("Ignoring cancelled key", err);

                // NIO threw us an IOException
                } catch (IOException iox) {

                    // Call handleIOException() on the same object we would be calling handleRead() or handleWrite() on
                    LOG.warn("IOX processing", iox);
                    attachment.handleIOException(iox);
                }

            // Some other exception happened
            } catch (Throwable t) {

                ErrorService.error(t, "Unhandled exception while dispatching");
                safeCancel(sk, attachment);
            }

        // Somehow, this one key appeared more than 10,000 times in the same key collection (do)
        } else {

            /*
             * we've had too many hits in a row.  kill this attachment.
             */

            // Cancel the key and close the channel
            if (LOG.isErrorEnabled()) LOG.error("Too many hits in a row for: " + attachment);
            safeCancel(sk, attachment);
        }
    }

    /**
     * Cancel a key and call shutdown() on the object we've been calling handleRead() and handleWrite() on.
     * Only process() above calls this.
     * 
     * @param sk         A key that appeared more than 10,000 times in the same key set (do)
     * @param attachment The Attachment object we had NIO keep a reference to when registering the channel
     */
    private void safeCancel(SelectionKey sk, Shutdownable attachment) {

        /*
         * This is a very safe cancel.
         * It ignores errors and only shuts down if possible.
         */

        try {

            // Cancel the key, and call shutdown() on the object we've been calling handleRead() and handleWrite() on
            cancel(sk, (Shutdownable)attachment);

        // Ignore every exception, they don't matter because we're closing the connection anyway
        } catch (Throwable ignored) {}
    }

    /**
     * Makes a new selector, registers all the channels to it, and replaces the old one with it.
     * 
     * When the selector breaks, the program can make a new one.
     * run() does this when process() throws an exception from the loop that calls select() to get selected keys.
     */
    private void swapSelector() {

        // Save a reference to the current selector
        Selector oldSelector = selector;

        // Get all the keys currently registered with the selector
        Collection oldKeys = Collections.EMPTY_SET;
        try {

            // Call keys() on the java.nio.channels.Selector object to have it give us all the keys
            if (oldSelector != null) oldKeys = oldSelector.keys();

        // If there's an exception, just leave oldKeys empty
        } catch (ClosedSelectorException ignored) { LOG.warn("error getting keys", ignored); }

        try {
            
            // Make a new selector
            selector = Selector.open();
            
        } catch (IOException iox) {

            // Throw a RuntimeException and not an IOException so run() will know an IOException came from Selector.open()
            LOG.error("Can't make a new selector!!!", iox);
            throw new RuntimeException(iox);
        }

        // Loop through all the keys we got from the old selector
        for (Iterator i = oldKeys.iterator(); i.hasNext(); ) {

            try {

                // Get an old key
                SelectionKey key = (SelectionKey)i.next();

                // Get all the information we need to do the registration from the old key
                SelectableChannel channel    = key.channel();     // The channel that goes with a socket
                Object            attachment = key.attachment();  // The Attachment object that contains the object we'll call handleRead() and handleWrite() on
                int               ops        = key.interestOps(); // The operation interest set of bits in an integer

                try {

                    // Register the channel with the new selector
                    channel.register(selector, ops, attachment); // This creates and returns a new key, but we don't need to keep it

                // This caused an exception
                } catch (IOException iox) {

                    // Have the given object handle it
                    ((Attachment)attachment).attachment.handleIOException(iox);
                }

            // If a key is cancelled, ignore it and just keep going
            } catch (CancelledKeyException ignored) { LOG.warn("key cancelled while swapping", ignored); }
        }

        try {

            // Close the old selector
            if (oldSelector != null) oldSelector.close();

        } catch (IOException ignored) { LOG.warn("error closing old selector", ignored); }
    }

    /**
     * The NIODispatch thread runs this run() method, and loops here the whole time.
     * 
     * When the NIODispatch thread is created, it calls this method named run().
     * This method calls process(), which calls selector.select() in a loop, and throws an exception if the selector breaks.
     * This method catches the exceptions process() throws, makes a new selector, and keeps going.
     */
    public void run() {

        // The NIODispatch thread runs here forever
        while (true) {

            try {

                // If we don't have a selector, make a new one
                if (selector == null) selector = Selector.open();

                // Call selector.select() and iterate through the collection of selected keys
                process();

            // The selector.select() started repeatedly returning really fast with no keys
            } catch (SpinningException spin) {

                // Make a new selector, and move all the channels from the old one to it
                LOG.warn("selector is spinning!", spin);
                swapSelector();

            // The selector.select() call caused an IOException
            } catch (ProcessingException uhoh) {

                // Make a new selector, and move all the channels from the old one to it
                LOG.warn("unknown exception while selecting", uhoh);
                swapSelector();

            // The Selector.open() call above threw an IOException
            } catch (IOException iox) { // process() doesn't throw IOException, so we know this came from Selector.open()

                // Wrap it as a RuntimeException and throw it
                LOG.error("Unable to create a new Selector!!!", iox);
                throw new RuntimeException(iox);

            // Something else threw some other kind of exception
            } catch (Throwable err) {

                // Record it, and replace the selector
                LOG.error("Error in Selector!", err);
                ErrorService.error(err);
                swapSelector();
            }
        }
    }

    /*
     * There are 4 nested classes defined in NIODispatcher.
     * 
     * RegisterOp bundles together the channel, IOErrorObserver, and bit flag integer we use to register a channel with the selector.
     * 
     * Attachment is the object we'll keep a reference to when registering a channel.
     * It contains an IOErrorObserver, and keeps track of some statistics.
     * 
     * The other two nested classes define exceptions: SpinningException and ProcessingException.
     * 
     * SpinningException
     * The selector can break, and start returning over and over again, really fast, with no keys.
     * If this happens, process() will throw a SpinningException.
     * 
     * ProcessingException
     * If selector.select() throws an IOException, process() will throw a ProcessingException, and the run() method will catch it.
     */

    /**
     * A RegisterOp just holds a SelectableChannel, IOErrorObserver, and operations bit flag integer.
     * These are the operands of the register call.
     * Only the NIODispatch thread can actually do the registration.
     * If another thrad needs to do one, it will make a RegisterOp object for the NIODispatch thread to do it.
     */
    private static class RegisterOp {

        // Member variables to hold the given objects
        private final SelectableChannel channel;
        private final IOErrorObserver handler;
        private final int op;

        /**
         * Make a new RegisterOp object that will hold everything the NIODispatch thread needs to do a channel registration.
         * 
         * @param channel The channel we need to register later
         * @param handler The object we will call methods like handleRead() and handleWrite() on when NIO tells us to
         * @param op      The operation interest set, with bits set for the operations we want NIO to tell us about
         */
        RegisterOp(SelectableChannel channel, IOErrorObserver handler, int op) {

            // Save the given channel, handler, and operations bit flag integer in the new object
            this.channel = channel;
            this.handler = handler;
            this.op = op;
        }
    }

    /**
     * When we register a channel with the selector, there is an extra Object reference we can use for whatever we want.
     * We use it to save a reference to an Attachment object, defined by this class.
     * 
     * The Attachment object contains a reference to an IOErrorObserver.
     * This is an object that has a handleIOError(e) method.
     * If NIO gives us an exception for a channel, we'll hand it off to this method.
     * 
     * The Attachment object also contains some statistics.
     */
    private static class Attachment {

        /** The object that we can call handleIOException(e) on. */
        private final IOErrorObserver attachment;

        /** Counts how many times we've modified (do) */
        private long lastMod;

        /** Counts (do) */
        private long hits;

        /**
         * Make a new Attachment object.
         * 
         * @param attachment If NIO gives us an exception for this channel, we'll call handleIOException(e) on this object
         */
        Attachment(IOErrorObserver attachment) {

            // Save a reference to the given IOErrorObserver object in the member variable named attachment
            this.attachment = attachment; // Attachment is the name of this class, while attachment is a member variable that points to an IOErrorObserver
        }
    }

    /**
     * Exception for process() to throw when the selector starts returning quickly with no keys.
     * 
     * If the selector returns in less than 0.03 seconds with no keys 5000 times in a row, there is something wrong with it.
     * This is called spinning.
     * The process() method above watches for this pattern of broken behavior, and throws a SpinningException if it detects it.
     */
    private static class SpinningException extends Exception {

        /** Make a new SpinningException object. */
        public SpinningException() {

            // Just call the Exception constructor
            super();
        }
    }

    /**
     * Exception for when selector.select() has a serious error.
     * 
     * If selector.select() throws an IOException, process() will throw a ProcessingException, and the run() method will catch it.
     */
    private static class ProcessingException extends Exception {

        /** Make a new empty ProcessingException. */
        public ProcessingException() {

            // Just call the Exception constructor
            super();
        }

        /**
         * Make a new ProcessingException given another exception.
         * 
         * @param t An exception we'll put inside this new one
         */
        public ProcessingException(Throwable t) {

            // Have the Exception constructor keep the given exception inside this one
            super(t);
        }
    }
}

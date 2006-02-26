
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.net.InetSocketAddress;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.util.ProcessingQueue;

/**
 * The MessageDispatcher class makes 3 asynchronous calls in LimeWire work:
 * UDPService.processMessage(m, addr) calls dispatchUDP(m, addr) to have the "MessageDispatch" thread call MessageRouter.handleUDPMessage(m, addr).
 * MulticastService.run() calls dispatchMulticast(m, addr) to have the "MessageDispatch" thread call MessageRouter.handleMulticastMessage(m, addr).
 * ManagedConnection.handleMessageInternal(m) calls dispatchTCP(m, conn) to have the "MessageDispatch" thread call MessageRouter.handleMessage(m, conn).
 * 
 * This lets the threads in UDPService, MulticastService, and ManagedConnection return immediately.
 * The "MessageDispatch" thread executes the code in the MessageRouter class.
 * 
 * There is one single MessageDispatcher object as LimeWire runs.
 * You can get a reference to it by calling MessageDispatcher.instance().
 * 
 * The MessageDispatcher makes a ProcessingQueue, which creates the "MessageDispatch" thread.
 */
class MessageDispatcher {

    /**
     * When Java loads this class, this line of code makes the program's single MessageDispatcher object.
     */
    private static final MessageDispatcher INSTANCE = new MessageDispatcher();

    /**
     * Make the MessageDispatcher object.
     * Doesn't contain any code because there's nothing we need to do to set up the MessageDispatcher.
     * Marked private to prevent external code from making a MessageDispatcher object.
     */
    private MessageDispatcher() {}

    /**
     * Get a reference to the single MessageDispatcher object.
     * When a thread first enters this method, Java loads this class.
     * The line of code above runs, creating the program's MessageDispatcher object and pointing INSTANCE at it.
     * This method returns that reference.
     * 
     * 3 methods in LimeWire call MessageDispatcher.instance() to get access to the MessageDispatcher.
     * UDPService.processMessage(Message, InetSocketAddress) accesses it to give us a Gnutella packet we've received over UDP.
     * ManagedConnection.handleMessageInternal(Message) accesses it to give us a Gnutella packet we've received over a TCP Gnutella connection.
     * MulticastService.run() accesses it to give us a Gnutella packet we've received over multicast UDP on the LAN. (do)
     * 
     * @return The program's single MessageDispatcher object
     */
    public static MessageDispatcher instance() {

        // Return a reference to the single MessageDispatcher object
        return INSTANCE;
    }

    /**
     * Make a ProcessingQueue that will create a thread named "MessageDispatch".
     * We can add objects that have a run() method to the ProcessingQueue, and its thread will call run() on them one at a time.
     */
    private final ProcessingQueue DISPATCH = new ProcessingQueue("MessageDispatch");

    /**
     * Have the "MessageDispatch" thread call MessageRouter.handleUDPMessage(message, addr).
     * 
     * @param m    A Gnutella packet we just received
     * @param addr The IP address and port number we got it from
     */
    public void dispatchUDP(Message m, InetSocketAddress addr) {

        // Wrap the given Gnutella packet and source address into a new UDPDispatch object, and add it to the ProcessingQueue
        DISPATCH.add(new UDPDispatch(m, addr));
    }

    /**
     * Have the "MessageDispatch" thread call MessageRouter.handleMulticastMessage(m, addr).
     * 
     * @param m    A Gnutella packet we just received
     * @param addr The IP address and port number we got it from
     */
    public void dispatchMulticast(Message m, InetSocketAddress addr) {

        // Wrap the given Gnutella packet and source address into a new MulticastDispatch object, and add it to the ProcessingQueue
        DISPATCH.add(new MulticastDispatch(m, addr));
    }

    /**
     * Have the "MessageDispatch" thread call MessageRouter.handleMessage(m, conn).
     * 
     * @param m    A Gnutella packet we just received
     * @param conn The ManagedConnection object that represents the remote computer we have a Gnutella TCP socket connection with that just sent us that packet
     */
    public void dispatchTCP(Message m, ManagedConnection conn) {

        // Wrap the given Gnutella packet and source connection into a new TCPDispatch object, and add it to the ProcessingQueue
        DISPATCH.add(new TCPDispatch(m, conn));
    }

    /**
     * A UDPDispatch object holds a Gnutella packet with the address we just received it from.
     * UDPDispatch implements Java's Runnable interface, which means it has a run() method.
     * It also means we can add a UDPDispatch object to the DISPATCH ProcessingQueue, which will have its "MessageDispatch" thread call run() on it.
     */
    private static class UDPDispatch implements Runnable {

        /** Get a reference to the program's single MessageRouter object. */
        private static final MessageRouter ROUTER = RouterService.getMessageRouter();

        /** The Gnutella packet this UDPDispatch object will dispatch. */
        private final Message m;

        /** The IP address and port number we got the packet from. */
        private final InetSocketAddress addr;

        /**
         * Make a new UDPDispatch object that keeps a Gnutella packet with its source address, and has a run() method.
         * 
         * @param m    A Gnutella packet we just received
         * @param addr The IP address and port number we got it from
         */
        UDPDispatch(Message m, InetSocketAddress addr) {

            // Save the packet and address in this new object
            this.m    = m;
            this.addr = addr;
        }

        /**
         * The "MessageDispatch" thread will call this run() method shortly after code above makes this UDPDispatch object.
         * Gives the message and source address to MessageRouter.handleUDPMessage().
         */
        public void run() {

            // Forward the call above to MessageRouter.handleUDPMessage(m, addr)
            ROUTER.handleUDPMessage(m, addr);
        }
    }

    /**
     * A MulticastDispatch object holds a Gnutella packet with the address we just received it from.
     * MulticastDispatch implements Java's Runnable interface, which means it has a run() method.
     * It also means we can add a MulticastDispatch object to the DISPATCH ProcessingQueue, which will have its "MessageDispatch" thread call run() on it.
     */
    private static class MulticastDispatch implements Runnable {

        /** Get a reference to the program's single MessageRouter object. */
        private static final MessageRouter ROUTER = RouterService.getMessageRouter();

        /** The Gnutella packet this UDPDispatch object will dispatch. */
        private final Message m;

        /** The IP address and port number we got the packet from. */
        private final InetSocketAddress addr;

        /**
         * Make a new MulticastDispatch object that keeps a Gnutella packet with its source address, and has a run() method.
         * 
         * @param m    A Gnutella packet we just received
         * @param addr The IP address and port number we got it from
         */
        MulticastDispatch(Message m, InetSocketAddress addr) {

            // Save the packet and address in this new object
            this.m    = m;
            this.addr = addr;
        }

        /**
         * The "MessageDispatch" thread will call this run() method shortly after code above makes this MulticastDispatch object.
         * Gives the message and source address to MessageRouter.handleMulticastMessage().
         */
        public void run() {

            // Forward the call above to MessageRouter.handleMulticastMessage(m, addr)
            ROUTER.handleMulticastMessage(m, addr);
        }
    }

    /**
     * A TCPDispatch object holds a Gnutella packet with the ManagedConnection that just sent it to us.
     * TCPDispatch implements Java's Runnable interface, which means it has a run() method.
     * It also means we can add a TCPDispatch object to the DISPATCH ProcessingQueue, which will have its "MessageDispatch" thread call run() on it.
     */
    private static class TCPDispatch implements Runnable {

        /** Get a reference to the program's single MessageRouter object. */
        private static final MessageRouter ROUTER = RouterService.getMessageRouter();

        /** The Gnutella packet this UDPDispatch object will dispatch. */
        private final Message m;

        /** The TCP socket Gnutella connection we got this packet from. */
        private final ManagedConnection conn;

        /**
         * Make a new TCPDispatch object that keeps a Gnutella packet with its source connection, and has a run() method.
         * 
         * @param m    A Gnutella packet we just received
         * @param conn The ManagedConnection object that represents the remote computer we have a Gnutella TCP socket connection with that just sent us that packet
         */
        TCPDispatch(Message m, ManagedConnection conn) {

            // Save the packet and connection object in this new one
            this.m    = m;
            this.conn = conn;
        }

        /**
         * The "MessageDispatch" thread will call this run() method shortly after code above makes this TCPDispatch object.
         * Gives the message and source connection to MessageRouter.handleMessage().
         */
        public void run() {

            // Forward the call above to MessageRouter(m, conn)
            ROUTER.handleMessage(m, conn);
        }
    }
}

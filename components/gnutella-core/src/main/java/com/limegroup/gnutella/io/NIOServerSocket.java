
// Commented for the Learning branch

package com.limegroup.gnutella.io;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.SocketAddress;
import java.net.ServerSocket;

import java.util.List;
import java.util.LinkedList;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Make a NIOServerSocket to create a listening socket remote computers can connect to.
 * 
 * Code here makes the ServerSocket and associated NIO ServerSocketChannel objects, and keeps them together.
 * The constructor calls bind() to start the socket listening on the given port number.
 * 
 * This class extends ServerSocket, and the Acceptor class thinks it is a ServerSocket.
 * It uses LOCK.wait() and LOCK.notify() to turn non-blocking NIO into the blocking behavior that the Acceptor class expects.
 * 
 * Extends and Implements
 * java.net.ServerSocket: NIOServerSocket extends ServerSocket to take ServerSocket's method names.
 * AcceptObserver:        NIO can tell this object a remote computer has connected, handleAccept().
 */
public class NIOServerSocket extends ServerSocket implements AcceptObserver {

    /** We can save lines of text to this debugging log to record how the program acts when running. */
    private static final Log LOG = LogFactory.getLog(NIOServerSocket.class);

    /*
     * For a connection socket, the types are java.net.Socket and java.nio.channels.SocketChannel.
     * This is a listening socket, so the types are java.net.ServerSocket and java.nio.channels.ServerSocketChannel.
     * In both cases, the socket and channel are a pair that together.
     */

    /** The ServerSocketChannel object that is paired with the ServerSocket here and listens for new connections. */
    private final ServerSocketChannel channel;
    /** The ServerSocket object that is paired with the ServerSocketChannel here and listens for new connections. */
    private final ServerSocket socket;

    /**
     * A list of sockets connected to remote computers that just connected to us.
     * NIO gives a new one to handleAccept(), which puts it in this list and wakes up the thread waiting in accept().
     * The thread in accept() takes it from the list and returns it.
     */
    private final List pendingSockets = new LinkedList(); // Start out with an empty LinkedList object

    /**
     * When NIO has an exception for us, it calls our handleException() method.
     * handleException() stores it in storedException, and then has the thread in accept() take care of it.
     */
    private IOException storedException = null;

    /**
     * Object used for thread synchronization.
     * Makes sure only one thread at a time can access the pendingSockets list and the storedException reference.
     * The thread in accept() waits on LOCK, and then another in handleAccept() wakes it up.
     */
    private final Object LOCK = new Object();

    /** Not used. */
    public NIOServerSocket() throws IOException {
        channel = ServerSocketChannel.open();
        socket  = channel.socket();
        init();
    }

    /**
     * Make a new NIOServerSocket object that contains a ServerSocket listening on the given port number.
     * Acceptor.setListeningPort() uses this constructor to create the program's TCP listening socket.
     * 
     * @param port The port number this new listening socket should start listening on
     */
    public NIOServerSocket(int port) throws IOException {

        // Make a new ServerSocketChannel object, and get the ServerSocket object associated with it
        channel = ServerSocketChannel.open();
        socket  = channel.socket();
        
        // Set the channel in non-blocking mode
        init();

        // Start the socket listening on the given port number
        bind(new InetSocketAddress(port)); // Wrap the port number into an InetSocketAddress object, leave the IP address 0.0.0.0
    }

    /** Not used. */
    public NIOServerSocket(int port, int backlog) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        bind(new InetSocketAddress(port), backlog);
    }

    /** Not used. */
    public NIOServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        channel = ServerSocketChannel.open();
        socket = channel.socket();
        init();
        bind(new InetSocketAddress(bindAddr, port), backlog);
    }

    /**
     * Set our ServerSocketChannel in non-blocking mode.
     * This means when we tell it to do something, the call won't block.
     * Later, NIO will tell us what happened.
     */
    private void init() throws IOException {

        // Configure the ServerSocketChannel to not block when we use it
        channel.configureBlocking(false);
    }

    /**
     * Waits here until a remote computer connects to us, and then returns a new NIOSocket object we can talk to it through.
     * 
     * In Acceptor, the run() method has an infinite loop.
     * The thread there keeps calling this accept() method with this line of code:
     * 
     *   client = _socket.accept()
     * 
     * It looks like this calls into Java, but it calls here.
     * _socket looks like a Java ServerSocket object, but it's actually a LimeWire NIOServerSocket object.
     * NIOServerSocket is pretending to be ServerSocket, and Acceptor needs to not be able to notice the difference.
     * 
     * The Acceptor class expects this method to block, just like Java's ServerSocket.accept() method would.
     * But, we're using NIO here, and NIO doesn't block.
     * We use LOCK.wait() and LOCK.notify() to simulate blocking.
     * When the thread here hits LOCK.wait(), it falls asleep.
     * Then, a remote computer connects to us.
     * NIO calls handleAccept(), which calls LOCK.notify().
     * This wakes the thread here up, which gets the new connection socket and returns it.
     * 
     * @return A new NIOSocket object we can use to talk to the remote computer that connected to us
     */
    public Socket accept() throws IOException {

        // Only let one thread at a time in any one of the synchronized blocks in this class
        synchronized (LOCK) {

            // Get the timeout value test code stored in the socket
            int timeout = getSoTimeout();

            // Loop until our listening socket is closed, isn't listening, or NIO gave us an exception or a connection socket
            boolean looped = false; // We haven't slept here yet
            while (!isClosed() && isBound() && storedException == null && pendingSockets.isEmpty()) {

                // If we did sleep here and test code set a timeout, throw an exception
                if (looped && timeout != 0) throw new SocketTimeoutException("accept timed out: " + timeout);

                // Now this thread is about to go to sleep
                LOG.debug("Waiting for incoming socket...");

                try {

                    // Have this thread wait here until another in handleAccept() below calls LOCK.notify()
                    LOCK.wait(timeout); // Or, the timeout expires

                // Another thread called LOCK.interrupt(), wrap it as an InterruptedIOException and throw it
                } catch (InterruptedException ix) { throw new InterruptedIOException(ix); }

                // Record that we have slept here now
                looped = true;
            }

            // Move an exception from storedException to x
            IOException x = storedException; // NIO called handleException(), which put the exception here
            storedException = null;          // Move it to x, don't copy it

            // Our listening socket is closed
            if (isClosed()) {

                // Throw a SocketException
                throw new SocketException("Socket Closed");

            // NIO gave us an exception
            } else if (x != null) {

                // Throw it
                throw x;

            // Our listening socket isn't listening
            } else if (!isBound()) {

                // Throw a SocketException
                throw new SocketException("Not Bound!");

            // Otherwise, we must have been woken up because a remote compuer connected to us
            } else {

                // Remove the socket from the list, and return it
                LOG.debug("Retrieved a socket!");
                return new NIOSocket((Socket)pendingSockets.remove(0)); // Wrap the new connection socket in an NIOSocket object with the NIOSocket constructor
            }
        }
    }

    /**
     * NIO calls this when a remote computer has connected to us.
     * 
     * The ServerSocket and ServerSocketChannel this NIOServerSocket object keep listen for new connections.
     * When a remote computer connects to us, NIODispatcher calls this handleAccept() method.
     * It passes us a different SocketChannel object.
     * This new channel is our connection to the remote computer.
     * It is not the same thing as the channel paired with our listening socket.
     * 
     * @param channel A new channel we can talk to the remote computer through
     */
    public void handleAccept(SocketChannel channel) {

        // Only let one thread at a time in any one of the synchronized blocks in this class
        synchronized(LOCK) {

            // Get the socket associated with the given channel, and add it to the List of them
            pendingSockets.add(channel.socket());
            
            // Wake up the thread waiting in the accept() method above
            LOCK.notify();
        }
    }
    
    /**
     * NIO calls this when an IOException happened.
     * 
     * @param iox The IOException from NIO that we need to deal with
     */
    public void handleIOException(IOException iox) {

        // Only let one thread at a time in any one of the synchronized blocks in this class
        synchronized(LOCK) {

            // Save it in storedException, the thread in accept() will pick it up from there
            storedException = iox;
        }
    }

    /**
     * Wake up the thread in accept() and close our listening socket.
     */
    public void shutdown() {

        try {

            // Wake up the thread in accept() and close our listening socket
            close();

        // Ignore any exceptions, we're shutting down anyway
        } catch (IOException ignored) {}
    }

    /**
     * This object keeps a ServerSocket, start it listening on the given port number.
     * 
     * When we call socket.bind(endpoint) here, Windows Firewall will pop open a warning.
     * The program should register itself with Windows Firewall before this code runs.
     * 
     * @param endpoint A SocketAddress object that contains the port number to listen on
     */
    public void bind(SocketAddress endpoint) throws IOException {

        // Bind our end of our listening socket to the given port number
        socket.bind(endpoint); // The IP address in endpoint is 0.0.0.0, we're just giving it the port number

        // Register the channel with NIO as one that will listen for new connections
        NIODispatcher.instance().registerAccept(channel, this);
    }

    /** Not used. */
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        socket.bind(endpoint, backlog);
        NIODispatcher.instance().registerAccept(channel, this);
    }

    /**
     * Close this NIOServerSocket object.
     * Wakes up the thread in accept().
     * Closes our ServerSocket, which stops it from listening.
     */
    public void close() throws IOException {

        // Only let one thread at a time in any one of the synchronized blocks in this class
        synchronized (LOCK) {

            // Wake up the thread waiting on LOCK.wait() in the accept() method above
            LOCK.notify();

            // Close our ServerSocket
            socket.close();
        }
    }

    /*
     * NIOServerSocket extends ServerSocket, and contains a ServerSocket.
     * There are really 2 sockets here, the socket NIOServerSocket contains, and the socket that NIOServerSocket is.
     * NIOServerSocket only uses the socket it contains, and doesn't use the socket that it is.
     * The class extends ServerSocket just to steal its method names.
     * Here, we override these methods and have the socket we're holding do each one.
     */

    /** Not used. */
    public ServerSocketChannel getChannel() {
        return socket.getChannel();
    }

    /** Not used. */
    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    /** Only used by test code. */
    public int getLocalPort() {
        return socket.getLocalPort();
    }
    
    /** Not used. */
    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }
    
    /** Not used. */
    public int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }
    
    /** Not used. */
    public boolean getReuseAddress() throws SocketException {
        return socket.getReuseAddress();
    }
    
    /**
     * Gets the timeout value stored in the socket.
     * 
     * @return The socket timeout value.
     */
    public int getSoTimeout() throws IOException {

        // Ask the ServerSocket for this value
        return socket.getSoTimeout();
    }

    /**
     * True if the ServerSocket is listening for new connections.
     * When we bound the ServerSocket to a local port number, it started listening.
     * 
     * @return True if the socket here is listening
     */
    public boolean isBound() {

        // Ask the ServerSocket if it's bound
        return socket.isBound();
    }

    /**
     * True if the ServerSocket is closed.
     * 
     * @return True if the socket here is closed
     */
    public boolean isClosed() {

        // Ask the ServerSocket if it's closed
        return socket.isClosed();
    }

    /** Not used. */
    public void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }

    /**
     * Enable or disable the socket's ability to use the same address as a socket we recently closed.
     * 
     * When we close a TCP connection, it may remain in a timeout state for a period of time longer.
     * Then, if we want to bind a new socket to the same address, we can't, because the socket we closed is still around.
     * Call setResumeAddress(true) before bind() to have it use the address even if this happens.
     * This is the SO_REUSEADDR socket option.
     * 
     * The MiniAcceptor and a lot of test code uses this.
     * 
     * @param b True to let the socket use a closing address, false to have it fail instead
     */
    public void setReuseAddress(boolean b) throws SocketException {

        // Set the ServerSocket this way
        socket.setReuseAddress(b);
    }

    /**
     * Set the timeout on the socket.
     * A lot of test code uses this.
     * 
     * @param timeout The timeout to set
     */
    public void setSoTimeout(int timeout) throws SocketException {

        // Set the ServerSocket this way
        socket.setSoTimeout(timeout);
    }

    /**
     * Compose text information about this NIOServerSocket object.
     * 
     * @return A string like "NIOServerSocket::" (do)
     */
    public String toString() {

        // Compose text and return it
        return "NIOServerSocket::" + socket.toString();
    }
}

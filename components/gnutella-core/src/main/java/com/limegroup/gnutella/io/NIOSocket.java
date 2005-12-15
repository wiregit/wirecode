
// Commented for the Learning branch

package com.limegroup.gnutella.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.channels.SocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketAddress;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A socket that does all of its connecting, reading and writing using NIO.
 * 
 * This class provides input and output streams you can use for blocking IO.
 * The class uses non-blocking IO internally, and makes threads wait to simulate blocking.
 * To switch to using event-based reads, call setReadObserver().
 * NIO will call the handleRead() method there when it's time to read.
 * You have to use a ChannelReadObserver so the socket can set the appropriate underlying channel.
 * 
 * NIOSocket extends Socket, meaning that it is a socket.
 * However, we never use that socket.
 * Instead, we create or get passed one and save it in the member variable named socket.
 * NIOSocket extends Socket just to use its interface.
 * 
 * This class is for Java NIO, while socket is a java.net.Socket object.
 * This is because NIO didn't define a new kind of socket.
 * It just added NIO abilities to java.net.Socket.
 * With NIO, you can call Socket s; s.getChannel(); to get an NIO channel from it.
 * You can then read and write to that channel without blocking.
 * The Socket and SocketChannel objects make up a pair.
 * 
 * Extends and Implements
 * ConnectObserver: NIO can tell this NIOSocket object a connection it initiated is made, handleConnect().
 * NIOMultiplexor:  When NIO tells this NIOSocket object to read or write, it has objects to call to do it instead, setReadObserver() and setWriteObserver().
 * ReadObserver:    NIO can tell this NIOSocket object to read now, handleRead().
 * WriteObserver:   NIO can tell this NIOSocket object to get data and write now, handleWrite().
 */
public class NIOSocket extends Socket implements ConnectObserver, NIOMultiplexor {

    /** Make a log for this class that we can leave notes in to document how the program runs. */
    private static final Log LOG = LogFactory.getLog(NIOSocket.class);

    /*
     * In NIO, a java.net.Socket and its associated java.nio.channels.SocketChannel go together as a pair.
     * Given one, you can ask Java for the other.
     * NIOSocket keeps both here, and names them socket and channel.
     */

    /** The java.net.Socket object that gets connected to the remote computer. */
    private final Socket socket;
    /** The java.nio.channels.SocketChannel object we communicate with the remote computer through. */
    private final SocketChannel channel;

    /*
     * NIOSocket makes 4 LimeWire objects to help exchange data with the remote computer.
     * For sending data, it makes a NIOOutputStream and a BufferOutputStream.
     * For receiving data, it makes a NIOInputStream and a BufferInputStream.
     * 
     * NIOOutputStream implements the WriteObserver interface, which requires a handleWrite() method.
     * NIO calls handleWrite() to get the NIOOutputStream to send data from its buffer into the channel.
     * 
     * NIOInputStream implements the ReadObserver interface, which requires a handleRead() method.
     * NIO calls handleRead() to get the NIOInputStream to read data from the channel into its buffer.
     * 
     * To send data to the remote computer, call getOutputStream().
     * It will return a reference to the BufferOutputStream object, call write(b) on it.
     * 
     * To get data the remote computer has sent us, call getInputStream().
     * It will return a reference to the BufferInputStream object, call read() on it.
     */

    /**
     * An object that implements the WriteObserver interface.
     * This means NIO can call handleWrite() on it when it's time for it to write.
     * 
     * At first, writer will be the NIOOutputStream object the constructor made.
     * Then, ManagedConnection.startOutput() will call setWriteObserver, giving it a new MessageWriter instead.
     */
    private WriteObserver writer;
    
    /**
     * An object that implements the ReadObserver interface.
     * This means NIO can call handleRead() on it when it's time for it to read.
     * 
     * At first, reader will be the NIOInputStream object the constructor made.
     * Then, ManagedConnection.loopForMessages() will call setReadObserver, giving it a new MessageReader instead.
     */
    private ReadObserver reader;

    /** Any exception that occurred while trying to connect */
    private IOException storedException = null;

    /**
     * The IP address of the remote computer our socket and channel are connected to.
     * 
     * When we disconnect the channel, it won't tell us the IP address we were connected to anymore.
     * So, we need to keep a copy of the information here.
     */
    private InetAddress connectedTo;

    /** True if our connection to the remote computer has been shut down. */
    private boolean shuttingDown;

    /**
     * An object NIOSocket uses with wait() and notify() thread calls to make connect() block like LimeWire expects.
     * 
     * When a thread calls connect(), it waits on this lock for NIO to make the connection.
     * When NIO makes the connection, it calls handleConnect(), which wakes the waiting thread up.
     * 
     * The call to make a thread wait on the lock is LOCK.wait(timeout), this is in connect().
     * The call to wake that thread up is LOCK.notify(), this is in handleConnect().
     * 
     * NIO is non-blocking, but this lock lets it provide a connect() method that blocks.
     * A large portion of LimeWire still expects the connect() call to block.
     */
    private final Object LOCK = new Object();

    /**
     * Make a new NIOSocket object for a remote computer that has connected to us.
     * NIOServerSocket.accept() calls this when a remote computer has connected to the listening socket in the Acceptor class.
     * 
     * Saves the Socket and SocketChannel pair of objects.
     * Makes NIOOutputStream, BufferOutputStream, NIOInputStream, and BufferInputStream objects beneath this one.
     * 
     * @param s The java.net.Socket connection socket NIO gave us for the remote computer that just connected to us
     */
    NIOSocket(Socket s) throws IOException {

        /*
         * In NIO, there are 2 objects that go together for a single connection.
         * java.net.Socket is the connection socket.
         * java.nio.channels.SocketChannel is the non-blocking communications channel.
         * The objects always exist as a pair.
         * If you have either one, you can ask Java for a reference to the other.
         */

        // Save the Socket and SocketChannel objects for this connection in this NIOSocket object
        channel = s.getChannel(); // Get the NIOSocketChannel object associated with the given connection java.net.Socket object
        socket = s;

        // Make NIOInputStream and NIOOutputStream objects that we'll use to exchange data with the remote computer
        writer = new NIOOutputStream(this, channel);
        reader = new NIOInputStream(this, channel);
        ((NIOOutputStream)writer).init(); // Have the NIOOutputStream make a BufferOutputStream and get an 8 KB ByteBuffer
        ((NIOInputStream)reader).init();  // Have the NIOInputStream make a BufferInputStream and get an 8 KB ByteBuffer

        // Tell NIO we want to know when we can send this remote computer data, and when data has arrived from it for us
        NIODispatcher.instance().registerReadWrite(channel, this);
        connectedTo = s.getInetAddress();
    }

    /**
     * Make a new NIOSocket object we can use later to try to connect to a remote computer.
     * Sockets.connectAndRelease() calls this.
     * 
     * Makes a new Socket and SocketChannel pair of objects.
     * Makes NIOOutputStream and NIOInputStream objects beneath this one.
     */
    public NIOSocket() throws IOException {

        // Make a new pair of Socket and SocketChannel objects
        channel = SocketChannel.open(); // Call the static method java.nio.channels.SocketChannel.open() to make a new SocketChannel object
        socket = channel.socket();      // Get the java.net.Socket object that it goes with

        // Tell NIO we want to use the channel in non-blocking mode
        init();

        // Make NIOInputStream and NIOOutputStream objects that we'll use to exchange data with the remote computer
        writer = new NIOOutputStream(this, channel);
        reader = new NIOInputStream(this, channel);
    }

    /** Only unit test code uses this constructor. */
    public NIOSocket(InetAddress addr, int port) throws IOException {

        // Only unit test code uses this constructor
        channel = SocketChannel.open();
        socket = channel.socket();
        init();
        writer = new NIOOutputStream(this, channel);
        reader = new NIOInputStream(this, channel);
        connect(new InetSocketAddress(addr, port));
    }

    /** This constructor is never used. */
    public NIOSocket(InetAddress addr, int port, InetAddress localAddr, int localPort) throws IOException {
        
        // This constructor is never used
        channel = SocketChannel.open();
        socket = channel.socket();
        init();
        writer = new NIOOutputStream(this, channel);
        reader = new NIOInputStream(this, channel);
        bind(new InetSocketAddress(localAddr, localPort));
        connect(new InetSocketAddress(addr, port));
    }

    /** Only unit test code uses this constructor. */
    public NIOSocket(String addr, int port) throws UnknownHostException, IOException {

        // Only unit test code uses this constructor
        this(InetAddress.getByName(addr), port);
    }

    /** This constructor is never used. */
    public NIOSocket(String addr, int port, InetAddress localAddr, int localPort) throws IOException {
        
        // This constructor is never used
        this(InetAddress.getByName(addr), port, localAddr, localPort);
    }

    /** Configure our SocketChannel channel so it won't block. */
    private void init() throws IOException {

        // Turn blocking mode for the channel off
        channel.configureBlocking(false);
    }

    /**
     * Set this NIOSocket so that when NIO tells it to read, it tells the first object in the chain of readers to read instead.
     * The read chain will look like this:
     * 
     *   (this computer) - MessageReader - InflaterReader - NIOSocket - (remote computer)
     * 
     * This NIOSocket object at the end contains channel, the SelectableChannel connected to the remote computer.
     * NIO will tell channel when there is data for channel to read.
     * It will do this by calling NIOSocket.handleRead().
     * 
     * NIOSocket.handleRead() just calls reader.handleRead(), forwarding the call to the reader this NIOSocket object has saved.
     * At the start, this reader is the NIOInputStream that this NIOSocket made for itself.
     * 
     * Now, MangedConnection has finished building the chain of readers.
     * We want NIO's command to read to hit the first object in the chain, MessageReader.
     * MessageReader will be told to read, it will read from InflaterReader, and InflaterReader will read from this NIOSocket object.
     * 
     * This is how a push becomes a pull.
     * NIO's notification that the channel can read doesn't push data onto the channel.
     * Rather, it causes MessageReader to read, pulling data in through InflaterReader and from us.
     * 
     * In ManagedConnection, loopForMessages() makes a new MessageReader, the object that slices uncompressed data into Gnutella packets.
     * It then calls _socket.setReadObserver(reader), giving us the MessageReader.
     * This is the only place this method is called.
     * Also, ManagedConnection calls this just one time.
     * As LimeWire runs, this method is only used once for each remote computer we're connected to.
     * 
     * Sets the new ReadObserver.
     * Finds the most distant ChannelReader in the chain.
     * Sets its source to the prior reader, and tells it to read.
     * This empties any data it has buffered.
     * With that done, sets the source to the socket's channel.
     * Turns interest in reading on.
     * 
     * @param newReader The MessageReader packet slicer ManagedConnection made.
     *                  The object at the start of the read chain.
     *                  The object that NIO's command to read should be forwarded to.
     */
    public void setReadObserver(final ChannelReadObserver newReader) {

        /*
         * This code defines a new class right here.
         * It's not named, but is Runnable, which means it has a run() method.
         * A thread can run a Runnable class later.
         * We make the class, and hand it to the NIODispatcher object's invokeLater() method.
         * When the right thread calls invokeLater(), it calls run() here.
         */

        // Define a new Runnable class right here, and have the NIODispatcher thread call its run() method later.
        NIODispatcher.instance().invokeLater(new Runnable() { public void run() {

            // Point oldReader and reader at this NIOSocket object's NIOInputStream
            ReadObserver oldReader = reader; // Point oldReader at the NIOInputStream

            try {

                // Change reader to point at the given MessageReader object instead of the NIOInputStream
                reader = newReader;                    // Point reader at the given MessageReader object
                ChannelReader lastChannel = newReader; // Point lastChannel at the given MessageReader object

                // This loop actually only runs one time
                while (lastChannel.getReadChannel() instanceof ChannelReader) {

                    // The given MessageReader reads from an InflaterReader, point lastChannel at it
                    lastChannel = (ChannelReader)lastChannel.getReadChannel(); // Point lastChannel at the InflaterReader
                }

                // This is true, oldReader is our NIOInputStream, and newReader is the given MessageReader packet slicer
                if (oldReader instanceof ReadableByteChannel && oldReader != newReader) {

                    // Empty the data from our NIOInputStream, and shut it down
                    lastChannel.setReadChannel((ReadableByteChannel)oldReader); // Set the InflaterReader to read from our NIOInputStream
                    reader.handleRead();  // Call MessageReader.handleRead() to have it pull all the buffered data through
                    oldReader.shutdown(); // This is the end of the NIOInputStream, call shutdown() on it
                }

                // Have the InflaterReader read directly from channel, the SocketChannel object connected to the remote computer
                lastChannel.setReadChannel(channel);

                // Register our channel with NIO so NIO will tell us when our channel has data for us to read
                NIODispatcher.instance().interestRead(channel, true);

            } catch (IOException iox) {

                // Shut down this NIOSocket object, and our NIOInputStream
                shutdown();
                oldReader.shutdown(); // If the exception happened before we did this above, we'll need to do it here
            }
        }});
    }

    /**
     * Set this NIOSocket so that when you write to it, you're actually writing to the MessageWriter at the start of the write chain.
     * 
     * Only ManagedConnection.startOutput() calls this method.
     * startOutput() created the base of the chain of writers, which looks like this:
     * 
     *   MessageWriter -> DeflaterWriter -> DelayedBufferWriter -> ThrottleWriter
     * 
     * Code does the following things.
     * Flush and remove the NIOOutputStream object we were using, but won't be a part of the chain of writers.
     * Find the ThrottleWriter at the end of the chain.
     * Make a new SocketInterestWriteAdapater.
     * 
     * The SocketInterestWriteAdapter is core to how LimeWire writes.
     * When you call handleWrite() on this NIOSocket object, the call will go to SocketInterestWriteAdapter.handleWrite().
     * When ThrottleWriter writes, it sends data to the SocketInterestWriteAdapter.
     * The SocketInterestWriteAdapter writes to channel, the actual java.nio.channels.SocketChannel object connected to the remote computer.
     * 
     * We'll use the SocketInterestWriteAdapter as a hub to receive and forward interest calls.
     * 
     * @param newWriter The MessageWriter ManagedConnection made that starts the base of the chain of writers
     */
    public void setWriteObserver(final ChannelWriter newWriter) {

        // Define a new Runnable class right here, and have the NIODispatcher call its run() method from a different thread later.
        NIODispatcher.instance().invokeLater(new Runnable() { public void run() {

            try {
                
                /*
                 * Before we change it, writer points at the NIOOutputStream object this NIOSocket made.
                 * Call handleWrite() on it to make it send out all its data.
                 * If it wasn't able to send everything out, it will return true.
                 * This is a problem, there is still data stuck in the NIOOutputStream, and we're about to replace it with the given MessageWriter.
                 * If this happens, throw an exception.
                 */

                // Tell our NIOInputStream to send all its data to the remote computer, and make sure it has none left in it
                if (writer.handleWrite()) throw new IllegalStateException("data still in old writer!");
                writer.shutdown(); // Shut down our NIOInputStream, we're done with it

                // Find the ThrottleWriter at the end of the chain
                ChannelWriter lastChannel = newWriter; // Start lastChannel at the starting MessageWriter
                while (lastChannel.getWriteChannel() instanceof ChannelWriter) {

                    // Find the object that lastChannel writes to, and point lastChannel at it instead
                    lastChannel = (ChannelWriter)lastChannel.getWriteChannel();

                    /*
                     * This loop runs 3 times.
                     * At this point, lastChannel points to: (1) DeflaterWriter, (2) DelayedBufferWriter, (3) ThrottleWriter
                     * The last one ThrottleWriter, implements ThrottleListener, so control enters the if statement.
                     * The loop then exits, and lastChannel points to ThrottleWriter.
                     */

                    // lastChannel points to ThrottleWriter
                    if (lastChannel instanceof ThrottleListener) {

                        // Call ThrottleListener.setAttachment(), giving it a reference to this class
                        ((ThrottleListener)lastChannel).setAttachment(NIOSocket.this); // (ask)
                    }
                }

                // Make a new SocketInterestWriteAdapter
                InterestWriteChannel source = new SocketInterestWriteAdapater(channel); // It's the object that will actually write to the Java SelectableChannel
                writer = source;                     // When you write to this NIOSocket object, you'll write to it
                lastChannel.setWriteChannel(source); // When the ThrottleWriter writes, it will write to it also

            } catch (IOException iox) {

                // Shut down this NIOSocket object, and our NIOOutputStream
                shutdown();
                newWriter.shutdown(); // If the exception happened before we did this above, we'll need to do it here
            }
        }});
    }

    /**
     * NIO calls this when our channel is connected.
     * 
     * There is a thread waiting in the connect() method right now.
     * It told NIO to connect the channel, and then called LOCK.wait().
     * It's blocking in connect() until the connection is made.
     * 
     * The connection is made now.
     * Call LOCK.notify() to wake the thread in connect() up.
     * It will return to its caller, having blocked for as long as it took to make the connection.
     */
    public void handleConnect() throws IOException {

        // Make sure only one thread can do this at a time
        synchronized (LOCK) {

            // Wake up the thread in connect() that called LOCK.wait()
            LOCK.notify();
        }
    }

    /**
     * NIODispatcher calls this when we can read from our channel.
     * Passes the call to the NIOInputStream object this NIOSocket made.
     * After ManagedConnection has created the chain of readers, passes the call to the MessageReader at the start of the chain.
     */
    public void handleRead() throws IOException {

        // Calls NIOInputStream.handleRead() or MessageReader.handleRead()
        reader.handleRead();
    }

    /**
     * NIODispatcher calls this when we can write to our channel.
     * Passes the call to the NIOOutputStream object this NIOSocket made.
     * After ManagedConnection has created the chain of writers, passes the call to the MessageWriter at the start of the chain.
     */
    public boolean handleWrite() throws IOException {

        // Calls NIOOutputStream.handleWrite() or MessageWriter.handleWrite()
        return writer.handleWrite();
    }

    /**
     * NIO calls this when an IOException occurred while it was processing a connect, read, or write. (do)
     * Shuts down the socket and all of its streams.
     * Wakes up any threads waiting on locks.
     * 
     * @param iox The IOException that happened
     */
    public void handleIOException(IOException iox) {

        // Make sure only one thread can do this at a time
        synchronized (LOCK) {

            /*
             * Save the given exception in storedException.
             * Call LOCK.notify() to wake up the thread sleeping in the connect() method.
             * It will check out storedException.
             * If there is an exception there, it will call shtudown(), and then throw it.
             */

            // Save the given exception in storedException, and wake up the thread in connect() so it will get it
            storedException = iox;
            LOCK.notify();
        }

        // Shut this NIOSocket object down
        shutdown(); // The shuttingDown boolean will keep the method from running twice
    }

    /**
     * Shut down this socket and all of its streams.
     * Wake up threads waiting on locks.
     * 
     * Closing things and shutting them down can cause IOExceptions.
     * Code here catches them, and does nothing, ignoring them.
     * There's nothing we can do about internal errors.
     * And, we're shutting down anyway, so it doesn't matter if something breaks.
     * 
     * The Shutdownable interface requires this method.
     */
    public void shutdown() {

        // Only let one thread in this section at a time
        synchronized (LOCK) {

            // Make sure this shutdown() method only runs once
            if (shuttingDown) return; // If it's already run, leave now
            shuttingDown = true;      // Mark shuttingDown true so we won't get past here again
        }

        // If the Apache log is on, make a note that we're shutting down
        if (LOG.isDebugEnabled()) LOG.debug("Shutting down socket & streams for: " + this);
        
        /*
         * socket is a java.net.Socket object that we're using with NIO.
         * The method calls below call socket.shutdownInput() and socket.shutdownOutput().
         * This tells Java we won't be reading or writing from the socket anymore.
         * It also ends our ability to do these things.
         */

        // Shut down this object's socket for reading and writing
        try { shutdownInput();  } catch (IOException ignored) {} // Calls socket.shutdownInput();
        try { shutdownOutput(); } catch (IOException ignored) {} // Calls socket.shutdownOutput();
        
        // Tell our NIOInputStream and NIOOutputStream objects to shut down
        reader.shutdown();
        writer.shutdown();

        try {

            // Close the java.net.Socket object
            socket.close();

        // Ignore exceptions
        } catch (IOException ignored) {
        } catch (Error ignored) {
        }
        
        // Close the NIO SocketChannel we got from our socket
        try { channel.close(); } catch (IOException ignored) {}

        // Only let one thread in this section at a time
        synchronized (LOCK) {

            /*
             * Another thread may have called LOCK.wait();
             * That made it block right there.
             * When we call LOCK.notify(), that thread will keep going.
             */

            // Wake up a thread waiting on the lock object
            LOCK.notify();
        }
    }

    /** Not used. */
    public void bind(SocketAddress endpoint) throws IOException {

        // Have the socket do this
        socket.bind(endpoint);
    }

    /**
     * Shut down this socket and all of its streams.
     * Wake up threads waiting on locks.
     */
    public void close() throws IOException {

        // This just calls the shutdown() method here in NIOSocket
        NIODispatcher.instance().shutdown(this);
    }

    /** Only used in test code. */
    public void connect(SocketAddress addr) throws IOException {

        // Connect with no timeout, wait forever if necessary
        connect(addr, 0);
    }

    /**
     * Have NIO connect the SocketChannel to the given address, and wait here until it makes the connection.
     * 
     * This method uses Java NIO to connect to the remote computer.
     * NIO doesn't block, the call channel.connect(addr) below returns immediately, and you can find out later if it worked.
     * But, the part of LimeWire above this method is built to have a thread wait to connect and then wait as we exchange handshake headers.
     * So, this method fakes blocking for the benefit of the rest of the program.
     * It takes a new, robust non-blocking NIO system and converts it to an old-fashioned, blocking one.
     * 
     * A thread is meant to call connect(), get stuck here, and leave once we're connected or a timeout has expired.
     * Later, when we start exchanging packets with the remote computer through this connection, we'll use NIO without this fake blocking.
     * 
     * @param addr    The IP address and port number to try to connect to
     * @param timeout After telling NIO to connect, the thread will wait here for up to 6 seconds
     */
    public void connect(SocketAddress addr, int timeout) throws IOException {

        /*
         * Here are 3 Java types that hold IP address and port number information.
         * SocketAddress and InetSocketAddress are the same.
         * They hold the IP address and port number together.
         * InetAddress just holds the IP address.
         */

        // Keep a record of the IP address and port number we connected to
        connectedTo = ((InetSocketAddress)addr).getAddress();
        
        // Make sur the given InetSocketAddress contains an IP address
        InetSocketAddress iaddr = (InetSocketAddress)addr;
        if (iaddr.isUnresolved()) throw new IOException("unresolved: " + addr);

        // Only let one thread in this section at a time
        synchronized (LOCK) {

            /*
             * This is where LimeWire actually asks Java to connect to an IP address and port number.
             * 
             * Here's how to connect to a remote computer the NIO way.
             * Make a Socket, and then call getChannel() on it to get its associated SocketChannel object.
             * Here, it's named channel.
             * Package the IP address and port number you want to connect to in an InetSocketAddress object.
             * Here, it's named addr.
             * Call channel.connect(addr).
             * 
             * If Java can establish the connection immediately, the connect(addr) method returns true.
             * This usually happens on local connections.
             * If it's going to take a little while, connect(addr) returns false.
             * 
             * This call doesn't block, and returns right away.
             * NIO will tell you later if it was able to make the connection.
             */

            // Have Java try to open a connection to the given IP address and port number
            if (!channel.connect(addr)) {

                /*
                 * channel.connect(addr) returned false.
                 * This means it's going to take a little while for NIO to contact the remote computer.
                 */

                // Tell NIO that we are interested in knowing when this channel has been connected
                NIODispatcher.instance().registerConnect(
                    channel, // The channel that NIO is connecting to the remote computer
                    this);   // NIO will call this object's handleConnect() method when the connection is made

                try {

                    /*
                     * Have this thread wait here for up to 6 seconds.
                     * When NIO calls handleConnect() above, the thread running that method will call LOCK.notify().
                     * When it does that, it wakes up the thread here.
                     */

                    // Wait here until NIO calls handleConnect(), meaning the connection is made
                    LOCK.wait(timeout); // Or 6 seconds pass

                // If another thread called LOCK.interrupt(), the wait() method above will throw an InterruptedException
                } catch (InterruptedException ix) {

                    // Wrap it in an InterruptedIOException and throw it upward
                    throw new InterruptedIOException(ix);
                }

                /*
                 * NIO may have called the handleIOException() method above.
                 * If it did, that method caught the exception it threw us and stored it in storedException.
                 */

                // Move the exception from storedException to x
                IOException x = storedException;
                storedException = null;

                // If NIO did throw us an exception
                if (x != null) {

                    // Shut down this NIOSocket object and all of the objects it made to use, and throw the exception upwards
                    shutdown();
                    throw x;
                }

                // If the socket isn't connected
                if (!isConnected()) { // wait above must have timed out after 6 seconds instead of getting woken up by handleConnect()

                    // Shut down this NIOSocket object and all of the objects it made to use, and throw an exception that explains how long we waited
                    shutdown();
                    throw new SocketTimeoutException("couldn't connect in " + timeout + " milliseconds");
                }
            }

            /*
             * If channel.connect(addr) makes the connection immediately, it returns true.
             * Control reaches here without running the inside of the if block at all.
             */
        }

        // Make a note about the connection we just made in the Apache debugging log
        if (LOG.isTraceEnabled()) LOG.trace("Connected to: " + addr);

        // This NIOSocket object made NIOOutputStream and NIOInputStream objects, call init() on both of them
        if (writer instanceof NIOOutputStream) ((NIOOutputStream)writer).init(); // Have NIOOutputStream make a BufferOutputStream and get an 8 KB buffer
        if (reader instanceof NIOInputStream)  ((NIOInputStream)reader).init();  // Have NIOInputStream make a BufferInputStream and get an 8 KB buffer
    }

    /**
     * The IP address of the remote computer this socket is connected to.
     * 
     * If the remote computer connected to us, we got the IP address from socket.getInetAddress().
     * If we connected to the remote computer, we saved the given IP address before we tried to connect to it.
     */
    public InetAddress getInetAddress() {

        // Return the value we saved
        return connectedTo;
    }

    /**
     * Get the object this NIOSocket made that implements InputStream and you can read from.
     * Calls to read will block, even though the NIO underneath is non-blocking.
     * 
     * @return The BufferInputStream the NIOInputSteam made, which you can call read() on
     */
    public InputStream getInputStream() throws IOException {

        // Make sure the socket is open
        if (isClosed()) throw new IOException("Socket closed.");

        // Return a reference to the BufferInputStream the NIOInputStream made
        if (reader instanceof NIOInputStream) return ((NIOInputStream)reader).getInputStream();
        else throw new IllegalStateException("reader not NIOInputStream!");
    }

    /**
     * Get the object this NIOSocket made that implements OutputStream and you can write to.
     * Calls to write will block, even though the NIO underneath is non-blocking.
     * 
     * @return The BufferOutputStream the NIOOutputStream made, which you can call write(b) on
     */
    public OutputStream getOutputStream() throws IOException {

        // Make sure the socket is open
        if (isClosed()) throw new IOException("Socket closed.");

        // Return a reference to the BufferOutputStream the NIOOutputStream made
        if (writer instanceof NIOOutputStream) return ((NIOOutputStream)writer).getOutputStream();
        else throw new IllegalStateException("writer not NIOOutputStream!");
    }

    /*
     * LimeWire's NIOSocket class extends Socket, so it is a socket.
     * It also has a member object of type Socket named socket, so it contains a socket.
     * So there are 2 different sockets here.
     * 
     * NIOSocket never uses the socket that it is, and only uses the socket that it contains instead.
     * This class extends Socket just to steal its interface.
     * It overrides methods Socket defines and passes them on to the member socket object.
     */

    /**
     * Return the NIO SocketChannel object associated with the Socket this NIOSocket object keeps.
     */
    public SocketChannel getChannel() {

        // Have the socket do this
        return socket.getChannel();
    }

    /**
     * Only test code calls this.
     * Get the local port number the Socket this NIOSocket object keeps is bound to.
     */
    public int getLocalPort() {

        // Have the socket do this
        return socket.getLocalPort();
    }

    /** Not used. */
    public SocketAddress getLocalSocketAddress() {

        // Have the socket do this
        return socket.getLocalSocketAddress();
    }

    /**
     * Use the connection socket in this NIOSocket object to find out what our IP address is.
     * Asks the socket for the local address, the one on the near end.
     * This is still just our LAN address, but it's a little better than calling InetAddress.getLocalHost().
     * 
     * @return Our IP address as an InetAddress object
     */
    public InetAddress getLocalAddress() {

        try {

            // Have the socket do this
            return socket.getLocalAddress();

        } catch (Error osxSucks) {

            /*
             * On Macintosh OS X 10.3 with Java 1.4.2_05,
             * if the connection dies and you call socket.getLocalAddress(),
             * it will throw an exception.
             */

            try {

                // Ask Java what our IP address is, and return that instead
                return InetAddress.getLocalHost(); // Returns our LAN IP address, like 192.168.1.102

            // InetAddress.getLocalHost can sometimes throw an exception, catch it and return null instead
            } catch (UnknownHostException uhe) { return null; }
        }
    }

    /** Not used. */ 
    public boolean getOOBInline() throws SocketException {

        // Have the socket do this
        return socket.getOOBInline();
    }

    /**
     * The remote computer's port number.
     * Calls socket.getPort() to ask the connection socket what port number on on its far end.
     * 
     * @return The port number the socket is connected to
     */
    public int getPort() {

        // Ask the socket this
        return socket.getPort();
    }

    /** Not used. */
    public int getReceiveBufferSize() throws SocketException {

        // Ask the socket this
        return socket.getReceiveBufferSize();
    }

    /** Not used. */
    public boolean getReuseAddress() throws SocketException {

        // Ask the socket this
        return socket.getReuseAddress();
    }

    /** Not used. */
    public int getSendBufferSize() throws SocketException {

        // Ask the socket this
        return socket.getSendBufferSize();
    }

    /** Not used. */
    public int getSoLinger() throws SocketException {

        // Ask the socket this
        return socket.getSoLinger();
    }

    /**
     * Find out how long the socket will wait for something to happen before giving up.
     * 
     * @param The socket timeout, or 0 if the socket will wait forever
     */
    public int getSoTimeout() throws SocketException {

        // Ask the socket this
        return socket.getSoTimeout();
    }

    /** Not used. */
    public boolean getTcpNoDelay() throws SocketException {

        // Ask the socket this
        return socket.getTcpNoDelay();
    }

    /** Not used. */
    public int getTrafficClass() throws SocketException {

        // Ask the socket this
        return socket.getTrafficClass();
    }

    /** Not used. */
    public boolean isBound() {

        // Ask the socket this
        return socket.isBound();
    }

    /**
     * Ask the connection socket in this NIOSocket object if it's closed or not.
     * 
     * @return True if the socket is closed
     */
    public boolean isClosed() {

        // Ask the socket this
        return socket.isClosed();
    }

    /**
     * Ask the connection socket in this NIOSocket object if it's connected to a remote computer or not.
     * 
     * @return True if the socket is connected.
     */
    public boolean isConnected() {

        // Ask the socket this
        return socket.isConnected();
    }

    /** Not used. */
    public boolean isInputShutdown() {

        // Ask the socket this
        return socket.isInputShutdown();
    }

    /** Not used. */
    public boolean isOutputShutdown() {

        // Ask the socket this
        return socket.isOutputShutdown();
    }

    /** Not used. */
    public void sendUrgentData(int data) {

        // Instead of having the socket do this, throw an exception
        throw new UnsupportedOperationException("No urgent data.");
    }

    /**
     * Enable or disable SO_KEEPALIVE on the connection socket this NIOSocket object keeps.
     * 
     * @param b True to enable the SO_KEEPALIVE socket option, false to disable it
     */
    public void setKeepAlive(boolean b) throws SocketException {

        // Have the socket do this
        socket.setKeepAlive(b);
    }

    /** Not used. */
    public void setOOBInline(boolean b) throws SocketException {

        // Have the socket do this
        socket.setOOBInline(b);
    }

    /** Not used. */
    public void setReceiveBufferSize(int size) throws SocketException {

        // Have the socket do this
        socket.setReceiveBufferSize(size);
    }

    /** Not used. */
    public void setReuseAddress(boolean on) throws SocketException {

        // Have the socket do this
        socket.setReuseAddress(on);
    }

    /** Not used. */
    public void setSendBufferSize(int size) throws SocketException {

        // Set the socket this way
        socket.setSendBufferSize(size);
    }

    /** Not used. */
    public void setSoLinger(boolean on, int linger) throws SocketException {

        // Set the socket this way
        socket.setSoLinger(on, linger);
    }

    /**
     * Set the amount of time the socket in this NIOSocket object should wait before giving up.
     * 
     * @param timeout The timeout, in milliseconds
     */
    public void setSoTimeout(int timeout) throws SocketException {

        // Set the socket this way
        socket.setSoTimeout(timeout);
    }

    /** Not used. */
    public void setTcpNoDelay(boolean b) throws SocketException {
        
        // Set the socket this way
        socket.setTcpNoDelay(b);
    }

    /** Not used. */
    public void setTrafficClass(int i) throws SocketException {
        
        // Set the socket this way
        socket.setTrafficClass(i);
    }

    /**
     * Shut down the socket so we can't read from it anymore.
     * 
     * socket is a java.net.Socket object that we're using with NIO.
     * This method calls the shutdownInput() method on our Socket object.
     * 
     * Places the input stream for this socket at the end of the stream.
     * Any data sent to the input stream side of the socket is acknowledged and then silently discarded.
     * If you read from a socket input stream after invoking shutdownInput() on the socket, the stream will return EOF. 
     */
    public void shutdownInput() throws IOException {

        // Shut down the socket for reading
        socket.shutdownInput();
    }

    /**
     * Shut down the socket so we can't write to it anymore.
     * 
     * socket is a java.net.Socket object that we're using with NIO.
     * This method calls the shutdownOutput() method on our Socket object.
     * 
     * Disables the output stream for this socket.
     * 
     * This is a TCP socket.
     * Java will actually send the data we've previously written to the socket, and then do the TCP connection termination sequence.
     * If you write to a socket output stream after invoking shutdownOutput() on the socket, the stream will throw an IOException.
     */
    public void shutdownOutput() throws IOException {

        // Shut down the socket for writing
        socket.shutdownOutput();
    }

    /**
     * Get information about this NIOSocket object as text in a string.
     * 
     * @return Text like (do)
     */
    public String toString() {

        // Compose text like (do)
        return "NIOSocket::" + channel.toString();
    }
}

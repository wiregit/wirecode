package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Properties;
import com.sun.java.util.collections.*;

/**
 * A simple subclass of Connection that provides old-style blocking send and
 * receive methods.  Useful for test classes.  Do NOT call
 * initialize(ConnectionListener), read(), or write() on this class.  Not
 * thread-safe.  
 */
public class SimpleConnection extends Connection {
    /** Messages and errors received.  Add to tail, remove from head. */
    private LinkedList /* Message or BadPacketException */ _received
        =new LinkedList();
    /** Received IOException reading or writing? */
    private boolean _error;


    /////////////////////// Initializing ////////////////////////////

    public SimpleConnection(String host, int port) {
        this(host, port, null, null, false);
    }

    public SimpleConnection(String host, int port,
                      Properties properties1,
                      HandshakeResponder properties2,
                      boolean negotiate) {
        super(host, port, properties1, properties2, negotiate);
    }
    
    public SimpleConnection(Socket socket) {
        this(socket, null);
    }

    public SimpleConnection(Socket socket, HandshakeResponder properties) {
        super(socket, properties);
    }

    public void initialize()  
            throws IOException, NoGnutellaOkException, BadHandshakeException {
        super.initialize(new SimpleConnectionListener());
    }


    //////////////////////// Reading and Writing ////////////////////////

    /**
     * Sends m blocking.  Does not return until m is sent.  Not thread-safe.
     * @exception IOException connection is closed
     */
    public void send(Message m) throws IOException {
        if (_error)
            throw new IOException();

        Assert.that(! hasQueued(), "Queue not empty");
        write(m);
        if (_error)
            throw new IOException();

        while (hasQueued()) {
            write();
            if (_error)
                throw new IOException();
        }
    }
    
    /**
     * Returns the next message with timeout.  Blocks until a message is
     * available or the given milliseconds have elapsed, whichever is first.  If
     * timeout occurs, throws InterruptedIOException; at this point you should
     * close the connection without reading more data.  INEFFICIENT; FOR 
     * TESTING PURPOSES ONLY.
     * 
     * @param timeout the maximum time to block in milliseconds, or 0 for 
     *  no timeout
     * @exception BadPacketException received a bad packet but input stream
     *  is recoverable
     * @exception IOException connection is closed 
     */
    public Message receive(int timeout) 
            throws IOException, BadPacketException {        
        //Can't simply set SO_TIMEOUT since connection uses
        //SocketChannel.read(buffer), which ignores this.
        Selector selector=Selector.open();
        SocketChannel channel=(SocketChannel)channel();
        try {            
            channel.configureBlocking(false);
            SelectionKey key=channel.register(selector, SelectionKey.OP_READ);
            selector.select(timeout);
            if (key.isReadable()) {
                //This is not guaranteed to return without blocking, e.g., if
                //only half a packet has been sent.  But it's probably good
                //enough for testing.
                return receive();
            }
        } catch (CancelledKeyException e) { 
            throw new IOException();
        } finally {
            selector.close();
            channel.configureBlocking(true);
        }
        throw new InterruptedIOException();
    }


    /**
     * Returns the next message.  Does not return until a message is available
     * or an error is encountered.
     * @exception BadPacketException received a bad packet but input stream
     *  is recoverable
     * @exception IOException connection is closed
     */    
    public Message receive() throws IOException, BadPacketException {
        while (true) {
            if (! _received.isEmpty()) {
                //Return data from a previous read, including bad packets.  Note
                //that this happens BEFORE checking _error.
                Object ret=_received.removeFirst();
                if (ret instanceof Message)
                    return (Message)ret;
                else if (ret instanceof BadPacketException)
                    throw (BadPacketException)ret;
                else
                    Assert.that(false, "Unexpected queue entry: "+ret);
            } else if (_error) {
                //If closed, throw IOException.
                throw new IOException();
            }
            read();
        } 
    }

    /** Does nothing; exists solely for backwards compatibility. */
    public void flush() throws IOException {        
    }

    /** Records reads and errors from superclass. */
    class SimpleConnectionListener implements ConnectionListener {
        public void initialized(Connection c) {
            try {
                channel().configureBlocking(true);
            } catch (IOException e) {
                System.err.println("Couldn't make channel non-blocking!");
                _error=true;
            }
        }
        
        public void read(Connection c, Message m) { 
            _received.addLast(m);
        }
        
        public void read(Connection c, BadPacketException error) { 
            _received.addLast(error);
        }
        
        public void needsWrite(Connection c) { 
        }
        
        public void error(Connection c) { 
            _error=true;
        }
    }
}

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

/**
 * A Gnutella connection. A connection is either INCOMING or OUTGOING;
 * the difference doesn't really matter. It is also in one of three
 * states: UNCONNECTED, CONNECTING, or CONNECTED.<p>
 *
 * This class provides the core logic for handling the Gnutella
 * protocol.  This includes sending replies to requests but not
 * details of routing.  The Message class (and subclasses)
 * actually does the tedious reading/writing of bytes from socket.<p>
 *
 * The class can be used in a number of ways.  The typical use, to
 * handle a normal Gnutella connection, involves creating a
 * ConnectionManager and a thread.<p>
 *
 * <pre>
 *   //1. Setup manager and connection.
 *   ConnectionManager cm=new ConnectionManager();
 *   Connection c=new Connection(host, port);
 *   c.connect();                  //actually connect
 *
 *   //2. Send initial ping request.  The second line is a complete hack
 *   //   hack so that we know responses are for this.
 *   PingRequest pr=new PingRequest(SettingsManager.instance().getTTL()); //Send initial ping.
 *   cm.fromMe(pr);
 *   c.send(pr);
 *
 *   //3. Handle everything else asynchronously
 *   cm.add(c);                    //connnections in cm can broadcast via c
 *   c.setManager(cm);             //c can broadcast via cm
 *   Thread t=new Thread(c);       //create new handler thread
 *   t.start();                    //services connection by calling run()
 * </pre>
 *
 * The second use is for "do it yourselfers".  This is useful for
 * Gnutella spiders.  In this case, you don't set a ConnectionManager
 * and don't call the run() method.<p>
 *
 * You will note that the first constructor doesn't actually connect
 * this.  For that you must call connect().  While this is awkward,
 * it is intentional, as it makes interfacing with the GUI easier.<p>
 *
 * All connections have two underlying spam filters: a personal filter
 * (controls what I see) and a route filter (also controls what I pass
 * along to others).  See SpamFilter for a description.  These
 * filters are configured by the properties in the SettingsManager, but
 * you can change them with setPersonalFilter and setRouteFilter.
 */
public class Connection {
    /*  For thread synchronization reasons, it is important that this
     *  only be modified by the send(m) and receive() methods.  Also,
     *  only use in and out, buffered versions of the input and output
     *  streams, for writing.
     */
    private String _host;
    private int _port;
    private Socket _socket;
    private InputStream _in;
    private OutputStream _out;
    private boolean _outgoing;

    private ConnectionListener _listener;

    /**
     * Trigger an opening connection to shutdown after it opens.  This
     * flag is set in shutdown() and then checked in the constructor
     * to insure the _socket.close() happens if shutdown is called
     * asynchronously before the constructor completes.
     */
    private boolean _shutdownCalled;

    /**
     * The number of packets I sent and received.  This includes bad
     * packets.  These are synchronized by out and in, respectively.
     *
     * Dropped is the number of packets I read (<read) and dropped because the
     * host made one of the following errors: sent replies to requests
     * I didn't make, sent bad packets, or sent (route) spam.  It does
     * not include: TTL's of zero, duplicate requests (it's not their
     * fault), or buffer overflows in sendToAll.
     *
     * lastReceived and lastDropped are the values of received and
     * dropped at the last call to getPercentDropped.
     */
    private int _sent=0;
    private int _received=0;

    /**
     * A ConnectionListener that does nothing.  Used when no connection
     * listening is desired.
     */
    private static final class NullConnectionListener
        implements ConnectionListener {
        private static NullConnectionListener _sInstance;
        public static NullConnectionListener instance() {
            // No synchronization necessary here.  It's okay if two
            // instances are created.
            if(_sInstance == null)
                _sInstance = new NullConnectionListener();
            return _sInstance;
        }

        private NullConnectionListener() {}

        public void connectionInitializing(Connection c) {}
        public void connectionInitialized(Connection c) {}
        public void connectionClosed(Connection c) {}
    }

    /**
     * A dummy constructor for ConnectionManager.ME_CONNECTION
     */
    public Connection() {}

    /**
     * Creates an outgoing connection with no listener.
     */
    public Connection(String host, int port) throws IOException {
        this(host, port, NullConnectionListener.instance());
    }

    /**
     * Creates an outgoing connection with the specified listener.
     */
    public Connection(String host, int port, ConnectionListener listener)
            throws IOException {
        _listener = listener;

        _host = host;
        _port = port;
        _outgoing = true;

        _listener.connectionInitializing(this);

        try {
            _socket = new Socket(host, port);
        } catch(IOException e) {
            _listener.connectionClosed(this);
            throw e;
        }

        if (_shutdownCalled) {
            _socket.close();
            throw new IOException();
        }

        try {
            _in = new BufferedInputStream(_socket.getInputStream());
            _out = new BufferedOutputStream(_socket.getOutputStream());

            SettingsManager settings=SettingsManager.instance();
            sendString(settings.getConnectString()+"\n\n");
            expectString(settings.getConnectOkString()+"\n\n");

            _listener.connectionInitialized(this);
        } catch(IOException e) {
            _socket.close();
            _listener.connectionClosed(this);
            throw e;
        }

        if (_shutdownCalled) {
            _socket.close();
            throw new IOException();
        }
    }

    /**
     * Creates an incoming connection with no listener.
     */
    public Connection(Socket socket) throws IOException {
        this(socket, NullConnectionListener.instance());
    }

    /**
     * Creates an incoming connection with the specified listener.
     * @requires the word "GNUTELLA " and nothing else has just been read
     *  from socket
     * @effects wraps a connection around socket and does the rest of the Gnutella
     *  handshake.  Throws IOException if the connection couldn't be established.
     *  If such an error happens, the socket is properly closed.
     */
    public Connection(Socket socket, ConnectionListener listener)
            throws IOException {
        _listener = listener;

        _host = socket.getInetAddress().toString();
        _port = socket.getPort();
        _outgoing = false;

        _listener.connectionInitializing(this);

        try {
            _socket=socket;
            _in=new BufferedInputStream(_socket.getInputStream());
            _out=new BufferedOutputStream(_socket.getOutputStream());

            //Handshake
            SettingsManager settings=SettingsManager.instance();
            expectString(settings.getConnectStringRemainder()+"\n\n");
            sendString(settings.getConnectOkString()+"\n\n");

            _listener.connectionInitialized(this);
        } catch(IOException e) {
            _socket.close();

            _listener.connectionClosed(this);
            throw e;
        }

        if (_shutdownCalled) {
            _socket.close();
            throw new IOException();
        }
    }

    /**
     * Called only from the constructors.
     * @requires _socket is properly set up
     */
    private void sendString(String s) throws IOException {
        //TODO1: timeout.
        byte[] bytes=s.getBytes();
        OutputStream out=_socket.getOutputStream();
        out.write(bytes);
        out.flush();
    }

    /**
     * Called only from the constructors
     *
     * @requires _socket is properly set up
     * @modifies network
     * @effects attempts to read s.size() characters from the network/
     *  If they do not match s, throws IOException.  If the characters
     *  cannot be read within TIMEOUT milliseconds (as defined by the
     *  property manager), throws IOException.
     */
    private void expectString(String s) throws IOException {
        int oldTimeout=_socket.getSoTimeout();
        try {
            _socket.setSoTimeout(SettingsManager.instance().getTimeout());
            byte[] bytes=s.getBytes();
            for (int i=0; i<bytes.length; i++) {
                int got=_in.read();
                if (got==-1)
                    throw new IOException();
                if (bytes[i]!=(byte)got)
                    throw new IOException();
            }
        } finally {
            //Restore socket timeout.
            _socket.setSoTimeout(oldTimeout);
        }
    }

    /**
     * Used to determine whether the connection is incoming or outgoing.
     */
    public boolean isOutgoing() {
        return _outgoing;
    }

    /**
     * Sends a message.
     *
     * @requires this is fully constructed
     * @modifies the network underlying this
     * @effects send m on the network.  Throws IOException if problems
     *   arise.  This is thread-safe.
     */
    public void send(Message m) throws IOException {
        //Can't use same lock as receive()!
        synchronized (_out) {
            m.write(_out);
            _out.flush();
            _sent++;
        }
    }

    /**
     * Receives a message.
     *
     * @requires this is fully constructed
     * @effects exactly like Message.read(), but blocks until a
     *  message is available.  A half-completed message
     *  results in InterruptedIOException.
     */
    public Message receive() throws IOException, BadPacketException {
        //Can't use same lock as send()!
        synchronized(_in) {
            while (true) {
                Message m=Message.read(_in);
                if (m==null)
                    continue;
                _received++;
                return m;
            }
        }
    }

    /**
     * Receives a message with timeout.
     *
     * @requires this is in the CONNECTED state
     * @effects exactly like Message.read(), but throws InterruptedIOException if
     *  timeout!=0 and no message is read after "timeout" milliseconds.  In this
     *  case, you should terminate the connection, as half a message may have been
     *  read.
     */
    public Message receive(int timeout)
        throws IOException, BadPacketException, InterruptedIOException {
        synchronized (_in) {
            //temporarily change socket timeout.
            int oldTimeout=_socket.getSoTimeout();
            _socket.setSoTimeout(timeout);
            try {
                Message m=Message.read(_in);
                if (m==null)
                    throw new InterruptedIOException();
                _received++;
                return m;
            } finally {
                _socket.setSoTimeout(oldTimeout);
            }
        }
    }

    /** Returns the host set at construction */
    public String getOrigHost() {
        return _host;
    }

    /** Returns the port set at construction */
    public int getOrigPort() {
        return _port;
    }

    /** Returns the port of the foreign host this is connected to. */
    public int getPort() {
        return _socket.getPort();
    }

    /** Returns the port this is connected to locally. */
    public int getLocalPort() {
        return _socket.getLocalPort();
    }

    /** Returns the address of the foreign host this is connected to. */
    public InetAddress getInetAddress() {
        return _socket.getInetAddress();
    }

    /** Returns the local address of this. */
    public InetAddress getLocalAddress() {
        return _socket.getLocalAddress();
    }

    /** Returns the number of messages sent on this connection */
    public int getNumMessagesSent() {
        return _sent;
    }

    /** Returns the number of messages received on this connection */
    public int getNumMessagesReceived() {
        return _received;
    }

    /**
     *  Shutdown the Connection's socket and thus the connection itself.
     */
    public void shutdown() {
        // Setting this flag insures that the socket is closed if this
        // method is called asynchronously before the socket is initialized.
        _listener.connectionClosed(this);
        _shutdownCalled = true;
        if(_socket != null) {
            try {
                _socket.close();
            } catch(IOException e) {}
        }
    }
}

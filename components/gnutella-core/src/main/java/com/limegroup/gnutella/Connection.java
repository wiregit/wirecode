package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.Buffer;

/**
 * A Gnutella connection. A connection is either INCOMING or OUTGOING.
 *
 * This class provides the core logic for handling the Gnutella
 * protocol.  This includes sending replies to requests but not
 * details of routing.  The Message class (and subclasses)
 * actually does the tedious reading/writing of bytes from socket.<p>
 *
 * The class can be used in a number of ways.  The typical use, to
 * handle a normal Gnutella connection, involves creating a
 * ConnectionManager.<p>
 *
 * <pre>
 *   //1. Setup manager and connection.
 *   ConnectionManager cm=new ConnectionManager();
 *   cm.createConnectionAsynchronously(host, port);
 * </pre>
 *
 * or, if you need to use the Connection immediately:
 *
 * <pre>
 *   //1. Setup manager and connection.
 *   ConnectionManager cm=new ConnectionManager();
 *   Connection c = cm.createConnectionBlocking(host, port);
 *   c.send(whatever);
 * </pre>
 *
 * The third use is for "do it yourselfers".  This is useful for
 * Gnutella spiders. This goes something like this:<p>
 *
 * <pre>
 *   Connection c = new Connection(host, port);
 *   c.initialize();
 *   c.send(whatever);
 * </pre>
 *
 * You will note that the constructors don't actually connect
 * this.  For that you must call initialize().  While this is awkward,
 * it is intentional, as it makes dealing with connection failures easier
 * The constuctor doesn't throw an exception, and the constructor does not
 * run for an unreasonable amount of time (i.e., the constructor does not
 * use the network).  Often, the connection is initialized on a new
 * thread for this reason. <p>
 *
 * All connections have two underlying spam filters: a personal filter
 * (controls what I see) and a route filter (also controls what I pass
 * along to others).  See SpamFilter for a description.  These
 * filters are configured by the properties in the SettingsManager, but
 * you can change them with setPersonalFilter and setRouteFilter.
 */
public class Connection {
    /** The underlying socket, its address, and input and output
     *  streams.  sock, in, and out are null iff this is in the
     *  unconnected state.  For thread synchronization reasons, it is
     *  important that this only be modified by the send(m) and
     *  receive() methods.
     *
     *  This implementation has two goals:
     *    1) a slow connection cannot prevent other connections from making
     *       progress.  Packets must be dropped.
     *    2) packets should be sent in large batches to the OS, but the
     *       batches should not be so long as to cause undue latency.
     *
     *  Towards this end, we queue sent messages on the front of
     *  outputQueue.  Whenever outputQueue contains at least
     *  BATCH_SIZE messages or QUEUE_TIME milliseconds has passed, the
     *  messages on outputQueue are written to out.  Out is then
     *  flushed exactly once. outputQueue is fixed size, so if the
     *  output thread can't keep up with the producer, packets will be
     *  (intentionally) droppped.  LOCKING: obtain outputQueueLock
     *  lock before modifying or replacing outputQueue.
     *
     *  One problem with this scheme is that IOExceptions from sending
     *  data happen asynchronously.  When this happens, _connectionClosed
     *  is set to true.  Then the next time send is called, an IOException
     *  is thrown.  */
    private String _host;
    private int _port;
    private Socket _socket;
    private InputStream _in;
    private OutputStream _out;
    private boolean _outgoing;

    /** The (approximate) max time a packet can be queued, in milliseconds. */
    private static final int QUEUE_TIME=750;
    /** The number of packets to present to the OS at a time.  This
     *  should be roughly proportional to the OS's send buffer. */
    private static final int BATCH_SIZE=50;
    /** The size of the queue.  This must be larger than BATCH_SIZE.
     *  Larger values tolerate temporary bursts of producer traffic
     *  without traffic but may result in overall latency. */
    private static final int QUEUE_SIZE=500;
    /** A lock to protect the swapping of outputQueue and oldOutputQueue. */
    private Object _outputQueueLock=new Object();
    /** The producer's queue. */
    private volatile Buffer _outputQueue=new Buffer(QUEUE_SIZE);
    /** The consumer's queue. */
    private volatile Buffer _oldOutputQueue=new Buffer(QUEUE_SIZE);
    /** True iff the output thread should write the queue immediately.
     *  Synchronized by _outputQueueLock. */
    private boolean _flushImmediately=false;

    /**
     * Trigger an opening connection to close after it opens.  This
     * flag is set in shutdown() and then checked in initialize()
     * to insure the _socket.close() happens if shutdown is called
     * asynchronously before initialize() completes.
     */
    private boolean _closed;

    /**
     * The number of packets I sent and received.  This includes bad
     * packets.  These are synchronized by out and in, respectively.
     *
     * sentDropped is the number of packets I dropped because the
     * output queue overflowed.  This happens when the remote host
     * cannot receive packets as quickly as I am trying to send them.
     * No synchronization is necessary.
     */
    private int _sent=0;
    private int _sentDropped=0;
    private int _received=0;

    /**
     * A dummy constructor for ConnectionManager.ME_CONNECTION
     */
    public Connection() {}

    /**
     * Creates an outgoing connection with the specified listener.
     * initalize() must be called before anything else.
     */
    public Connection(String host, int port) {
        _host = host;
        _port = port;
        _outgoing = true;
    }

    /**
     * Creates an incoming connection.
     * initalize() must be called before anything else.
     *
     * @requires the word "GNUTELLA " and nothing else has just been read
     *  from socket
     * @effects wraps a connection around socket and does the rest of the Gnutella
     *  handshake.  Throws IOException if the connection couldn't be established.
     *  If such an error happens, the socket is properly closed.
     */
    public Connection(Socket socket) {
        _host = socket.getInetAddress().toString();
        _port = socket.getPort();
        _outgoing = false;
    }

    /**
     * Initialize the connection by doing the handshake.  Subclasses of
     * connection should override this method and call super.initialize()
     * in the first line of the override.
     */
    public void initialize() throws IOException {
        SettingsManager settingsManager = SettingsManager.instance();
        String expectString;

        if(isOutgoing()) {
            _socket = new Socket(_host, _port);
            expectString = settingsManager.getConnectString();
        } else {
            // "GNUTELLA" was already read off the socket
            expectString = settingsManager.getConnectStringRemainder();
        }

        if (_closed) {
            _socket.close();
            throw new IOException();
        }

        try {
            _in = new BufferedInputStream(_socket.getInputStream());
            _out = new BufferedOutputStream(_socket.getOutputStream());

            sendString(expectString+"\n\n");
            expectString(settingsManager.getConnectOkString()+"\n\n");
        } catch(IOException e) {
            _socket.close();
            throw e;
        }
    }

    /**
     * Called only from initialize()
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

    private static boolean disposeable(Message m) {
        return  ((m instanceof PingRequest) && (m.getHops()!=0))
          || (m instanceof PingReply);
    }

    /**
     * Sends a message.
     *
     * @requires this is fully constructed
     * @modifies the network underlying this
     * @effects send m on the network.  Throws IOException if problems
     *   arise.  This is thread-safe and guaranteed not to block.
     */
    public void send(Message m) throws IOException {
        if (_closed)
            throw new IOException();

        synchronized (_outputQueueLock) {
            _sent++;
            if (_outputQueue.isFull()) {
                //Drop case. Instead of using a FIFO replacement scheme, we
                //use the following:
                //  1) Throw away m if it is (a) a ping request
                //     whose hops count is not zero or (b) a pong.
                //  2) If that doesn't work, throw away the oldest message
                //     message meeting above criteria.
                //  3) If that doesn't work, throw away the oldest message.
                _sentDropped++;
                if (disposeable(m))
                    return;

                //It's possible to optimize this by keeping track of the
                //last value of i, but case (1) occurs more frequently.
                int i;
                for (i=_outputQueue.getSize()-1; i>=0; i--) {
                    Message mi=(Message)_outputQueue.get(i);
                    if (disposeable(mi))
                        break;
                }

                if (i>=0)
                    _outputQueue.set(i,m);
                else
                    _outputQueue.addFirst(m);
            } else {
                //Normal case.
                _outputQueue.addFirst(m);
                if (_outputQueue.getSize() >= BATCH_SIZE)
                    _outputQueueLock.notify();
            }
        }
    }

    /** Ensures that any data queued in this is written to output as
     *  soon as possible.  Normally, there is no need to call this
     *  method; the output buffers are automatically flushed every few
     *  seconds (at most).  However, it may be necessary to call this
     *  method in situations where latencies are not tolerable, e.g.,
     *  in the network discoverer. */
    public void flush() {
        synchronized (_outputQueueLock) {
            _flushImmediately=true;
            _outputQueueLock.notify();
        }
    }

    /** Repeatedly sends all the queued data every few seconds. */
    private class OutputRunner implements Runnable {
        public void run() {
            while (!_closed) {
                //1. Wait until (1) the queue is full or (2) the
                //maximum allowable send latency has passed (and the
                //queue is not empty)...
                synchronized (_outputQueueLock) {
                    try {
                        if (!_flushImmediately &&
                                _outputQueue.getSize() < BATCH_SIZE)
                            _outputQueueLock.wait(QUEUE_TIME);
                    } catch (InterruptedException e) {
                    }
                    _flushImmediately=false;
                    if (_outputQueue.isEmpty())
                        continue;
                    //...and swap _outputQueue and _oldOutputQueue.
                    Buffer tmp=_outputQueue;
                    _outputQueue=_oldOutputQueue;
                    _outputQueue.clear();
                    _oldOutputQueue=tmp;
                }

                //2. Now send all the data on the old queue.
                //No need for any locks here since there is only one
                //OutputRunner thread.
                while (! _oldOutputQueue.isEmpty()) {
                    Message m=(Message)_oldOutputQueue.removeLast();
                    try {
                        m.write(_out);
                    } catch (IOException e) {
                        //An IO error while sending will be detected next
                        //time send() is called.
                        close();
                        break;
                    }
                }

                //3. Flush.  IOException can also happen here.  Treat as above.
                try {
                    _out.flush();
                } catch (IOException e) {
                    close();
                }
            }
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
            Message m = null;
            while (m == null) {
                m=Message.read(_in);
            }
            _received++;
            return m;
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

    /**
     * Returns the port of the foreign host this is connected to.
     * @requires this is initialized.
     */
    public int getPort() {
        return _socket.getPort();
    }

    /**
     * Returns the port this is connected to locally.
     * @requires this is initialized.
     */
    public int getLocalPort() {
        return _socket.getLocalPort();
    }

    /**
     * Returns the address of the foreign host this is connected to.
     * @requires this is initialized.
     */
    public InetAddress getInetAddress() {
        return _socket.getInetAddress();
    }

    /**
     * Returns the local address of this.
     * @requires this is initialized.
     */
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

    /** Returns the number of messages I dropped while trying to send
     *  on this connection.  This happens when the remote host cannot
     *  keep up with me. */
    public int getNumSentMessagesDropped() {
        return _sentDropped;
    }

    /**
     *  Closes the Connection's socket and thus the connection itself.
     */
    public void close() {
        // Setting this flag insures that the socket is closed if this
        // method is called asynchronously before the socket is initialized.
        _closed = true;
        if(_socket != null) {
            try {
                _socket.close();
            } catch(IOException e) {}
        }
    }

    ///** Unit test */
    /*
    public static void main(String args[]) {
        //1. Test replacement policies.
        Message qr=new QueryRequest((byte)5, 0, "test");
        Message qr2=new QueryRequest((byte)5, 0, "test2");
        Message preq=new PingRequest((byte)5); preq.hop(); //from other
        Message preq2=new PingRequest((byte)5);            //from me
        Message prep=new PingReply(new byte[16], (byte)5, 6346,
                                   new byte[4], 0, 0);

        Connection c=new Connection("localhost", 6346);
        try {
            //   a') Regression test
            c.send(qr);
            c.send(qr2);
            Assert.that(c._outputQueue.get(0)==qr2);
            Assert.that(c._outputQueue.get(1)==qr);

            for (int i=0; i<QUEUE_SIZE-2; i++) {
                Assert.that(! c._outputQueue.isFull());
                c.send(qr);
            }
            Assert.that(c._outputQueue.isFull());

            //   a) No pings or pongs.  Boot oldest.
            c.send(preq2);
            Assert.that(c._outputQueue.isFull());
            Assert.that(c._outputQueue.get(0)==preq2);

            //   b) Old ping request in last position
            c._outputQueue.set(QUEUE_SIZE-1, preq);
            c.send(preq2);
            Assert.that(c._outputQueue.get(QUEUE_SIZE-1)==preq2);

            //   c) Old ping reply in second to last position.
            c._outputQueue.set(QUEUE_SIZE-2, prep);
            c.send(qr);
            Assert.that(c._outputQueue.get(QUEUE_SIZE-1)==preq2);
            Assert.that(c._outputQueue.get(QUEUE_SIZE-2)==qr);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.that(false, "IOException");
        }
    }
    */
}

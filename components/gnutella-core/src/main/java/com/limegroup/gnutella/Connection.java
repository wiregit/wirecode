package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;
import java.util.Properties;
import java.util.Enumeration;

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
 *   ConnectionManager cm=new ConnectionManager(...);
 *   cm.createConnectionAsynchronously(host, port);
 * </pre>
 *
 * or, if you need to use the Connection immediately:
 *
 * <pre>
 *   //1. Setup manager and connection.
 *   ConnectionManager cm=new ConnectionManager(...);
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
 * you can change them with setPersonalFilter and setRouteFilter.<p>
 *
 * Connections support the 0.4 and 1.0 handshakses.  Connections try to connect
 * at the highest protocol level possible, but they also try to avoid
 * incompatibility.  The only time there is a tension is when attempting to
 * establish outgoing 1.0 connections; you don't know whether they'll accept
 * your headers.  Gnutella 1.0 connections have a list of properties read and
 * written during the handshake sequence.  Typical property/value pairs might be
 * "Query-Routing: 0.3" or "Pong-Caching: 0.1".  
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

    /**
     * Trigger an opening connection to close after it opens.  This
     * flag is set in shutdown() and then checked in initialize()
     * to insure the _socket.close() happens if shutdown is called
     * asynchronously before initialize() completes.  Note that the 
     * connection may have been remotely closed even if _closed==true.
     */
    private volatile boolean _closed=false;

    /** The properties read from the connection, or null if Gnutella 0.4.  */
    private Properties _propertiesRead;
    /** The properties wrote to the connection, or null if Gnutella 0.4.  */
    private Properties _propertiesWritten;
    public static final String GNUTELLA_CONNECT_04="GNUTELLA CONNECT/0.4";
    public static final String GNUTELLA_OK_04="GNUTELLA OK";
    public static final String GNUTELLA_CONNECT_10="GNUTELLA CONNECT/1.0";
    public static final String GNUTELLA_OK_10="GNUTELLA/1.0 200 OK";
    /** End of line for Gnutella 1.0 */
    public static final String CRLF="\r\n";
    /** End of line for Gnutella 0.4 */
    public static final String LF="\n";
    

    /**
     * Creates an outgoing Gnutella 0.4 connection with no extra properties.
     * initalize() must be called before anything else.
     */
    public Connection(String host, int port) {
        this(host, port, null);
    }


    /**
     * Creates an outgoing Gnutella 1.0 connection with the specified outgoing
     * properties.  initialize() must be called before anything else.
     */
    public Connection(String host, int port, Properties properties) {
        _host = host;
        _port = port;
        _outgoing = true;
        _propertiesWritten=properties;
    }
    
    /**
     * Creates an incoming Gnutella connection with no extra properties.
     * initalize() must be called before anything else.  Connects at the 
     * same protocol level as the incoming connection.
     *
     * @requires the word "GNUTELLA " and nothing else has just been read
     *  from socket
     */
    public Connection(Socket socket) {
        this(socket, null);
    }

    /**
     * Creates an incoming Gnutella connection with the specified outgoing
     * properties.  initalize() must be called before anything else.  
     * Connects at the same protocol level as the incoming connection.  Hence
     * the properties are only written if supported remotely.
     *
     * @requires the word "GNUTELLA " and nothing else has just been read
     *  from socket 
     */
    public Connection(Socket socket, Properties properties) {
        _host = socket.getInetAddress().toString();
        _port = socket.getPort();
        _socket = socket;
        _outgoing = false;
        _propertiesWritten=properties;
    }

    /**
     * Initialize the connection by doing the handshake.  Subclasses of
     * connection should override this method and call super.initialize()
     * in the first line of the override.
     */
    public void initialize() throws IOException {
        SettingsManager settingsManager = SettingsManager.instance();
        String expectString;

        if(isOutgoing())
            _socket = new Socket(_host, _port);

        // Check to see if close() was called while the socket was initializing
        if (_closed) {
            _socket.close();
            throw new IOException();
        }

        try {
            // Set the Acceptors IP address
            Acceptor.setAddress( _socket.getLocalAddress().getAddress() );
            
            _in = new BufferedInputStream(_socket.getInputStream());
            _out = new BufferedOutputStream(_socket.getOutputStream());
            if (_in==null || _out==null) throw new IOException();
        } catch (Exception e) {
            //Apparently Socket.getInput/OutputStream throws
            //NullPointerException if the socket is closed.  (See Sun bug
            //4091706.)  Unfortunately the socket may have been closed after the
            //the check above, e.g., if the user pressed disconnect.  So we
            //catch NullPointerException here--and any other weird possible
            //exceptions.  An alternative is to obtain a lock before doing these
            //calls, but we are afraid that getInput/OutputStream may be a
            //blocking operation.  Just to be safe, we also check that in/out
            //are not null.
            throw new IOException();
        }

        try {
            //In all the line reading code below, we are somewhat lax in
            //distinguishing between '\r' and '\n'.  Who cares?
            if(isOutgoing()) {
                //On outgoing connections, ALWAYS try Gnutella 1.0 if requested
                //by the user.  If the other end doesn't understand it--too bad!
                //It might be nice to provide an option for automatic reconnect
                //at lower protocol version.
                if (_propertiesWritten==null)
                    sendString(GNUTELLA_CONNECT_04+LF+LF);
                else {
                    sendString(GNUTELLA_CONNECT_10+CRLF);
                    sendHeaders();                
                }
                String line=readLine();
                if (line.equals(GNUTELLA_OK_04))
                    readLine();
                else if (line.equals(GNUTELLA_OK_10))
                    readHeaders();
                else
                    throw new IOException("Bad connect string");                
            } else {
                //Remember that "GNUTELLA " has already been read by Acceptor.
                //Hence we are looking for "CONNECT/0.4" or "CONNECT/1.0".  As a
                //dirty hack, we use String.endsWith.  This means we will
                //accidentally allow crazy things like "0.4".  Oh well!
                String line=readLine();  
                if (GNUTELLA_CONNECT_04.endsWith(line)) {
                    readLine();
                    sendString(GNUTELLA_OK_04+LF+LF);
                    //If the user requested properties, we can't send them.
                    _propertiesWritten=null;
                } else if (GNUTELLA_CONNECT_10.endsWith(line)) {
                    readHeaders();
                    sendString(GNUTELLA_OK_10+CRLF);
                    if (_propertiesWritten==null)
                        _propertiesWritten=new Properties();
                    sendHeaders();
                } else {
                    throw new IOException("Unexpected connect string");
                }
            }
        } catch(IOException e) {
            _socket.close();
            throw e;
        }
    }

    /**
     * Writes the properties in _propertiesWritten to network, throwing
     * IOException if there are any problems.
     *    @modifies network
     */
    private void sendHeaders() throws IOException {
        Enumeration enum=_propertiesWritten.propertyNames();
        while (enum.hasMoreElements()) {
            String key=(String)enum.nextElement();
            String value=_propertiesWritten.getProperty(key);
            if (value==null)
                value="";
            sendString(key+": "+value+CRLF);            
        }
        sendString(CRLF);
    }


    /**
     * Allocates _propertiesRead and reads the properties from the network into
     * _propertiesRead, throwing IOException if there are any problems.
     *     @modifies network 
     */
    private void readHeaders() throws IOException {
        _propertiesRead=new Properties();
        //TODO: limit number of headers read
        while (true) {
            //This doesn't distinguish between \r and \n.  That's fine.
            String line=readLine();
            if (line==null)
                throw new IOException();   //unexpected EOF
            if (line.equals(""))
                return;                    //blank line ==> done
            int i=line.indexOf(':');
            if (i<0)
                continue;                  //ignore lines without ':'
            String key=line.substring(0, i);
            String value=line.substring(i+1).trim();
            _propertiesRead.setProperty(key, value);
        }
    }

    /**
     * Writes s to out, with no trailing linefeeds.  Called only from
     * initialize().  
     *    @requires _socket, _out are properly set up */
    private void sendString(String s) throws IOException {
        //TODO: character encodings?
        byte[] bytes=s.getBytes();
        _out.write(bytes);
        _out.flush();
    }

    /**
     * Reads and returns one line from the network.  A line is defined as a
     * maximal sequence of characters without '\n', with '\r''s removed.  If the
     * characters cannot be read within TIMEOUT milliseconds (as defined by the
     * property manager), throws IOException.
     *
     * @requires _socket is properly set up
     * @modifies network
     */
    private String readLine() throws IOException {
        int oldTimeout=_socket.getSoTimeout();
        try {
            _socket.setSoTimeout(SettingsManager.instance().getTimeout());
            String line=(new ByteReader(_in)).readLine();
            if (line==null)
                throw new IOException();
            return line;
        } finally {
            //Restore socket timeout.
            _socket.setSoTimeout(oldTimeout);
        }
    }

    /////////////////////////////////////////////////////////////////////////

    /**
     * Used to determine whether the connection is incoming or outgoing.
     */
    public boolean isOutgoing() {
        return _outgoing;
    }

    /** A tiny allocation optimization; see Message.read(InputStream,byte[]). */
    private byte[] HEADER_BUF=new byte[23];
    /**
     * Receives a message.  This method is NOT thread-safe.  Behavior is
     * undefined if two threads are in a receive call at the same time for a
     * given connection.
     *
     * @requires this is fully initialized
     * @effects exactly like Message.read(), but blocks until a
     *  message is available.  A half-completed message
     *  results in InterruptedIOException.
     */
    public Message receive() throws IOException, BadPacketException {
        //On the Macintosh, sockets *appear* to return the same ping reply
        //repeatedly if the connection has been closed remotely.  This prevents
        //connections from dying.  The following works around the problem.  Note
        //that Message.read may still throw IOException below.
        if (_closed)
            throw new IOException();

        Message m = null;
        while (m == null) {
            m = Message.read(_in, HEADER_BUF);
        }
        return m;
    }

    /**
     * Receives a message with timeout.  This method is NOT thread-safe.
     * Behavior is undefined if two threads are in a receive call at the same
     * time for a given connection.
     *
     * @requires this is fully initialized
     * @effects exactly like Message.read(), but throws InterruptedIOException
     *  if timeout!=0 and no message is read after "timeout" milliseconds.  In
     *  this case, you should terminate the connection, as half a message may
     *  have been read.
     */
    public Message receive(int timeout)
            throws IOException, BadPacketException, InterruptedIOException {
        //See note in receive().
        if (_closed)
            throw new IOException();

        //temporarily change socket timeout.
        int oldTimeout=_socket.getSoTimeout();
        _socket.setSoTimeout(timeout);
        try {
            Message m=Message.read(_in);
            if (m==null)
                throw new InterruptedIOException();
            return m;
        } finally {
            _socket.setSoTimeout(oldTimeout);
        }
    }

    /**
     * Sends a message.  The message may be buffered, so call flush() to
     * guarantee that the message is sent synchronously.  This method is NOT
     * thread-safe. Behavior is undefined if two threads are in a send call
     * at the same time for a given connection.
     *
     * @requires this is fully initialized
     * @modifies the network underlying this
     * @effects send m on the network.  Throws IOException if problems
     *   arise.
     */
    public void send(Message m) throws IOException {
        m.write(_out);
    }

    /**
     * Flushes any buffered messages sent through the send method.
     */
    public void flush() throws IOException {
        _out.flush();
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

    /**
     * Returns the value of the given incoming connection property, or null if
     * no such property.  For example, getProperty("Query-Routing") tells
     * whether the remote host supports query routing.  
     */
    public String getProperty(String name) {
        if (_propertiesRead==null)
            return null;
        else
            return _propertiesRead.getProperty(name);
    }

    /**
     * @return true until close() is called on this Connection
     */
    public boolean isOpen() {
        return !_closed;
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

    public String toString() {
        return "host=" + _host  + " port=" + _port; 
    }
    
    /////////////////////////// Unit Tests  ///////////////////////////////////
    
//      /** Unit test */
//      public static void main(String args[]) {
//          Properties props=new Properties();  props.setProperty("Query-Routing", "0.3");
//          ConnectionPair p=null;

//          //1. 0.4 => 0.4
//          p=connect(null, null);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing")==null);
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);

//          //2. 1.0 => 1.0
//          p=connect(props, props);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing").equals("0.3"));
//          Assert.that(p.out.getProperty("Query-Routing").equals("0.3"));
//          disconnect(p);

//          //3. 0.4 => 1.0 (Incoming doesn't send properties)
//          p=connect(props, null);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing")==null);
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);

//          //4. 1.0 => 0.4 (If the receiving connection were Gnutella 0.4, this
//          //wouldn't work.  But the new guy will automatically upgrade to 1.0.)
//          p=connect(null, props);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing").equals("0.3"));
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);
//     }   

//      private static class ConnectionPair {
//          Connection in;
//          Connection out;
//      }

//      private static ConnectionPair connect(Properties inProperties,
//                                            Properties outProperties) {
//          ConnectionPair ret=new ConnectionPair();
//          MiniAcceptor acceptor=new MiniAcceptor(inProperties);
//          try {
//              ret.out=new Connection("localhost", 6346, outProperties);
//              ret.out.initialize();
//          } catch (IOException e) { }
//          ret.in=acceptor.accept();
//          if (ret.in==null || ret.out==null)
//              return null;
//          else
//              return ret;
//      }

//      private static void disconnect(ConnectionPair cp) {
//          if (cp.in!=null)
//              cp.in.close();
//          if (cp.out!=null)
//              cp.out.close();
//      }
    

//      private static class MiniAcceptor implements Runnable {
//          Object lock=new Object();
//          Connection c=null;
//          boolean done=false;

//          Properties properties;
        
//          /** Starts the listen socket without blocking. */
//          public MiniAcceptor(Properties properties) {
//              this.properties=properties;
//              Thread runner=new Thread(this);
//              runner.start();
//              Thread.yield();  //hack to make sure runner creates socket
//          }

//          /** Blocks until a connection is available, and returns it. 
//           *  Returns null if something went awry. */
//          public Connection accept() {
//              synchronized (lock) {
//                  while (! done) {
//                      try {
//                          lock.wait();
//                      } catch (InterruptedException e) {
//                          return null;
//                      }
//                  }
//                  return c;
//              }
//          }
        
//          /** Don't call.  For internal use only. */
//          public void run() {
//              try {
//                  ServerSocket ss=new ServerSocket(6346);
//                  Socket s=ss.accept();
//                  //Technically "GNUTELLA " should be read from s.  Turns out that
//                  //out implementation doesn't care;
//                  Connection c=new Connection(s, properties);
//                  c.initialize();
//                  ss.close();
//                  synchronized (lock) {
//                      this.c=c;
//                      done=true;
//                      lock.notify();
//                  } 
//              } catch (IOException e) {
//                  synchronized (lock) {
//                      done=true;
//                      lock.notify();
//                  } 
//              }
//          }
//      }
}

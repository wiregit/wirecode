package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;
import java.util.Properties;
import java.util.Enumeration;
import com.limegroup.gnutella.handshaking.*;

/**
 * A Gnutella messaging connection.  Provides handshaking functionality and
 * routines for reading and writing of Gnutella messages.  A connection is
 * either incoming (created from a Socket) or outgoing (created from an
 * address).  This class does not provide sophisticated buffering or routing
 * logic; use ManagedConnection for that. <p>
 *
 * You will note that the constructors don't actually involve the network and
 * hence never throw exceptions or block. <b>To actual initialize a connection,
 * you must call initialize().</b> While this is somewhat awkward, it is
 * intentional.  It makes it easier, for example, for the GUI to show
 * uninitialized connections.<p>
 *
 * Connection supports the 0.4 and 0.6 handshakes.  Gnutella 0.6 connections
 * have a list of properties read and written during the handshake sequence.
 * Typical property/value pairs might be "Query-Routing: 0.3" or "User-Agent:
 * LimeWire".  Incoming connections always connect at the protocol level
 * specified by the remote host.  Outgoing connections can be made at the 0.4
 * level, the 0.6 level, or the best level possible.  Realize that the latter is
 * implemented by reconnecting the socket.<p>
 *
 * This class augments the basic 0.6 handshaking mechanism to allow
 * authentication via "401" messages.  Authentication interactions can take
 * multiple rounds.  
 */
public class Connection {
    /** 
     * The underlying socket, its address, and input and output streams.  sock,
     * in, and out are null iff this is in the unconnected state.  For thread
     * synchronization reasons, it is important that this only be modified by
     * the send(m) and receive() methods.
     */
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
     * connection may have been remotely closed even if _closed==true.  */
    private volatile boolean _closed=false;

    /** The properties read from the connection, or null if Gnutella 0.4.  */
    private Properties _propertiesRead;
    /** For outgoing Gnutella 0.6 connections, the properties written
     *  after "GNUTELLA CONNECT".  Null otherwise. */
    private Properties _propertiesWrittenP;
    /** For outgoing Gnutella 0.6 connections, a function calculating the
     *  properties written after the server's "GNUTELLA OK".  For incoming
     *  Gnutella 0.6 connections, the properties written after the client's
     *  "GNUTELLA CONNECT". */
    private HandshakeResponder _propertiesWrittenR;
    /** The list of all properties written during the handshake sequence,
     *  analogous to _propertiesRead.  This is needed because
     *  _propertiesWrittenR lazily calculates properties according to what it
     *  read. */
    private Properties _propertiesWrittenTotal=new Properties();
    /** True iff this should try to reconnect at a lower protocol level on
     *  outgoing connections. */    
    
    private boolean _negotiate=false;
    public static final String GNUTELLA_CONNECT_04="GNUTELLA CONNECT/0.4";
    public static final String GNUTELLA_OK_04="GNUTELLA OK";
    public static final String GNUTELLA_CONNECT_06="GNUTELLA CONNECT/0.6";
    public static final String GNUTELLA_OK_06="GNUTELLA/0.6 200 OK";
    public static final String GNUTELLA_06 = "GNUTELLA/0.6";
    public static final String _200_OK     = " 200 OK";
    public static final String GNUTELLA_06_200 = "GNUTELLA/0.6 200";
    public static final String CONNECT="CONNECT/";
    /** End of line for Gnutella 0.6 */
    public static final String CRLF="\r\n";
    /** End of line for Gnutella 0.4 */
    public static final String LF="\n";
    
    /**
     * Time to wait for inut from user at the remote end. (in milliseconds)
     */
    public static final int USER_INPUT_WAIT_TIME = 2 * 60 * 1000; //2 min
    
    /**
     * The number of times we will respond to a given challenge 
     * from the other side, or otherwise, during connection handshaking
     */
    public static final int MAX_HANDSHAKE_ATTEMPTS = 5;
    
    /**
	 * Constant handle to the <tt>SettingsManager</tt> for accessing
	 * various properties.
	 */
	private final SettingsManager SETTINGS = SettingsManager.instance();

    /**
     * Creates an uninitialized outgoing Gnutella 0.4 connection.
     *
     * @param host the name of the host to connect to
     * @param port the port of the remote host 
     */
    public Connection(String host, int port) {
        this(host, port, null, null, false);
    }


    /**
     * Creates an uninitialized outgoing Gnutella 0.6 connection with the
     * desired outgoing properties, possibly reverting to Gnutella 0.4 if
     * needed.
     * 
     * If properties1 and properties2 are null, forces connection at the 0.4
     * level.  This is a bit of a hack to make implementation in this and
     * subclasses easier; outside classes are discouraged from using it.
     *
     * @param host the name of the host to connect to
     * @param port the port of the remote host
     * @param properties1 the headers to be sent after "GNUTELLA CONNECT"
     * @param properties2 a function returning the headers to be sent
     *  after the server's "GNUTELLA OK".  Typically this returns only
     *  vendor-specific properties.
     * @param negotiate if true and if the first connection attempt fails, try
     *  to reconnect at the Gnutella 0.4 level with no headers 
     */
    public Connection(String host, int port,
                      Properties properties1,
                      HandshakeResponder properties2,
                      boolean negotiate) {
        _host = host;
        _port = port;
        _outgoing = true;
        _negotiate = negotiate;
        _propertiesWrittenP=properties1;
        _propertiesWrittenR=properties2;            
    }
    
    /**
     * Creates an uninitialized incoming Gnutella 0.6/0.4 connection with no
     * extra properties.  Connects at the same protocol level as the remote
     * host.
     *
     * @param socket the socket accepted by a ServerSocket.  The word
     *  "GNUTELLA " and nothing else must have been read from the socket.
     */
    public Connection(Socket socket) {
        this(socket, null);
    }

    /**
     * Creates an uninitialized incoming 0.6/0.4 Gnutella connection.  Connects
     * at the same protocol level as the incoming connection.  Hence 
     * properties are only written if the remote client supports 0.6.
     * 
     * @param socket the socket accepted by a ServerSocket.  The word
     *  "GNUTELLA " and nothing else must have been read from the socket.
     * @param properties a function returning the headers to be sent in response
     *  to the client's "GNUTELLA CONNECT".  If the client connected at the 0.4
     *  level, this method is never called.  
     */
    public Connection(Socket socket, HandshakeResponder properties) {
        //Get the address in dotted-quad format.  It's important not to do a
        //reverse DNS lookup here, as that can block.  And on the Mac, it blocks
        //your entire system!
        _host = socket.getInetAddress().getHostAddress();
        _port = socket.getPort();
        _socket = socket;
        _outgoing = false;
        _propertiesWrittenR=properties;
    }

    /**
     * Initialize the connection by doing the handshake.  Throws IOException
     * if we were unable to establish a normal messaging connection for
     * any reason.  Do not call send or receive if this happens.
     *
     * @exception IOException we were unable to connect to the host
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception BadHandshakeException some other problem establishing 
     *  the connection, e.g., the server responded with HTTP, closed the
     *  the connection during handshaking, etc.
     */
    public void initialize() 
            throws IOException, NoGnutellaOkException, BadHandshakeException {
        try {
            initializeWithoutRetry();
        } catch (NoGnutellaOkException e) {
            //Other guy speaks the same language but doesn't want us.
            //Don't bother to retry
            throw e;
        } catch (BadHandshakeException e) {
            //If an outgoing attempt at Gnutella 0.6 failed, and the user
            //has requested we try lower protocol versions, try again.
            if (_negotiate 
                    && isOutgoing() 
                    && _propertiesWrittenP!=null
                    && _propertiesWrittenR!=null) {
                //reset the flags
                _propertiesRead = null;
                _propertiesWrittenP=null;
                _propertiesWrittenR=null;
                initializeWithoutRetry();
            } else {
                throw e;
            }
        }
    }
    
    /*
     * Exactly like initialize, but without the re-connection.
     *
     * @exception IOException couldn't establish the TCP connection
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception BadHandshakeException some sort of protocol error after
     *  establishing the connection
     */
    private void initializeWithoutRetry() throws IOException {
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
            if(isOutgoing())
                initializeOutgoing();
            else
                initializeIncoming();
        } catch (NoGnutellaOkException e) {
            _socket.close();
            throw e;
        } catch (IOException e) {
            _socket.close();
            throw new BadHandshakeException(e);
        }
    }

    /** 
     * Sends and receives handshake strings for outgoing connections,
     * throwing exception if any problems. 
     * 
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException any other error.  May wish to retry at 0.4
     */
    private void initializeOutgoing() throws IOException {
        //On outgoing connections, ALWAYS try Gnutella 0.6 if requested by the
        //user.  If the other end doesn't understand it--too bad!  There is an
        //option at higher levels to retry.
        if (_propertiesWrittenP==null || _propertiesWrittenR==null) {
            sendString(GNUTELLA_CONNECT_04+LF+LF);
            if (! readLine().equals(GNUTELLA_OK_04))
                throw new IOException("Bad connect string"); 
            if (! readLine().equals(""))  //Get second \n
                throw new IOException("Bad connect string"); 
        }
        else {
            //1. Send "GNUTELLA CONNECT" and headers
            sendString(GNUTELLA_CONNECT_06+CRLF);
            sendHeaders(_propertiesWrittenP);   
            
            //conclude the handshake (This may involve exchange of 
            //information multiple times with the host at the other end).
            concludeOutgoingHandshake();
        }
    }
    
    /**
     * Responds to the responses/challenges from the host on the other
     * end of the connection, till a conclusion reaches. Handshaking may
     * involve multiple steps. 
     *
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException any other error.  May wish to retry at 0.4
     */
    private void concludeOutgoingHandshake() throws IOException
    {
        //This step may involve handshaking multiple times so as
        //to support challenge/response kind of behaviour
        for(int i=0; i < MAX_HANDSHAKE_ATTEMPTS; i++){
            //2. Read "GNUTELLA/0.6 200 OK" and headers.  We require that the
            //response be at the same protocol level as we sent out.  This is
            //necessary because BearShare will accept "GNUTELLA CONNECT/0.6" and
            //respond with "GNUTELLA OK", only to be confused by the headers
            //later.
            String connectLine = readLine();
            if (! connectLine.startsWith(GNUTELLA_06))
                throw new IOException("Bad connect string");
            //Read the headers.  The _propertiesRead field is allocated here
            //if not already done to signify that this is Gnutella 0.6.
            if (_propertiesRead==null)
                _propertiesRead=new Properties();
            readHeaders();
            //Terminate abnormally if we read something other than 200 or 401.
            HandshakeResponse theirResponse=new HandshakeResponse(
                connectLine.substring(GNUTELLA_06.length()).trim(), 
                _propertiesRead);
            int theirCode=theirResponse.getStatusCode();
            if (theirCode!=HandshakeResponse.OK 
                    &&  theirCode!=HandshakeResponse.UNAUTHORIZED_CODE)
                throw new NoGnutellaOkException(false, 
                                                theirResponse.getStatusCode(),
                                                "Server sent fatal response");

            //3. Write "GNUTELLA/0.6 200 OK" and headers.
            HandshakeResponse ourResponse = _propertiesWrittenR.respond(
                theirResponse, true);
            sendString(GNUTELLA_06 + " " 
                + ourResponse.getStatusLine() + CRLF);
            sendHeaders(ourResponse.getHeaders());
            //Consider termination...
            if(ourResponse.getStatusCode() == HandshakeResponse.OK) {
                if(ourResponse.getStatusMessage().equals(
                    HandshakeResponse.OK_MESSAGE)){
                    //a) Terminate normally if we wrote "200 OK".
                    return;
                } else {
                    //b) Continue loop if we wrote "200 AUTHENTICATING".                    
                    continue;
                }
            } else {
                //c) Terminate abnormally if we wrote anything else.               
                throw new NoGnutellaOkException(true,
                                                ourResponse.getStatusCode(),
                                                "We sent fatal response");
            }
        }
            
        //If we didn't successfully return out of the method, throw an exception
        //to indicate that handshaking didn't reach any conclusion.  The values
        //here are kind of a hack.
        throw new NoGnutellaOkException(false,
                                        HandshakeResponse.UNAUTHORIZED_CODE,
                                        "Too much handshaking, no conclusion");
    }
    
    /** 
     * Sends and receives handshake strings for incoming connections,
     * throwing exception if any problems. 
     * 
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException any other error.  May wish to retry at 0.4
     */
    private void initializeIncoming() throws IOException {
        //Dispatch based on first line read.  Remember that "GNUTELLA " has
        //already been read by Acceptor.  Hence we are looking for "CONNECT/0.4"
        //or "CONNECT/0.6".  As a dirty hack, we use String.endsWith.  This
        //means we will accidentally allow crazy things like "0.4".  Oh well!
        String line=readLine();  
        if ( !SETTINGS.acceptAuthenticatedConnectionsOnly()
            && GNUTELLA_CONNECT_04.endsWith(line)) {
            //a) Old style
            if (! readLine().equals(""))  //Get second \n
                throw new IOException("Bad connect string"); 
            sendString(GNUTELLA_OK_04+LF+LF);
            //If the user requested properties, we can't send them.
            _propertiesWrittenP=null;
            _propertiesWrittenR=null;
        } else if (notLessThan06(line)) {
            //b) New style
            _propertiesRead=new Properties();
            //1. Read headers (connect line has already been read)
            readHeaders();
            //Conclude the handshake (This may involve exchange of information
            //multiple times with the host at the other end).
            concludeIncomingHandshake();
        } else {
            throw new IOException("Unexpected connect string");
        }
    }

    
    /**
     * Responds to the handshake from the host on the other
     * end of the connection, till a conclusion reaches. Handshaking may
     * involve multiple steps.
     * 
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception IOException any other error.  May wish to retry at 0.4
     */
    private void concludeIncomingHandshake() throws IOException
    {
        //Respond to the handshake.  This step may involve handshaking multiple
        //times so as to support challenge/response kind of behaviour
        for(int i=0; i < MAX_HANDSHAKE_ATTEMPTS; i++){
            //2. Send our response and headers.
            HandshakeResponse ourResponse=null;
            if (_propertiesWrittenR==null) 
                //user requested didn't specify, so use default 200 OK;
                ourResponse=new HandshakeResponse(new Properties());
            else
                //Note: in the following code, it appears that we're ignoring
                //the response code written by the initiator of the connection.
                //However, you can prove that the last code was always 200 OK.
                //See initializeIncoming and the code at the bottom of this
                //loop.
                ourResponse= _propertiesWrittenR.respond(
                    new HandshakeResponse(_propertiesRead), false);
            sendString(GNUTELLA_06 + " " 
                       + ourResponse.getStatusLine() + CRLF);
            sendHeaders(ourResponse.getHeaders());                   
            //Our response should be either OK or UNAUTHORIZED for the handshake
            //to proceed.
            if((ourResponse.getStatusCode() != HandshakeResponse.OK)
               && (ourResponse.getStatusCode() !=
                   HandshakeResponse.UNAUTHORIZED_CODE)) {
                throw new NoGnutellaOkException(true,
                                                ourResponse.getStatusCode(),
                                                "We sent fatal status code");
            }
                    
            //3. read the response from the other side.  If we asked the other
            //side to authenticate, give more time so as to receive user input
            String connectLine;
            if(ourResponse.getStatusCode() 
               == HandshakeResponse.UNAUTHORIZED_CODE){
                connectLine = readLine(USER_INPUT_WAIT_TIME);  
                readHeaders(USER_INPUT_WAIT_TIME); 
            }else{
                connectLine = readLine();  
                readHeaders();
            }
            if (! connectLine.startsWith(GNUTELLA_06))
                throw new IOException("Bad connect string");
            HandshakeResponse theirResponse=new HandshakeResponse(
                connectLine.substring(GNUTELLA_06.length()).trim(), 
                _propertiesRead);

            //Decide whether to proceed.
            int ourCode=ourResponse.getStatusCode();
            if(ourCode == HandshakeResponse.OK) {
                if(theirResponse.getStatusCode()==HandshakeResponse.OK)
                    //a) If we wrote 200 and they wrote 200 OK, stop normally.
                    return;
            } else {
                Assert.that(ourCode==HandshakeResponse.UNAUTHORIZED_CODE,
                            "Response code: "+ourCode);
                if(theirResponse.getStatusCode()==HandshakeResponse.OK)
                    //b) If we wrote 401 and they wrote "200...", keep looping.
                    continue;
            }
            //c) Terminate abnormally
            throw new NoGnutellaOkException(false,
                                            theirResponse.getStatusCode(),
                                            "Initiator sent fatal status code");
        }        

        //If we didn't successfully return out of the method, throw an exception
        //to indicate that handshaking didn't reach any conclusion.  The values
        //here are kind of a hack.
        throw new NoGnutellaOkException(true,
                                        HandshakeResponse.UNAUTHORIZED_CODE,
                                        "Too much handshaking, no conclusion");
    }
    
    /** Returns true iff line ends with "CONNECT/N", where N
     *  is a number greater than or equal "0.6". */
    private static boolean notLessThan06(String line) {
        int i=line.indexOf(CONNECT);
        if (i<0)
            return false;
        try {
            Float F = new Float(line.substring(i+CONNECT.length()));
            float f= F.floatValue();
            return f>=0.6f;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Writes the properties in props to network, including the blank line at
     * the end.  Throws IOException if there are any problems.
     * @param props The headers to be sent. Note: null argument is 
     * acceptable, if no headers need to be sent (still the trailer will
     * be sent
     * @modifies network 
     */
    private void sendHeaders(Properties props) throws IOException {
        if(props != null) {
            Enumeration enum=props.propertyNames();
            while (enum.hasMoreElements()) {
                String key=(String)enum.nextElement();
                String value=props.getProperty(key);
                if (value==null)
                    value="";
                sendString(key+": "+value+CRLF);   
                _propertiesWrittenTotal.put(key, value);
            }
        }
        //send the trailer
        sendString(CRLF);
    }


    /**
     * Reads the properties from the network into _propertiesRead, throwing
     * IOException if there are any problems. 
     *     @modifies network 
     */
    private void readHeaders() throws IOException {
        readHeaders(SETTINGS.getTimeout());
    }
    
    /**
     * Reads the properties from the network into _propertiesRead, throwing
     * IOException if there are any problems. 
     * @param timeout The time to wait on the socket to read data before 
     * IOException is thrown
     * @return The line of characters read
     * @modifies network
     * @exception IOException if the characters cannot be read within 
     * the specified timeout
     */
    private void readHeaders(int timeout) throws IOException {
        //TODO: limit number of headers read
        while (true) {
            //This doesn't distinguish between \r and \n.  That's fine.
            String line=readLine(timeout);
            if (line==null)
                throw new IOException();   //unexpected EOF
            if (line.equals(""))
                return;                    //blank line ==> done
            int i=line.indexOf(':');
            if (i<0)
                continue;                  //ignore lines without ':'
            String key=line.substring(0, i);
            String value=line.substring(i+1).trim();
            _propertiesRead.put(key, value);
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
     * property manager), throws IOException.  This includes EOF.
     * @return The line of characters read
     * @requires _socket is properly set up
     * @modifies network
     * @exception IOException if the characters cannot be read within 
     * the specified timeout
     */
    private String readLine() throws IOException {
        return readLine(SETTINGS.getTimeout());
    }

    /**
     * Reads and returns one line from the network.  A line is defined as a
     * maximal sequence of characters without '\n', with '\r''s removed.  If the
     * characters cannot be read within the specified timeout milliseconds,
     * throws IOException.  This includes EOF.
     * @param timeout The time to wait on the socket to read data before 
     * IOException is thrown
     * @return The line of characters read
     * @requires _socket is properly set up
     * @modifies network
     * @exception IOException if the characters cannot be read within 
     * the specified timeout
     */
    private String readLine(int timeout) throws IOException {
        int oldTimeout=_socket.getSoTimeout();
        try {
            _socket.setSoTimeout(timeout);
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
     * Sets the port where the conected node listens at, not the one
     * got from socket
     */
    void setOrigPort(int port){
        this._port = port;
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
     * Returns the headers received during connection Handshake
     * @return the headers received during connection Handshake. All the
     * headers received are combined together. 
     * (headers are received twice for the incoming connections)
     */
    public Properties getHeaders(){
        if (_propertiesRead==null)
            return null;
        else
            return (Properties)_propertiesRead.clone();
    }

    /**
     * Returns the value of the given outgoing (written) connection property, or
     * null if no such property.  For example, getProperty("X-Supernode") tells
     * whether I am a supernode or a leaf node.  If I wrote a property multiple
     * time during connection, returns the latest.
     */
    public String getPropertyWritten(String name) {
        return _propertiesWrittenTotal.getProperty(name);
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
//          Assert.that(! notLessThan06("CONNECT"));
//          Assert.that(! notLessThan06("CONNECT/0.4"));
//          Assert.that(! notLessThan06("CONNECT/0.599"));
//          Assert.that(! notLessThan06("CONNECT/XP"));
//          Assert.that(notLessThan06("CONNECT/0.6"));
//          Assert.that(notLessThan06("CONNECT/0.7"));
//          Assert.that(notLessThan06("GNUTELLA CONNECT/1.0"));

//          final Properties props=new Properties();
//          props.setProperty("Query-Routing", "0.3");        
//          HandshakeResponder standardResponder=new HandshakeResponder() {
//              public HandshakeResponse respond(HandshakeResponse response,
//                                               boolean outgoing) {
//                  return new HandshakeResponse(props);;
//              }
//          };        
//          HandshakeResponder secretResponder=new HandshakeResponder() {
//              public HandshakeResponse respond(HandshakeResponse response,
//                                               boolean outgoing) {
//                  Properties props2=new Properties();
//                  props2.setProperty("Secret", "abcdefg");
//                  return new HandshakeResponse(props2);
//              }
//          };
//          ConnectionPair p=null;

//          //1. 0.4 => 0.4
//          p=connect(null, null, null);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing")==null);
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);

//          //2. 0.6 => 0.6
//          p=connect(standardResponder, props, secretResponder);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing").equals("0.3"));
//          Assert.that(p.out.getProperty("Query-Routing").equals("0.3"));
//          Assert.that(p.out.getProperty("Secret")==null);
//          Assert.that(p.in.getProperty("Secret").equals("abcdefg"));
//          disconnect(p);

//          //3. 0.4 => 0.6 (Incoming doesn't send properties)
//          p=connect(standardResponder, null, null);
//          Assert.that(p!=null);
//          Assert.that(p.in.getProperty("Query-Routing")==null);
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);

//          //4. 0.6 => 0.4 (If the receiving connection were Gnutella 0.4, this
//          //wouldn't work.  But the new guy will automatically upgrade to 0.6.)
//          p=connect(null, props, standardResponder);
//          Assert.that(p!=null);
//          //Assert.that(p.in.getProperty("Query-Routing")==null);
//          Assert.that(p.out.getProperty("Query-Routing")==null);
//          disconnect(p);
//      }   

//      private static class ConnectionPair {
//          Connection in;
//          Connection out;
//      }

//      private static ConnectionPair connect(HandshakeResponder inProperties,
//                                            Properties outProperties1,
//                                            HandshakeResponder outProperties2) {
//          ConnectionPair ret=new ConnectionPair();
//          MiniAcceptor acceptor=new MiniAcceptor(inProperties);
//          try {
//              ret.out=new Connection("localhost", 6346,
//                                     outProperties1, outProperties2,
//                                     true);
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

//          HandshakeResponder properties;
        
//          /** Starts the listen socket without blocking. */
//          public MiniAcceptor(HandshakeResponder properties) {
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

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import com.sun.java.util.collections.*;
import java.util.Properties;
import java.util.Enumeration;
import com.limegroup.gnutella.connection.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.util.Sockets;

/**
 * A Gnutella messaging connection.  Provides handshaking functionality and
 * routines for reading and writing of Gnutella messages.  A connection is
 * either incoming (created from a Socket) or outgoing (created from an
 * address).  This class does not provide buffering or flow control; use
 * ManagedConnection.  <p>
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
     * The underlying socket, its address, and input and output streams.  These
     * variables are null iff this is in the unconnected state.  _in/_out and
     * _reader/_writer refer to the same socket.  The former is used for
     * handshaking and the latter for message reading/writing.  Hence they must
     * not be buffered, and only one can be used at a time.  
     */
    private String _host;
    private int _port;
    private Socket _socket;
    private InputStream _in;
    private OutputStream _out;
    private MessageReader _reader;
    private MessageWriter _writer;
    private boolean _outgoing;

    /** The listener for connection events. */
    private ConnectionListener _listener;

    /**
     * Trigger an opening connection to close after it opens.  This
     * flag is set in shutdown() and then checked in initialize()
     * to insure the _socket.close() happens if shutdown is called
     * asynchronously before initialize() completes.  Note that the 
     * connection may have been remotely closed even if _closed==true.  
     * Protected (instead of private) for testing purposes only.
     */
    protected volatile boolean _closed=false;

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
     *  The socket MUST have been created with ServerSocketChannel.accept()
     *  instead of ServerSocket.accept; otherwise non-blocking IO will fail.
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
     *  The socket MUST have been created with ServerSocketChannel.accept()
     *  instead of ServerSocket.accept; otherwise non-blocking IO will fail.
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
        Assert.that(socket.getChannel()!=null, "No channel for socket");
    }

    /** 
     * Initializes this without timeout; exactly like initialize(listener, 0). 
     * @see initialize(int)
     */
    public void initialize(ConnectionListener listener) 
            throws IOException, NoGnutellaOkException, BadHandshakeException {
        initialize(listener, 0);
    }

    /**
     * Initialize the connection by doing the handshake.  Throws IOException
     * if we were unable to establish a normal messaging connection for
     * any reason.  Do not call send or receive if this happens.
     *
     * @param listener the observer of all Connection events
     * @param timeout for outgoing connections, the timeout in milliseconds
     *  to use in establishing the socket, or 0 for no timeout.  If the 
     *  platform does not support native timeouts, it will be emulated with
     *  threads.
     * @exception IOException we were unable to connect to the host
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception BadHandshakeException some other problem establishing 
     *  the connection, e.g., the server responded with HTTP, closed the
     *  the connection during handshaking, etc.
     */
    public void initialize(ConnectionListener listener, int timeout) 
            throws IOException, NoGnutellaOkException, BadHandshakeException {
        this._listener=listener;
        try {
            initializeWithoutRetry(timeout);
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
                initializeWithoutRetry(timeout);
            } else {
                throw e;
            }
        }
        //Change to non-blocking mode for messaging.
        _socket.getChannel().configureBlocking(false);
        //Notify of initialization.  TODO: notify of close as well.
        _listener.initialized(this);
    }
    
    /*
     * Exactly like initialize, but without the re-connection.
     *
     * @param timeout for outgoing connections, the timeout in milliseconds
     *  to use in establishing the socket, or 0 for no timeout
     * @exception IOException couldn't establish the TCP connection
     * @exception NoGnutellaOkException one of the participants responded
     *  with an error code other than 200 OK (possibly after several rounds
     *  of 401's)
     * @exception BadHandshakeException some sort of protocol error after
     *  establishing the connection
     */
    private void initializeWithoutRetry(int timeout) throws IOException {
        SettingsManager settingsManager = SettingsManager.instance();
        String expectString;
 
        if(isOutgoing())
            _socket=Sockets.connect(_host, _port, timeout, true);

        // Check to see if close() was called while the socket was initializing
        if (_closed) {
            _socket.close();
            throw new IOException();
        }

        try {
            // Set the Acceptors IP address
            Acceptor.setAddress( _socket.getLocalAddress().getAddress() );
            
            _in = _socket.getInputStream();
            _out = _socket.getOutputStream();           
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
        SocketChannel channel=_socket.getChannel();
        Assert.that(channel!=null,"Null channel for socket ("+isOutgoing()+")");
        channel.configureBlocking(true);  //for handshaking; see initialize()
        _reader=new MessageReader(channel);
        _writer=new MessageWriter(channel);

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
    
    /** Returns true iff line ends with "CONNECT/N", where N is a number greater
     *  than or equal "0.6".  Public for testing purposes only. */
    public static boolean notLessThan06(String line) {
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
                // Overwrite any domainname with true IP address
                if ( ConnectionHandshakeHeaders.REMOTE_IP.equals(key) )
                    value=getInetAddress().getHostAddress();
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
        } catch (CancelledKeyException e) {
            //There appears to be a bug in Java 1.4.1-beta on Windows where this
            //can be thrown if a connection is closed by the client while the
            //read is in progress.
            throw new IOException();
        } catch (NullPointerException e) {
            //There appears to be a bug in Java 1.4.1-beta on Windows where this
            //can be thrown if a connection is closed by the client while the
            //read is in progress.
            throw new IOException();
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

    /**
     * Returns this' underlying communication channel.  This exposes the
     * internals of this but is necessary for registering with a Selector.  DO
     * NOT MODIFY CHANNEL.  
     */
    public SocketChannel channel() {
        return _socket.getChannel();
    }

    /**
     * Reads as much data from the channel as possible.  For any messages m
     * received (zero or many), calls listener.read(this, m).  Calls
     * listener.read(this, error) for any non-fatal errors, and
     * listener.error(c) for any fatal errors, like the connection being closed.
     */
    public void read() {
        //On the Macintosh, sockets *appear* to return the same ping reply
        //repeatedly if the connection has been closed remotely.  This prevents
        //connections from dying.  The following works around the problem.  Note
        //that Message.read may still throw IOException below.
        if (_closed) {            
            _listener.error(this);         //Is this still needed?
            return;            
        }
             
        //TODO: read as much as possible without blocking?
        try {
            Message m=_reader.read();
            if (m!=null)
                _listener.read(this, m);            
        } catch (BadPacketException e) {
            _listener.read(this, e);
        } catch (IOException e) {
            _listener.error(this);
        }                   
    }

    /** Returns true if this has queued data, and hence is unable to accept
     *  more messages. */
    protected boolean hasQueued() {
        return _writer.hasQueued();
    }

    /**
     * Attempts to send m, or as much as possible.  First attempts to add m to
     * this' send queue, possibly discarding other queued messages (or m) for
     * which sending has not yet started.  Then attempts to send as much data to
     * the network as possible without blocking.  Calls
     * listener.needsWrite(this) if not all data was sent.  Calls
     * listener.error(this) if connection closed.<p>
     *
     * This method is called from deep within the bowels of the message handling
     * code.  That's why it doesn't block and generates needWrite events through
     * a callback in addition to a return value.  
     *
     * @return true iff this still has unsent queued data.  If true, the caller
     *  must subsequently call write() again 
     */
    public boolean write(Message m) {
        if (_closed) {
            _listener.error(this);
            return false;
        }                    

        try {            
            boolean needsWrite=_writer.write(m);
            if (needsWrite)
                _listener.needsWrite(this); 
            return needsWrite;
        } catch (IOException e) {
            _listener.error(this);
            return false;
        }                 
    }

    /**
     * Sends as much queued data as possible, if any.  Calls
     * listener.error(this) if connection closed.  Typically does NOT call
     * listener.needsWrite(this), though subclasses may change this behavior.<p>
     *
     * This method is called within the Selector code, which typically
     * needs to know whether to register the write operation.  That's why this
     * returns a value instead of using the callback.
     * 
     * @return true iff this still has unsent queued data.  If true, the caller
     *  must subsequently call write() again 
     */
    public boolean write() {
        if (_closed) {
            _listener.error(this);
            return false;
        }

        try {            
            return _writer.write();
        } catch (IOException e) {
            _listener.error(this);
            return false;
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
     * Sets the port where the conected node listens at, not the one
     * got from socket
     */
    void setOrigPort(int port){
        this._port = port;
    }

    /**
     * Returns the port of the foreign host this is connected to.
     * @exception IllegalStateException this is not initialized
     */
    public int getPort() throws IllegalStateException {
        try {
            return _socket.getPort();
        } catch (NullPointerException e) {
            throw new IllegalStateException("Not initialized");
        }
    }

    /**
     * Returns the port this is connected to locally.
     * @exception IllegalStateException this is not initialized
     */
    public int getLocalPort() throws IllegalStateException {
        try {
            return _socket.getLocalPort();
        } catch (NullPointerException e) {
            throw new IllegalStateException("Not initialized");
        }
    }

    /**
     * Returns the address of the foreign host this is connected to.
     * @exception IllegalStateException this is not initialized
     */
    public InetAddress getInetAddress() throws IllegalStateException {
        try {
            //TODO: this is just a work-around....let's just eliminate this
            //method.
            return InetAddress.getByName(_host);
        } catch (NullPointerException e) {
            throw new IllegalStateException("Not initialized");
        } catch (UnknownHostException e) {
            //Actually this COULD happen
            Assert.that(false, "Couldn't resolve already resolved name");
            return null;
        }
    }

    /**
     * Returns the local address of this.
     * @exception IllegalStateException this is not initialized
     */
    public InetAddress getLocalAddress() throws IllegalStateException {
        try {
            return _socket.getLocalAddress();
        } catch (NullPointerException e) {
            throw new IllegalStateException("Not initialized");
        }
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
    
    //Unit test: tests/com/limegroup/gnutella/connection/ConnectionTest.java   
}

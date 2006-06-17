
// Commented for the Learning branch

package com.limegroup.gnutella;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.HeaderUpdateVendorMessage;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.messages.vendor.StatisticVendorMessage;
import com.limegroup.gnutella.messages.vendor.VendorMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.StringSetting;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.statistics.CompressionStat;
import com.limegroup.gnutella.statistics.ConnectionStat;
import com.limegroup.gnutella.statistics.HandshakingStat;
import com.limegroup.gnutella.util.CompressingOutputStream;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.util.UncompressingInputStream;

/**
 * A Connection object represents our connection to a remote computer running Gnutella software.
 * LimeWire never makes a Connection object, and just makes objects from the extended class ManagedConnection instead.
 * 
 * There are two constructors.
 * Use one to connect to a remote computer, and the other when the remote computer has connected to us.
 * To connect to a remote computer, use the one that takes the IP address and port number you want to connect to.
 * When a remote computer has connected to us, use the one that takes the connection socket the Acceptor got.
 * 
 * The constructors don't actually do anything on the Internet.
 * This lets you make a new Connection object without worrying about anything blocking or throwing an exception.
 * This also makes it easier to show a listing for a not yet connected connection in the program's user interface.
 * Make a new Connection object with the IP address and port number you want to connect to,
 * then call initialize() to have it actually try to connect.
 * 
 * Handshake
 * This class contains the code that actually performs the 3 steps of the Gnutella 0.6 handshake.
 * Handshake headers are kept in Properties hash tables of strings, with keys like "Query-Routing" and values like "0.3".
 * 
 * Messages
 * There are methods here to read Gnutella packets from the remote computer, and write packets to it.
 * 
 * Compression
 * This class can compress data before sending it to the remote computer, and remove compression from the data we download.
 * The HandshakeResponder object decides if we'll turn compression on or not.
 * This class looks at the headers we wrote and got, and sets up compression if necessary.
 * If compression is setup and you call getInputStream(), you'll get the compressed input stream that reads from the socket.
 * This starts a chain of objects, each reading from the next.
 * 
 * Statistics
 * This class keeps statistics about how many bytes of data, before and after compression, are transferred each way.
 * 
 * Soft Max TTL
 * The Soft Max TTL is our limit to hops + TTL for Gnutella packets from this remote computer.
 * If this remote computer sends us a packet with hops + TTL greater than this maximum, we'll lower the TTL to be within the limit.
 * This limit doesn't apply to query hit packets.
 * There is a custom soft max TTL for each remote computer, not just one for the whole program.
 * This lets us make it 4 instead of 3 for computers that understand the "X-Max-TTL" header.
 * They will use higher TTL packets for rare searches.
 * The soft max TTL limit prevents malicious hosts from using headers like "X-Max-TTL" to simply get connections.
 * They also have to abide by the contract of the "X-Max-TTL" header, making them send lower TTL traffic overall.
 */
public class Connection implements IpPort {

	/** A log that we can write lines of text into as the code here runs. */
    private static final Log LOG = LogFactory.getLog(Connection.class);

    // Thread synchronization for ping and pong forwarding
	/** Lock that only lets one thread into the allowNewPings() method at a time. */
	private final Object PING_LOCK = new Object();
    /** Lock for maintaining accurate data for when to allow pong forwarding. */
    private final Object PONG_LOCK = new Object();

    /*
     * The remote computer's IP and port, the socket, streams to read and write, and who initiated the connection.
     * Before this Connection is initialized, _socket, _in, and _out are null.
     */

    /**
     * The IP address of the remote computer.
     * 
     * If we connected to the remote computer, this is the IP address we connected to.
     * It's the remote computer's externally contactable Internet IP address.
     * The remote computer is probably behind a NAT, so this is the IP address of the NAT.
     * 
     * If the remote computer connected to us, this is the IP address on the far end of the connection socket.
     * It's the IP address of the remote computer's NAT.
     * The remote computer won't know it's own IP address until we tell it this address.
     */
    private final String _host;

    /**
     * The port number of the remote computer.
     * 
     * If we connected to the remote computer, this is the port number we connected to.
     * It's the remote computer's externally contactable Internet port number.
     * The remote computer is probably behind a NAT, so this is the port number on the NAT.
     * The remote computer setup port forwarding so packets that hit the NAT with this port number get forwarded to it on the LAN.
     * 
     * If the remote computer connected to us, this starts out as the ephemeral port.
     * The ephemeral port is a random high-numbered port the remote computer's operating system chose to make the new connection from.
     * The ephemeral port number is no use to us.
     * Separate from this ephemeral port, the remote computer has probably setup port forwarding from its NAT to its listening socket.
     * When the remote computer tells us the port number we can contact it at, we'll call setListeningPort() to save that number here.
     */
    private int _port;

    /** The connection socket we talk to the remote computer through. */
    protected Socket _socket;
    /** Read from this BufferInputStream object to get data the remote computer sent. */
    private InputStream _in;
    /** Write to this ThrottledOutputStream object to send data to the remote computer */
    private OutputStream _out;
    /** True if we connected to the remote computer's IP address, false if the remote computer connected to our listening socket. */
    private final boolean OUTGOING;

    /*
     * Objects that compress and decompress data
     * The initialize method sets these up if we're going to talk compressed data with this remote computer
     * This is stream compression, we can start compression the data without knowing what will come next
     * The headers identify this kind of compression as "Content-Encoding: deflate"
     * 
     * C and C++ program use the zlib library to do this kind of zip compression
     * It's built right into Java, in the package java.util.zip
     * 
     * Most Gnutella connections are compressed in both directions.
     * This means that we read compressed data from a remote computer, and then decompress it.
     * We start out with decompressed data for a remote computer, and compress it and then send it.
     * Here's how to get the total number of bytes transferred in each direction, before and after compression.
     * 
     * Read compressed data:    _inflater.getTotalIn,  use _compressedBytesReceived instead
     * Read data:               _inflater.getTotalOut
     * Written data:            _deflater.getTotalIn
     * Written compressed data: _deflater.getTotalOut, use _compressedBytesSent instead
     * 
     * When you close this connection, code here calls end() on the deflater and inflater.
     * After calling end() on the deflater or inflater, you can't call getTotalOut() or getTotalIn() anymore.
     * So, use _compressedBytesSent and _compressedBytesReceived instead.
     * 
     * _deflater is the same as _out.
     * It's referenced as a Deflater so you don't have to cast _out to Deflater to read compression statistics from it.
     */

    /** The object that decompresses the input stream of data we're reading from the remote computer. */
    protected Inflater _inflater;
    /** The object that compresses the output stream of data we're writing to the remote computer. */
    protected Deflater _deflater; // Same as _out

    // Statistics about how many bytes we've sent and received compressed and not compressed

    /** The number of bytes of data we've sent this remote computer, before compression. */
    private volatile long _bytesSent;
    /** The number of bytes of data we've received from this remote computer, after decompressing the data. */
    private volatile long _bytesReceived;
    /** The number of bytes of compressed data we've sent to this remote computer. */
    private volatile long _compressedBytesSent;
    /** The number of bytes of compressed data we've received from this remote computer. */
    private volatile long _compressedBytesReceived;

    /**
     * The Messages Supported vendor message from the remote computer.
     * This vendor message lists what other vendor-specific messages the remote computer understands.
     * Gnutella programs exchange Messages Supported vendor messages right after the handshake.
     */
    protected MessagesSupportedVendorMessage _messagesSupported = null;

    /**
     * The Capabilities vendor-specific message from the remote computer.
     * This vendor message tells whether the remote computer supports some additional advanced features.
     * Gnutella programs exchange Capabilities vendor messages right after the handshake.
     */
    protected CapabilitiesVM _capabilities = null;

    /**
     * (do)
     * 
     * Trigger an opening connection to close after it opens.
     * This flag is set in shutdown() and then checked in initialize() to insure the _socket.close() happens if shutdown is called aysnchronously before initialize() completes.
     * Note that the connection may have been remotely closed even if _closed == true.
     * Protected (instead of private) for testing purposes only.
     * This also protects us from calling methods on the Inflater/Deflater objects after end has been called on them.
     */
    protected volatile boolean _closed = false;

    /*
     * A Gnutella connection begins with the Gnutella handshake.
     * The Gnutella handshake consists of 3 groups of headers.
     * The computer that connects sends group 1, the receiving computer responds with group 2, and then the connecting computer finishes with group 3.
     * Each header in the group is a line of ASCII text.
     * The line ends with "\r\n", and a blank line ends the group.
     * 
     * A simple way to hold a group of headers is to use a Properties hash table of strings.
     * Another way to hold a group of headers is with a HandshakeResponse object.
     * The HandshakeResponder object holds a group of headers from the remote computer, and composes the next group that we'll send in response.
     * 
     * Here's a diagram that shows the 3 groups of headers in both the case where we initiated the connection, and the remote computer connected to us.
     * 
     * We connected to the remote computer                     If the remote computer connected to us
     * OUTGOING is true                                        OUTGOING is false
     * 
     * us                      remote computer                 us                      remote computer
     * --                      ---------------                 --                      ---------------
     * 
     * (1)                                                                             (1)
     * "GNUTELLA CONNECT/0.6"                                                          "GNUTELLA CONNECT/0.6"
     * REQUEST_HEADERS                                                                 RESPONSE_HEADERS (hold)
     * 
     *                         (2)                             (2)
     *                         "GNUTELLA/0.6 200 OK"           "GNUTELLA/0.6 200 OK"
     *                         RESPONSE_HEADERS (hold)         RESPONSE_HEADERS (compose)
     * 
     * (3)                                                                             (3)
     * "GNUTELLA/0.6 200 OK"                                                           "GNUTELLA/0.6 200 OK"
     * RESPONSE_HEADERS (compose)
     * 
     * HEADERS_WRITTEN         HEADERS_READ                    HEADERS_WRITTEN         HEADERS_READ
     * _headersWritten         _headers                        _headersWritten         _headers
     * 
     * HEADERS_WRITTEN and _headersWritten hold all the headers we sent.
     * If we connected, this means they hold the headers of groups 1 and 3 combined.
     * If the remote computer connected, this means they hold the headers of group 2.
     * 
     * HEADERS_READ and _headers hold all the headers the remote computer sent us.
     * If we connected, this means they hold the headers of group 2.
     * If the remote computer connected, this means they hold the headers of groups 1 and 3.
     * 
     * The HandshakeResponder RESPONSE_HEADERS lazily calculates properties according to what it has read.
     * So, if you want to find out if we've written some header, look for it in HEADERS_WRITTEN instead.
     */
    
    /**
     * If we connected to the remote computer, REQUEST_HEADERS is our stage 1 headers we send after "GNUTELLA CONNECT".
     * If the remote computer connected to us, REQUEST_HEADERS is null.
     */
    private final Properties REQUEST_HEADERS;
    
    /**
     * If we connected to the remote computer, RESPONSE_HEADERS reads stage 2 and composes stage 3.
     * If the remote computer connected to us, RESPONSE_HEADERS reads stage 1 and composes stage 2.
     */
    protected HandshakeResponder RESPONSE_HEADERS; // Not final so you can set it to null and let the garbage collector free it

    /** All the handshake headers the remote computer sent us, kept as a Properties hash table of strings */
    private final Properties HEADERS_READ = new Properties();
    /** All the handshake headers the remote computer sent us, kept as a HandshakeResponse object that holds one group of headers */
    private volatile HandshakeResponse _headers = HandshakeResponse.createEmptyResponse(); // Start it out with "GNUTELLA/0.6 200 OK" and no headers after that yet

    /** All the handshake headers we sent the remote computer, kept as a Properties hash table of strings */
    private final Properties HEADERS_WRITTEN = new Properties();
    /** All the handshake headers we sent the remote computer, kept as a HandshakeResponse object that holds one group of headers */
    private HandshakeResponse _headersWritten = HandshakeResponse.createEmptyResponse(); // Start it out with "GNUTELLA/0.6 200 OK" and no headers after that yet

    /*
     * Text commonly used in the Gnutella handshake.
     * Instead of typing it in the code, use these public static final strings instead.
     */

	/** "GNUTELLA CONNECT/0.6", the first line of the first group of handshake headers */
    private             String GNUTELLA_CONNECT_06 = "GNUTELLA CONNECT/0.6";
	/** "GNUTELLA/0.6 200 OK", the first line of the second and third groups of handshake headers */
    public static final String GNUTELLA_OK_06      = "GNUTELLA/0.6 200 OK";
    /** "GNUTELLA/0.6", the start of the first line of the second and third groups of handshake headers */
    public static final String GNUTELLA_06         = "GNUTELLA/0.6";
    /** " 200 OK", part of the first line of the second and third groups of handshake headers */
    public static final String _200_OK             = " 200 OK";
    /** "GNUTELLA/0.6 200", part of the first line of the second and third groups of handshake headers */
    public static final String GNUTELLA_06_200     = "GNUTELLA/0.6 200";
    /** "CONNECT/", part of the first line of the first group of handshake headers*/
    public static final String CONNECT             = "CONNECT/";
    /** "\r\n", the two characters that end each line in the handshake */
    public static final String CRLF                = "\r\n";

    /** 2 minutes, wait this long for the remote computer to respond */
    public static final int USER_INPUT_WAIT_TIME = 2 * 60 * 1000; // 2 minutes in milliseconds
    // TODO:kfaaborg This doesn't seem to be used at all

    /** 5, designed for the authentication handshake feature, not used */
    public static final int MAX_HANDSHAKE_ATTEMPTS = 5;

    /**
     * The time we finished the handshake with this remote computer.
     * 
     * Initialized to Long.MAX_VALUE, the largest value a long can hold, meaning not set yet.
     * The initialize() method sets this to System.currentTimeMillis().
     * This is the number of milliseconds between January 1970 and the moment initialize() ran.
     */
    private long _connectionTime = Long.MAX_VALUE;

    /**
     * 3, the limit for hops + TTL on packets this remote compuer sends us.
     * If the remote computer sends us a message with hops + TTL greater than _softMax, we set the TTL to SOFT_MAX - hops.
     * We do this on a per-connection basis because on newer connections that understand the X-Max-TTL header, we can regulate the TTLs they send us.
     * This helps prevent malicious hosts from using headers like X-Max-TTL to simply get connections.
     * It also makes hosts abide by the contract of the X-Max-TTL header, illustrated by sending lower TTL traffic generally.
     * 
     * Query hit packets are an exception, and can have a larger TTL than this.
     * If the remote computer supports a lot of advanced features, we might allow a maximum of 4 instead of 3.
     * 
     * The initialize() method reads the value directly from settings, like ConnectionSettings.SOFT_MAX.getValue().
     * It doesn't come from the remote computer at all.
     */
    private byte _softMax;

    /*
     * volatile to avoid multiple threads caching old data for the ping time.
     */

    /**
     * The time when we'll next relay a ping packet from this remote computer.
     * 
     * When the remote computer sends us a ping, we'll relay it to more remote computers.
     * If it sends another ping right away, we won't pass it on.
     * If we did, we'd be flooding the network with ping traffic.
     */
    private volatile long _nextPingTime = Long.MIN_VALUE; // Initialize to a very large negative number

    /**
     * The time when we'll next relay a pong packet from this remote computer.
     * 
     * When the remote computer sends us a pong, we'll relay it to more remote computers.
     * If it sends another ping right away, we won't pass it on.
     * If we did, we'd be flooding the network with pong traffic.
     */
    private volatile long _nextPongTime = Long.MIN_VALUE; // Initialize to a very large negative number

    /**
     * The connection closed exception we'll throw if the remote computer closes the connection.
     * 
     * This object is static and created here in the class.
     * This means the whole program will have just one CONNECTION_CLOSED exception object, even if it has a lot of Connection objects.
     * We're doing this for performance.
     * The program won't have to create a new exception every time a remote computer closes a connection.
     */
    protected static final IOException CONNECTION_CLOSED = new IOException("connection closed");

    /**
     * Make a new Connection object that we'll use to try to connect to an IP address and port number.
     * This constructor takes the IP address and port number of the remote computer.
     * It doesn't try to connect to the remote computer here, call initialize() to do that.
     * 
     * @param host            The IP address of the remote computer we're going to try to connect to, in a string like "24.183.177.68"
     * @param port            The port number of the remote computer we're going to try to connect to, like 6346
     * @param requestHeaders  A new UltrapeerHeaders or LeafHeaders object filled with all the default headers we send.
     *                        This will be saved as REQUEST_HEADERS, our stage 1 headers we'll send with "GNUTELLA CONNECT/0.6".
     * @param responseHeaders A new UltrapeerHandshakeResponder or LeafHandshakeResponder object.
     *                        We'll give this HandshakeResponder object the remote computer's stage 2, and it will compose our stage 3
     */
    public Connection(String host, int port, Properties requestHeaders, HandshakeResponder responseHeaders) {

    	// Make sure the caller gave us the IP address and port number of the remote computer we're going to try to connect to
    	// The string host is like "24.183.177.68", and port is like 6346
		if (host == null) throw new NullPointerException("null host");
		if (!NetworkUtils.isValidPort(port)) throw new IllegalArgumentException("illegal port: " + port);

		// Make sure the caller gave us Properties and HandshakeResponder objects, even though both will be blank
		if (requestHeaders  == null) throw new NullPointerException("null request headers");
		if (responseHeaders == null) throw new NullPointerException("null response headers");

		// Save everything in this new Connection object
        _host = host;
        _port = port;
        REQUEST_HEADERS  = requestHeaders;  // Our stage 1 headers we'll send with "GNUTELLA CONNECT/0.6"
        RESPONSE_HEADERS = responseHeaders; // The HandshakeResponder that will read stage 2 and compose stage 3

        // This is an outgoing connection, we know the remote computer's IP address and port number, and are going to try to initiate a connecion to it
        OUTGOING = true;

        // Record that we're about to attempt one more outgoing connection
		ConnectionStat.OUTGOING_CONNECTION_ATTEMPTS.incrementStat();
    }

    /**
     * Make a new Connection object for a remote computer that just connected to us.
     * 
     * @param socket          The connection socket that accept() returned.
     *                        The Acceptor already read "GNUTELLA " from the socket, so the next thing in it will probably be "CONNECT/0.6".
     * @param responseHeaders A new LeafHandshakeResponder or UltrapeerHandshakeResponder object, depending on which we are.
     *                        We'll save this as RESPONSE_HEADERS.
     *                        We'll store the remote computers stage 1 greeting in it, and it will compose our stage 2 response.
     */
    public Connection(Socket socket, HandshakeResponder responseHeaders) {

    	// Make sure none of the parameters are null
		if (socket          == null) throw new NullPointerException("null socket");
		if (responseHeaders == null) throw new NullPointerException("null response headers");

		/*
		 * Don't do a reverse DNS lookup here, because that can block.
		 * On the Mac, doing a reverse DNS lookup will freeze the entire computer.
		 */

		// Get the IP address and port number of the remote computer, and save the socket that connects us
        _host   = socket.getInetAddress().getHostAddress();
        _port   = socket.getPort(); // The ephemeral port the remote computer connected from, not the same as the port it's listening on
        _socket = socket; // The Acceptor already read "GNUTELLA " from the socket

        /*
         * When the remote computer connected to us, it chose a random high-numbered port to start the connection from.
         * This is called the ephemeral port.
         * socket.getPort() above returned the port number on the far end of the connection, the ephemeral port.
         * 
         * The remote computer is also listening for new connections with a listening socket.
         * This socket is totally different, and has a different port number.
         * To find out what port number the remote computer's socket is listening on, we'll have to ask it.
         * 
         * For now, we've saved the remote epehermal port number of the connection socket in _port.
         * This information really isn't useful to us at all.
         * When the remote computer tells us what port it's listening on,
         * we'll call Connection.setListeningPort() to change _port to be the remote computer's listening socket.
         * 
         * TODO:kfaaborg Why are we saving the ephemeral port at all? Why not just leave it 0 until the remote computer tells us its listening socket?
         */

        // This is an incoming connection, the remote computer connected to our TCP listening socket
        OUTGOING = false;

        // Save the given HandshakeResponder object that will hold the remote computer's stage 1 greeting and composes our stage 2 response
        RESPONSE_HEADERS = responseHeaders;	
		REQUEST_HEADERS  = null; // Only used when we initiate the connection

		// Record we accepted one more incoming connection, and will now try the handshake
		ConnectionStat.INCOMING_CONNECTION_ATTEMPTS.incrementStat();
    }

    /**
     * Send the remote computer messages about the vendor-specific messages we understand.
     * ConnectionManager.connectionInitialized() calls this after we've connected to the remote computer and finished the Gnutella handshake.
     */
    protected void postInit() {

        try {

        	// If the remote computer told us "Vendor-Message: 0.1" and the version number is bigger than 0
			if (_headers.supportsVendorMessages() > 0) {

                /*
                 * After two Gnutella computers finish the handshake, they exchange Messages Supported and Capabilities vendor messages.
                 * These vendor messages have the same purpose as the handshake, advertising support for ways to communicate and features.
                 */

                // Send the remote computer our Messages Supported and Capabilities vendor message
                send(MessagesSupportedVendorMessage.instance()); // Tell it which vendor messages we support
                send(CapabilitiesVM.instance());                 // Tell it what advanced and vendor-specific capabilities we have
			}

		// It's OK if that didn't work
        } catch (IOException e) {}
    }

    /**
     * Tell the remote computer about our updated vendor-specific message capabilities.
     */
    protected void sendUpdatedCapabilities() {

        try {

        	// If the remote computer told us "Vendor-Message: 0.1" and the version number is bigger than 0
            if (_headers.supportsVendorMessages() > 0) {

            	// Send information about our current vendor message capabilities
            	send(CapabilitiesVM.instance());
            }

        // It's OK if that didn't work
        } catch (IOException e) {}
    }

    /**
     * Save this remote computer's Messages Supported, Capabilities, and Header Update vendor messages.
     * 
     * This remote computer sent us a vendor message through our TCP socket connection with it.
     * Saves a Messages Supported vendor message in _messagesSupported and a Capabilities vendor message in _capabilities.
     * If it's a Header Update vendor message, adds its headers to HEADERS_READ and _headers.
     * 
     * These messages serve the same purpose as the handshake, advertising support for features and outline how we can communicate.
     * Gnutella computers exchange Messages Supported and Capabilities vendor messages shortly after the handshake.
     * 
     * @param vm The vendor message from the remote computer
     */
    protected void handleVendorMessage(VendorMessage vm) {

    	// The given vendor message is a Messages Supported vendor message object, listing the kinds of vendor messages the remote computer supports
        if (vm instanceof MessagesSupportedVendorMessage) {

        	// Cast it to that type and save it in _messagesSupported
        	_messagesSupported = (MessagesSupportedVendorMessage)vm;
        }

        // The given vendor message is a CapabilitiesVM object, listing the remote computer's capabilities
        if (vm instanceof CapabilitiesVM) {

        	// Cast it to that type and save it in _capabilities
        	_capabilities = (CapabilitiesVM)vm;
        }

        // The given vendor message is a HeaderUpdateVendorMessage object with more handshake headers
        if (vm instanceof HeaderUpdateVendorMessage) {

        	// Cast it to that type
            HeaderUpdateVendorMessage huvm = (HeaderUpdateVendorMessage)vm;

            // Copy all the keys and values from the header update vendor message into HEADERS_READ
            HEADERS_READ.putAll(huvm.getProperties());

            try {

            	/*
            	 * Both HEADERS_READ and _headers are the handshake headers the remote computer sent us.
            	 * HEADERS_READ is a Properties hash table of strings.
            	 * _headers is a HandshakeResponse object, which wraps a Properties and parses the headres to make them easy to read.
            	 * 
            	 * Now that we've changed HEADERS_READ, we have to rewrap and reparse _headers
            	 */

            	// Recreate _headers now that we've edited HEADERS_READ
                _headers = HandshakeResponse.createResponse(HEADERS_READ);

            // It's OK if that didn't work
            } catch (IOException e) {}
        }
    }

    /**
     * Connect to the remote computer, do the Gnutella handshake, and setup compression on the connection.
     * 
     * Code here does 3 important jobs:
     * Connect to the remote computer, going from an IP address and port number to a connection socket.
     * Negotiate the Gnutella handshake with the remote computer.
     * Setup compression on the connection.
     * 
     * @exception IOException           We were unable to connect to the host.
     * @exception NoGnutellaOkException We or the remote computer started a group of handshake headers with a rejection code instead of "200 OK".
     * @exception BadHandshakeException There was some other problem establishing the connection.
     *                                  For instance, the remote computer might close the connection during the handshake.
     *                                  Or, it might respond with HTTP headers instead of Gnutella headers.
     */
    public void initialize() throws IOException, NoGnutellaOkException, BadHandshakeException {

    	// Connect to the remote computer, do the Gnutella handshake, and setup compression on the connection
        initialize(0); // Specify no timeout, the call will block until the connection is made
    }

    /**
     * Connect to the remote computer, do the Gnutella handshake, and setup compression on the connection.
     * 
     * Code here does 3 important jobs:
     * Connect to the remote computer, going from an IP address and port number to a connection socket.
     * Negotiate the Gnutella handshake with the remote computer.
     * Setup compression on the connection.
     * 
     * @param timeout A timeout in milliseconds, like 6 seconds, for trying to connect to the remote computer.
     *                0 for no timeout.
     *                If the platform doesn't support native timeouts, the timeout will be emulated with threads. (do) does nio change this
     * 
     * @exception IOException           We were unable to connect to the host.
     * @exception NoGnutellaOkException We or the remote computer started a group of handshake headers with a rejection code instead of "200 OK".
     * @exception BadHandshakeException There was some other problem establishing the connection.
     *                                  For instance, the remote computer might close the connection during the handshake.
     *                                  Or, it might respond with HTTP headers instead of Gnutella headers.
     */
    public void initialize(int timeout) throws IOException, NoGnutellaOkException, BadHandshakeException {

    	// We made this Connection object to initiate a connection to a remote computer
        if (isOutgoing()) {

        	// Try to open a connection to the remote computer
        	// We have _host and _port, the IP address and port number we want to connect to
        	// We want to get _socket, a new connection socket to the remote computer
        	// This call blocks, execution will hold here until the remote computer accepts our connection
        	_socket = Sockets.connect(_host, _port, timeout); // Or the timeout, 6 seconds, expires
        }

        // While we were waiting for the remote computer to accept our connection, another thread may have called this Connection object's close() method
        // If one did, _closed will be true
        if (_closed) {

        	// Close the connection socket and throw an exception
            _socket.close();
            throw CONNECTION_CLOSED;
        }

        // Make sure we didn't connect to ourself
		InetAddress localAddress = _socket.getLocalAddress(); // Make localAddress our LAN IP address, like 192.168.0.102
        if (ConnectionSettings.LOCAL_IS_PRIVATE.getValue() && // Settings forbid us to connect to other computers here on the same LAN we are on
            _socket.getInetAddress().equals(localAddress) &&  // The address of the far end of the connection socket matches our LAN IP address
            _port == ConnectionSettings.PORT.getValue()) {    // The port number we connected to is also the port we're listening on

        	// This is just a connection to ourself
            throw new IOException("Connection to self");
        }

        // Tell the Acceptor what we now think our address is, it will store it in Acceptor._address
        // Up to now, _address holds the address from InetAddress.getLocalHost()
        // localAddress is the same as InetAddress.getLocalHost, both are just our internal IP address on the LAN
        RouterService.getAcceptor().setAddress(localAddress);
        
        try {

        	// Get streams for reading and writing from the connection socket
            _in  = getInputStream();  // Calls Connection.getOutputStream, the ManagedConnection class doesn't override this method
            _out = getOutputStream(); // This is actually a ManagedConnection object, so this will call ManagedConnection.getOutputStream()
            
            /*
             * At this point, NIOSocket has the following 4 objects:
             * 
             *              - NIOInputStream  - BufferInputStream
             *   NIOSocket -
             *              - NIOOutputStream - BufferOutputStream
             * 
             * getInputStream() finds the BufferInputStream behind the NIOSocket.
             * getOutputStream() finds the BufferOutputStream, and places a new ThrottledOutputStream before it.
             * After the two lines of code above run, _in and _out point to the following object chains:
             * 
             *   _in  - BufferInputStream
             *   _out - ThrottledOutputStream - BufferOutputStream
             */

            // Make sure we were able to find the BufferInputStream and ThrottledOutputStream
            if      (_in  == null) throw new IOException("null input stream");
			else if (_out == null) throw new IOException("null output stream");

        } catch (Exception e) {

        	/*
        	 * Apparently, Socket.getInputStream and Socket.getOutputStream throw NullPointerException if the socket is closed.
        	 * See Sun bug 4091706.
        	 * Unfortunately, the socket may have been closed after the check above, if, for instance, the user pressed disconnect.
        	 * So, we catch NullPointerException here, and any other weird possible exceptions.
        	 * An alternative is to obtain a lock before doing these calls,
        	 * but, we are afraid that getInputStream or getOutputStream may be a blocking operation.
        	 * Just to be safe, we also check that _in and _out are not null.
        	 */

        	// Trying to get the input and output streams caused an exception, close the socket and report error
            close();
            throw new IOException("could not establish connection");
        }

        try {

        	// We connected to the remote computer
        	if (isOutgoing()) {

        		// Do the entire Gnutella handshake, writing to the socket, waiting for a response, reading it, and writing some more
        		// Send our stage 1 headers, read the remote computer's stage 2, and finish with our stage 3
                initializeOutgoing();

            // The remote computer connected to us
        	} else {
        		
        		// Do the entire Gnutella handshake, reading the remote computer's headers, composing a response, writing it, and waiting some more
        		// Read the remote computer's stage 1 headers, compose and send our stage 2, and read the remote computer's stage 3
        		initializeIncoming();
        	}

            // Headers we sent
            _headersWritten = HandshakeResponse.createResponse(HEADERS_WRITTEN);

            // Record the time we finished the handshake
            _connectionTime = System.currentTimeMillis();
            
            /*
             * Set the soft max TTL that should be used on this connection.
             * The +1 on the soft max for good connections is because the message may come from a leaf, and therefore can have an extra hop.
             * Good connections are connections with features such as intra-ultrapeer QRP passing.
             * We give these an extra hop because they might be sending us traffic from their leaves.
             */

            // If this remote computer sends us a packet with hops + TTL > _softMax, we'll lower the TTL to make it compliant
            _softMax = ConnectionSettings.SOFT_MAX.getValue(); // 3, the maximum hops + TTL for packets from this remote computer
            if (isGoodUltrapeer() || isGoodLeaf()) _softMax++; // If the remote computer supports advanced features, use 4 instead

            /*
             * These calls must be delayed until absolutely necessary (here),
             * because the native construction for Deflater and Inflater allocate buffers outside Java's memory heap.
             * This prevents Java from fully knowing when and how to do garbage collection.
             * The call to end(), done explicitly in the close() method of this class,
             * and implicitly in the finalization of the Deflater and Inflater, releases these buffers.
             */

            // We need to compress data before sending it to the remote computer
            if (isWriteDeflated()) { // If _headersWritten includes "Content-Encoding: deflate"

            	// Make a new Java Deflater, the object that will actually compress the data
                _deflater = new Deflater();

                // Returns the given stream unchanged now that LimeWire has switched to NIO
                _out = createDeflatedOutputStream(_out); // Calls ManagedConnection.createDeflatedOutputStream()
            }

            // The remote computer is going to be sending us compressed data, we need to decompress it so we can read it
            if (isReadDeflated()) { // If _headers includes "Content-Encoding: deflate"

            	// Make a new Java Inflater, the object that will actually decompress the data
                _inflater = new Inflater();

                // Returns the given stream unchanged now that LimeWire has switched to NIO
                _in = createInflatedInputStream(_in); // Calls ManagedConnection.createInflatedInputStream()
            }

            /*
             * RESPONSE_HEADERS is our HandshakeResponder object that can read a group of headers and compose a response group.
             * We're done with the handshake, so we don't need it anymore.
             * Set the reference to null to let the garbage collector free it.
             * 
             * This doesn't need to be in a finally clause.
             * If an exception gets thrown before this line runs, the Connection object and all its contents will get garbage collected anyway.
             */

            // We don't need the HandshakeResponder object RESPONSE_HEADERS anymore
            RESPONSE_HEADERS = null;

        // Either we or the remote computer started a group of handshake headers with a rejection code instead of "200 OK"
        } catch (NoGnutellaOkException e) {

        	// Close our connection socket with this remote computer, and pass on the exception
            close();
            throw e;

        // There was some other error communicating with the remote computer
        } catch (IOException e) {

        	// Close our connection socket with this remote computer, and throw a BadHandshakeException instead of the IOException we just caught
            close();
            throw new BadHandshakeException(e);
        }
    }

    /**
     * No longer used.
     * Make a new CompressingOutputStream object that will write data to the given OutputStream object it has compressed with _deflater.
     * 
     * @param out The OutputStream the CompressingOutputStream object this method returns will write compressed data to.
     */
    protected OutputStream createDeflatedOutputStream(OutputStream out) {

    	// Make a new CompressingOutputStream that uses _deflater and then writes to out
        return new CompressingOutputStream(out, _deflater);
    }

    /**
     * No longer used.
     * Make a new UncompressingInputStream object that will read data from the given InputStream and then decompress it with _inflater.
     * 
     * @param in The InputStream the UncompressingInputStream object this method returns will write compressed data to.
     */
    protected InputStream createInflatedInputStream(InputStream in) {

    	// Make a new UncompressingInputStream that reads from in and then uses _inflater
        return new UncompressingInputStream(in, _inflater);
    }

    /**
     * True if _socket is a NIOSocket object we can use without blocking, false if it's a regular Java socket that always blocks.
     * 
     * True if _socket is a LimeWire NIOSocket object.
     * It has a Socket and SocketChannel inside.
     * It will simulate blocking for us when we connect.
     * For message exchange, we can use it without blocking.
     * 
     * False if _socket is just a regular Java socket object.
     * It will always block.
     * 
     * LimeWire is now programmed to use NIOSocket objects instead of regular Java sockets.
     * This method will always return true.
     * 
     * @return Always returns true
     */
    public boolean isAsynchronous() {

    	// If we can get a channel from our connection socket, it's an NIO socket, return true
        return _socket.getChannel() != null;
    }

    /**
     * True if this Connection object has a connection socket, false if _socket is still null.
     * 
     * If the remote computer connected to us,
     * Acceptor got the connection socket and gave it to this Connection object in its constructor.
     * In this case, _socket is never null, and this isInitialized() method will always return true.
     * 
     * If this Connection object is about an outgoing connection to a remote computer, _socket will start out null.
     * The initialize() method here will block to make the connection, and return with a new connection socket.
     * Before initialize() is called, isInitialized() will return false, after it will return true.
     * 
     * Several methods of this class require that we have a connection socket.
     * They can use this method to verify this.
     * 
     * @return True if we have a connection socket for this connection, false if it's still null.
     */
    public boolean isInitialized() {

    	// True if we have a connection socket, false if it's still null
        return _socket != null;
    }

    /**
     * Does the Gnutella handshake with a remote computer we just connected to.
     * Sends our stage 1 headers, then has concludeOutgoingHandshake read their stage 2 and compose our stage 3.
     * If the stages 2 and 3 are "200 OK", this method just returns.
     * If the remote computer rejects us or we look at their stage 2 and decide we don't want to connect, this method throws a NoGnutellaOkException.
     * 
     * @exception NoGnutellaOkException The remote computer rejected or connection in stage 2, or we rejected it in stage 3
     */
    private void initializeOutgoing() throws IOException {

    	// Send our stage 1 group of headers to the remote computer
        writeLine(GNUTELLA_CONNECT_06 + CRLF); // The first line is "GNUTELLA CONNECT/0.6"
        sendHeaders(REQUEST_HEADERS);          // REQUEST_HEADERS is a new UltrapeerHeaders or LeafHeaders object that contains all the headers we send

        // Do stages 2 and 3 of the handshake
        // This involves multiple steps of reading and writing with the remote computer
        concludeOutgoingHandshake();
    }

    /**
     * Finishes doing the Gnutella handshake with a remote computer we just connected to.
     * We already sent our stage 1 headers, now this method reads their stage 2 and composes our stage 3.
     * If the stages 2 and 3 are "200 OK", this method just returns.
     * If the remote computer rejects us or we look at their stage 2 and decide we don't want to connect, this method throws a NoGnutellaOkException.
     * 
     * This method saves the remote computer's stage 2 headers in a local HandshakeResponse object named theirResponse.
     * The object's HandshakeResponder RESPONSE_HEADERS reads those headers and composes our stage 3 headers, a HandshakeResponse object named ourResponse.
     * 
     * @exception NoGnutellaOkException The remote computer rejected or connection in stage 2, or we rejected it in stage 3
     */
    private void concludeOutgoingHandshake() throws IOException {

    	/*
    	 * The Gnutella handshake always has exactly 3 stages.
    	 * 
    	 * This for loop was put in to support a feature that was never finished: user name and password authentication.
    	 * Such a feature might require computers to challenge and respond multiple times, creating handshakes longer than 3 stages.
    	 * 
    	 * The loop is coded to run up to 5 times, but it actually never runs a second time.
    	 * The code within it reads the remote computer's stage 2 headers, and then composes and sends our stage 3 response.
    	 * If either computer says 503, this method throws a NoGnutellaOkException.
    	 * If stages 2 and 3 complete with "200 OK", this method returns.
    	 */

    	// This loop is written to run up to 5 times, but actually, it only runs once
        for (int i = 0; i < MAX_HANDSHAKE_ATTEMPTS; i++) { // Code in this loop will throw an exception or return from the method, control never gets back here

        	/*
        	 * We connected to the remote computer, and sent our stage 1 group of headers.
        	 * Now, the remote computer will respond with stage 2.
        	 */

        	// Read the first line of the remote computer's stage 2 headers
			String connectLine = readLine();                      // Blocks until we've downloaded a whole line
			Assert.that(connectLine != null, "null connectLine"); // Make sure we didn't hit the end of the socket's input stream

			// The remote computer's stage 2 headers don't start with "GNUTELLA/0.6", it must be running something else, like eDonkey or BitTorrent
			if (!connectLine.startsWith(GNUTELLA_06)) {

				// Record the error and leave now
                HandshakingStat.OUTGOING_BAD_CONNECT.incrementStat();
                throw new IOException("Bad connect string");
            }

			// Read the remote computer's stage 2 headers
			readHeaders(Constants.TIMEOUT); // 8 seconds, but doesn't do anything now that we've switched to NIO

			// Wrap the remote computer's stage 2 headers into a HandshakeResponse object, which will parse them and make reading them easy
            HandshakeResponse theirResponse = HandshakeResponse.createRemoteResponse( // theirResponse is stage 2
                    connectLine.substring(GNUTELLA_06.length()).trim(), // The end of the first line of stage 2, like "503 Service unavailable"
                    HEADERS_READ);                                      // The stage 2 headers the remote computer sent us
            _headers = theirResponse;                                   // _headers is all the handshake headers the remote computer sent us
            Assert.that(theirResponse != null, "null theirResponse");   // Make sure that createRemoteResponse was able to make and return a HandshakeResponse object

            // Get the remote computer's stage 2 status code, like 200 or 503
            int code = theirResponse.getStatusCode();

            // The remote computer did not say "200 OK"
            if (code != HandshakeResponse.OK) {

            	// The remote computer said "503 Some error"
                if (code == HandshakeResponse.SLOTS_FULL) {

                	// The remote computer said "User-Agent: LimeWire" and then some version number
                    if (theirResponse.isLimeWire()) {

                    	// The remote computer said "X-Ultrapeer: true", it is an ultrapeer running LimeWire
                    	if (theirResponse.isUltrapeer()) {

                    		// We connected to a LimeWire ultrapeer, and it rejected our connection in stage 2
                    		HandshakingStat.OUTGOING_LIMEWIRE_ULTRAPEER_REJECT.incrementStat();

                    	// The remote computer is a LimeWire leaf
                    	} else {

                    		// We connected to a LimeWire leaf, and it rejected our connection in stage 2
                    		HandshakingStat.OUTGOING_LIMEWIRE_LEAF_REJECT.incrementStat();
                        }

                    // The remote computer isn't running LimeWire, it's running some other Gnutella program
                    } else {

                    	// The remote computer said "X-Ultrapeer: true", it's running BearShare or something as an ultrapeer
                    	if (theirResponse.isUltrapeer()) {

                    		// We connected to an ultrapeer running something other than LimeWire, and it rejected our connection in stage 2
                    		HandshakingStat.OUTGOING_OTHER_ULTRAPEER_REJECT.incrementStat();

                    	// The remote computer said "X-Ultrapeer: false", it's a leaf running some other brand of Gnutella program
                    	} else {

                    		// We connected to a remote leaf running something other than LimeWire, and it rejected our connection in stage 2
                    		HandshakingStat.OUTGOING_OTHER_LEAF_REJECT.incrementStat();
                        }                            
                    }

                    // We connected, sent stage 1, and the remote computer refused with stage 2
                    // Throw the NoGnutellaOkException with 503 stored inside it
                    throw NoGnutellaOkException.SERVER_REJECT;

                    /*
                     * The initialize() method called concludeOutgoingHandshake, and will catch this exception.
                     * It will close the socket connection, and throw the NoGnutellaOkException again.
                     */

                // The remote computer didn't say 200 or 503
                // This is very unusual, Gnutella connections are accepted with 200 or rejected with 503
                } else {

                	// Count the statistic and throw a NoGnutellaOkException with the strange status code the remote computer sent us
                	HandshakingStat.OUTGOING_SERVER_UNKNOWN.incrementStat();
                    throw NoGnutellaOkException.createServerUnknown(code); // The code number that isn't 200 or 503
                }
            }

            // Use the HandshakeResponder object RESPONSE_HEADERS to read the remote computer's stage 2 headers and compose our stage 3 headers
            // Returns a HandshakeResponse object named ourResponse that contains our stage 3 response
			Assert.that(RESPONSE_HEADERS != null, "null RESPONSE_HEADERS");			
            HandshakeResponse ourResponse = RESPONSE_HEADERS.respond( // ourResponse is stage 3
                theirResponse, // The remote computer's stage 2 headers
                true);         // We connected to the remote computer
            Assert.that(ourResponse != null, "null ourResponse");

            // Send our stage 3 headers to the remote computer
            writeLine(GNUTELLA_06 + " " + ourResponse.getStatusLine() + CRLF); // First line like "GNUTELLA/0.6 200 OK"
            sendHeaders(ourResponse.props());                                  // Send the headers on each line
            
            // Read what status code we sent in our own group of stage 3 headers
            code = ourResponse.getStatusCode();

            /*
             * We connected to the remote computer and sent our stage 1 headers.
             * The remote computer replied with its stage 2 headers, which we wrapped into the HandshakeResponse object named theirResponse.
             * We called stage3 = RESPONSE_HEADERS.respond(stage2, outgoing), using a HandshakeResponder object to read stage 2 and compose stage 3.
             */

            // We started stage 3 with a status code of 200
            if (code == HandshakeResponse.OK) {

            	// After that, we said "OK"
                if (HandshakeResponse.OK_MESSAGE.equals(ourResponse.getStatusMessage())) {

                	// Count another successful handshake on a connection we initiated, and leave this method, we're done
                    HandshakingStat.SUCCESSFUL_OUTGOING.incrementStat();
                    return; // If there had been an error, we would have thrown an exception instead of just returning

                // We said 200, but then status text different from "OK"
                } else {
                	
                	/*
                	 * LimeWire has code to support a feature that was never finished: authenticated connections.
                	 * The idea is that two computers would exchange user names and passwords during the handshake.
                	 * They could then sign themselves into private groups on the Gnutella network.
                	 * This stage 3 status line of "200 AUTHENTICATING" is for that feature.
                	 * Gnutella handshakes always have exactly 3 stages, but with authenticated connections, there might be more.
                	 * This is why the continue below will go back to the top of the for loop.
                	 * 
                	 * TODO:kfaaborg We should remove the code for this feature.
                	 */
                	
                	// We may have written "200 AUTHENTICATING"
                    continue;
                }

            // We started stage 3 with something else, like "503 Service unavailable"
            } else {

            	// We sent a response code of 503, the Gnutella rejection code
                if (code == HandshakeResponse.SLOTS_FULL) {

                	// We connected to the remote computer, read its stage 2 headers, and then rejected it with our stage 3
                    HandshakingStat.OUTGOING_CLIENT_REJECT.incrementStat();
                    throw NoGnutellaOkException.CLIENT_REJECT;

                // We sent a response code of 577
                } else if (code == HandshakeResponse.LOCALE_NO_MATCH) {

                	// We don't want to connect because our "X-Locale-Pref" headers indicate languages that don't match
                	// This feature, locale preferencing, is turned off by default, so this won't happen
                    throw NoGnutellaOkException.CLIENT_REJECT_LOCALE;

                // We started stage 3 with some other rejection code
                } else {

                	// Record that we rejected one more computer with our unknown status code in our stage 3 headers after we first connected to it
                    HandshakingStat.OUTGOING_CLIENT_UNKNOWN.incrementStat();
                    throw NoGnutellaOkException.createClientUnknown(code); // Save the strange code we sent in the exception object
                }
            }
        }
        
        // The code in the for loop above ran 5 times without returning or throwing a method.
        // This should never actually happen, since the handshake is always 3 stages and all 3 are handled the first time the loop runs.
        throw NoGnutellaOkException.UNRESOLVED_SERVER;
    }

    /**
     * Does the Gnutella handshake with a remote computer that just connected to us.
     * Reads stage 1 headers and has concludeIncomingHandshake do stages 2 and 3.
     * 
     * @exception NoGnutellaOkException We rejected the connection in stage 2, or the remote computer rejected it in stage 3
     */
    private void initializeIncoming() throws IOException {

    	/*
    	 * The remote computer connected to us and its stage 1 headers are waiting on our end of the socket for us to read them.
    	 * The Acceptor already read "GNUTELLA " from the socket to figure out what this remote computer wanted, so "CONNECT/0.6\r\n" will be next.
    	 */

    	// Read the rest of the remote computer's stage 1 greeting line
		String connectString = readLine();

		// It's "CONNECT/0.6"
        if (notLessThan06(connectString)) {

        	// Read the remote computer's stage 1 headers into READ_HEADERS and _headers
            readHeaders();

            // Send it our stage 2 headers, and read its stage 3
            concludeIncomingHandshake();

        // The remote computer said "GNUTELLA ", but then something other than "CONNECT/0.6"
        } else { throw new IOException("Unexpected connect string: " + connectString); } // Could be an ancient 0.4 host, or just something very confused
    }

    /**
     * Finishes doing the Gnutella handshake with a remote computer that just connected to us.
     * We already read stage 1 headers, now this method composes our stage 2 response and reads the remote computer's stage 3.
     * 
     * @exception NoGnutellaOkException We rejected the connection in stage 2, or the remote computer rejected it in stage 3
     */
    private void concludeIncomingHandshake() throws IOException {

        // This loop never runs a second time, the handshake finishes the first time through
        for (int i = 0; i < MAX_HANDSHAKE_ATTEMPTS; i++) {

        	// This is the crawler contacting us, it said "Crawler: 0.1"
			boolean isCrawler = _headers.isCrawler();
			
			/*
			 * In the following code, it appears that we're ignoring the response code written by the initiator of the connection.
			 * However, you can prove that the last code was always 200 OK.
			 * See initializeIncoming and the code at the bottom of this loop.
			 */
			
			// Have our HandshakeResponder object RESPONSE_HEADERS take the remote computer's stage 1 headers and compose our stage 2 response
			HandshakeResponse ourResponse = RESPONSE_HEADERS.respond( // ourResponse is stage 2
			    _headers, // The stage 1 headers the remote computer sent us when it connected to us
			    false);   // The remote computer connected to us
			
			// Send the remote computer our stage 2 response
            writeLine(GNUTELLA_06 + " " + ourResponse.getStatusLine() + CRLF); // First line like "GNUTELLA/0.6 200 OK\r\n"
            sendHeaders(ourResponse.props());                                  // The headers after that
            
            // The crawler connected to us, and we told it our statistics in our stage 2 response
            if (isCrawler) {

            	// Read the crawler's single line stage 3 confirmation, and throw an exception to leave here and disconnect
                readLine();
                throw new IOException("crawler");
            }

            // Find out what the HandshakeResponder made the stage 2 response that we sent say
            int code = ourResponse.getStatusCode();

            // We rejected the connection
            if (code != HandshakeResponse.OK) { // Our stage 2 headers start with something other than "200 OK"

            	// We rejected the remote computer with 503
                if (code == HandshakeResponse.SLOTS_FULL) {

                	// A remote computer connected to us, and we rejected it with stage 2 headers that said 503
                    HandshakingStat.INCOMING_CLIENT_REJECT.incrementStat();
                    throw NoGnutellaOkException.CLIENT_REJECT;

                // We rejected the remote computer with some other number
                } else {

                	// A remote computer connected to us, and we rejected it with stage 2 headers that had an unusual status code
                    HandshakingStat.INCOMING_CLIENT_UNKNOWN.incrementStat();
                    throw NoGnutellaOkException.createClientUnknown(code);
                }
            }

            // Read the remote computer's stage 3 response
            String connectLine = readLine(); // The first line of the headers, like "GNUTELLA/0.6 200 OK"
            readHeaders();                   // _headers and HEADERS_READ are the same, and contain stages 1 and 3 combined

            // The remote computer's stage 3 headers start with something other than "GNUTELLA/0.6"
            if (!connectLine.startsWith(GNUTELLA_06)) {

            	// The remote computer connected to us, we sent stage 2, then they sent stage 3 that didn't start "GNUTELLA/0.6"
                HandshakingStat.INCOMING_BAD_CONNECT.incrementStat();
                throw new IOException("Bad connect string");
            }

            // Wrap and parse the remote computer's headers from stages 1 and 3 combined into a HandshakeResponse object named theirResponse
            HandshakeResponse theirResponse = HandshakeResponse.createRemoteResponse(
                connectLine.substring(GNUTELLA_06.length()).trim(), // The text after "GNUTELLA/0.6", like "200 OK", with spaces trimed
                HEADERS_READ);                                      // readHeaders() loaded stages 1 and 3 into HEADERS_READ

            // If we said "200 OK" in our stage 2 headers and the remote computer said "200 OK" in its stage 3 headers
            // TODO:kfaaborg We already checked for our stage 2 OK, all this needs to do is confirm the remote computer's stage 3 OK
            if (ourResponse.getStatusCode() == HandshakeResponse.OK && theirResponse.getStatusCode() == HandshakeResponse.OK) {

            	// We've successfully finished shaking hands with a remote computer that contacted us
                HandshakingStat.SUCCESSFUL_INCOMING.incrementStat();
                return; // Return normally, if a computer had rejected the connection we would have thrown an exception
            }

            // We sent a stage 2 OK, but the remote computer replied with a stage 3 refusal
			HandshakingStat.INCOMING_SERVER_UNKNOWN.incrementStat();
            throw NoGnutellaOkException.createServerUnknown(theirResponse.getStatusCode());
        }

        // The code in the for loop above ran 5 times without returning or throwing a method.
        // This should never actually happen, since the handshake is always 3 stages and all 3 are handled the first time the loop runs.
        HandshakingStat.INCOMING_NO_CONCLUSION.incrementStat();
        throw NoGnutellaOkException.UNRESOLVED_CLIENT;
    }

    /**
     * Takes text like "CONNECT/0.6", and returns true if it ends "0.6", the version number of the Gnutella protocol in use.
     * 
     * There have only been two versions of the Gnutella protocol.
     * The first version 0.4, is no longer in use.
     * It would be very unusual to encouter a Gnutella 0.4 computer on the Internet, and if we found one, we would not connect to it.
     * 
     * The only other version is the current one, 0.6.
     * This method will understand higher or different number, but that's not really necessary.
     * All it really needs to look for is the text "CONNECT/0.6".
     * 
     * @param line Text like "CONNECT/0.6"
     * @return     True if the number after the slash is 0.6 or higher
     */
    private static boolean notLessThan06(String line) {

    	// Find how far into the line the text "CONNECT/" appears
        int i = line.indexOf(CONNECT);
        if (i < 0) return false;       // Not found

        try {

        	// Clip out the text after the slash, and read it as a floating point number
            Float F = new Float(line.substring(i + CONNECT.length()));
            float f = F.floatValue();

            // Return true if it's 0.6 or higher
            return f >= 0.6f;

        // Reading the text as a number may have caused an exception
        } catch (NumberFormatException e) { return false; } // Report that we didn't find the 0.6 we were looking for
    }

    /**
     * Sends a group of headers in a Properties hash table to the remote computer, including the blank line that ends the group.
     * If you give this null instead of a Properties hash table, it just sends a blank line.
     * 
     * @param props The Properites hash table of strings with keys like "Header-Name" and values like "some, value"
     */
    private void sendHeaders(Properties props) throws IOException {

    	// Only look in the Properties hash table of strings if the caller gave us one
        if (props != null) {

        	// Loop for each key in the hash table
            Enumeration names = props.propertyNames();
            while (names.hasMoreElements()) {

            	// Get one key and its value from the Properties hash table
                String key   = (String)names.nextElement();
                String value = props.getProperty(key);

                // Overwrite any domainname with true IP address
                if (HeaderNames.REMOTE_IP.equals(key)) value=getInetAddress().getHostAddress();
                if (value == null) value = "";

                // Send this header as a line of ASCII text like "Header-Name: some, value\r\n"
                writeLine(key + ": " + value + CRLF); // The method name writeLine sounds like it writes the ASCII text and then "\r\n", but it doesn't

                // Add it to our record of the headers we've sent this remote computer
                HEADERS_WRITTEN.put(key, value);
            }
        }

        // End the group of headers with a blank line, just write "\r\n" to the remote computer
        writeLine(CRLF); // This just writes one "\r\n", not two
    }

    /**
     * Reads a group of headers from the remote computer.
     * Adds each header to the Properties hash table of strings named HEADERS_READ.
     * Then, it wraps the group into the HandshakeResponse object _headers.
     * 
     * If you call readHeaders() a second time, it will add more headers to HEADERS_READ, but reset _headers to the one group of headers it reads.
     */
    private void readHeaders() throws IOException {

    	// Read the next group of headers from the remote computer into the HEADERS_READ Properties hash table of strings.
        readHeaders(Constants.TIMEOUT); // 8 seconds, but doesn't work now that we've switched to NIO

        // Wrap the group of headers into the Connection object's HandshakeResponse object named _headers
        _headers = HandshakeResponse.createResponse(HEADERS_READ);
    }

    /**
     * Reads a group of headers from the remote computer into this Connection object's HEADERS_READ Properties hash table of strings.
     * 
     * Each header is a line of ASCII text characters that end with "\r\n", like "Header-Name: some, value\r\n".
     * A group of headers ends with a blank line.
     * In the Properties hash table, the keys are like "Header-Name" and the values are like "some, value"
     * 
     * @param timeout This timeout doesn't do anything now that we've switched to NIO
     */
    private void readHeaders(int timeout) throws IOException {

    	/*
    	 * TODO: Limit the number of headers read
    	 */

    	// Loop to read each line in the group of headers, stopping when a blank line indicates the end of the group
        while (true) {

        	// Read a line of text from the remote computer
            String line = readLine(timeout); // Doesn't distinguish between \r and \n
            if (line == null) throw new IOException("unexpected end of file"); // Reading the input stream returned -1

            // A group of Gnutella handshake headers ends with a blank line
            if (line.equals("")) return; // We just read a blank line, so we're done with this group of handshake headers

            // The line looks like "X-Header-Name: some, values", find where the colon is in the middle
            int i = line.indexOf(':');
            if (i < 0) continue; // There is no colon in this line, ignore the entire line

            // Split the line into the key "X-Header-Name" and value "some, value" strings
            String key   = line.substring(0, i);         // From the start up to the ":"
            String value = line.substring(i + 1).trim(); // From beyond the ":" to the end, trimming spaces off the start and end

            // This is the "Remote-IP" header, the remote computer is telling us what our IP address really is
            if (HeaderNames.REMOTE_IP.equals(key)) changeAddress(value); // Save this information in settings

            // Add key and value we just parsed to the Connection object's record of every header the remote computer sent us
            HEADERS_READ.put(key, value);
        }
    }

    /**
     * The readHeaders() method calls changeAddress() as soon as the remote computer tells us our real Internet IP address with a header like "Remote-IP: 216.27.178.74".
     * Saves it in settings or Acceptor._address, and also in Acceptor._externalAddress.
     * 
     * @param The value of the "Remote-IP" header, like "216.27.178.74".
     */
    private void changeAddress(final String v) {

    	// Convert the string like "216.27.178.74" into an InetAddress object
    	InetAddress ia = null;
        try { ia = InetAddress.getByName(v); } catch(UnknownHostException uhe) { return; } // Error converting it, don't keep it

        // If the IP address starts 0 or 255, or is in a range of LAN IP addresses, return without saving the address
        if (!NetworkUtils.isValidAddress(ia) || NetworkUtils.isPrivateAddress(ia)) return;

        // If we're supposed to save our IP address in settings
        if (ConnectionSettings.FORCE_IP_ADDRESS.getValue()) {

        	// If the caller gave us a different IP address than that one we've saved in settings
        	StringSetting addr = ConnectionSettings.FORCED_IP_ADDRESS_STRING;
            if (!v.equals(addr.getValue())) {

            	// Save the new one in settings and tell the router service our record of our IP address changed
                addr.setValue(v);
                RouterService.addressChanged();
            }

        // We're not supposed to save our Internet address in settings, and RouterService.getAddress is return a LAN address
        } else if (!NetworkUtils.isValidAddress(RouterService.getAddress())) {
        	
        	// Save this as our address instead
            RouterService.getAcceptor().setAddress(ia); // This will call RouterService.addressChanged() if necessary
        }

        // Save our real Internet IP address in Acceptor._externalAddress
        RouterService.getAcceptor().setExternalAddress(ia);
    }

    /**
     * Sends the given text to the remote computer as ASCII characters, and doesn't write "\r\n" afterwards.
     * 
     * TODO:kfaaborg The name is very misleading, writeLine usually means it also writes "\r\n".
     */
    private void writeLine(String s) throws IOException {

    	// Make sure the string isn't null or blank
        if (s == null || s.equals("")) throw new NullPointerException("null or empty string: " + s);

        // Convert the text in the string into an array of ASCII bytes
        byte[] bytes = s.getBytes(); // Uses the default character encoding
        // TODO:kfaaborg Make the character encoding here explicit

        // Count these bytes as upload bandwidth done for a Gnutella header
		BandwidthStat.GNUTELLA_HEADER_UPSTREAM_BANDWIDTH.addData(bytes.length);

		// Send the bytes to the remote computer
        _out.write(bytes);
        _out.flush(); // Tell the object to actually send everything it's holding to the remote computer right now
    }

    /**
     * Reads and returns one line of text the remote computer sent us.
     * 
     * @return A string that contains the line of text we downloaded from the remote computer
     */
    private String readLine() throws IOException {

    	// Read and return one line of text the remote computer sent us
        return readLine(Constants.TIMEOUT); // Now that we've switched to NIO, this timeout no longer works
    }

    /**
     * Reads and returns one line of text the remote computer sent us.
     * 
     * This method uses the program's ByteReader.readLine() method.
     * It interprets a line as a sequence of bytes up to the first \n, with all the \r bytes in it removed.
     * 
     * @param timeout Now that we've switched to NIO, this timeout doesn't work anymore
     * @return        A string that contains the line of text we downloaded from the remote computer
     */
    private String readLine(int timeout) throws IOException {

    	// Get the socket's timeout so we can change it and then put it back the way it was
        int oldTimeout = _socket.getSoTimeout(); // Doesn't actually do anything because _socket is actually a NIOSocket object that uses a different socket

        try {

        	/*
        	 * Change the timeout to 8 seconds.
        	 * We'll call read() and it will block, waiting for the remote computer to send us something.
        	 * If the remote computer never sends us anything, after 8 seconds read() will throw a java.io.InterruptedIOException.
        	 * The socket is still valid if this happens.
        	 * 
        	 * TODO: This is a silent bug, _socket is actually an NIO socket, which extends Socket but doesn't use it.
        	 * This call will set the timeout on the socket we never use.
        	 * It doesn't really matter, because in NIO sockets don't block, so they don't have timeouts either.
        	 */

        	// Configure the socket so if read() blocks for 8 seconds without any data downloaded, it will throw an InterruptedIOException
            _socket.setSoTimeout(timeout); // Doesn't actually do anything because _socket is actually a NIOSocket object that uses a different socket

            // Wrap a new ByteReader around the input stream, and use it to read a line
            String line = (new ByteReader(_in)).readLine(); // Blocks until we download the whole line

            // If the ByteReader called read() on the input stream and got -1, we reached the end of the stream
            if (line == null) throw new IOException("read null line");

            // Add this to our count of bytes downloaded doing the Gnutella handshake
            BandwidthStat.GNUTELLA_HEADER_DOWNSTREAM_BANDWIDTH.addData(line.length());

            // Return the line the remote computer sent us
            return line;

        // If another thread or the remote computer closes the connection while we're reading the bytes of a line, we'll get a NullPointerException
        } catch (NullPointerException npe) {

        	// Throw the IOException this class keeps to indicate the connection was closed
            throw CONNECTION_CLOSED;

        } finally {

            // Put back the socket's timeout the way it was before we set it to 8 seconds to read the line
            _socket.setSoTimeout(oldTimeout); // Doesn't actually do anything because _socket is actually a NIOSocket object that uses a different socket
        }
    }

    /**
     * Returns the stream you can use to send data to this remote computer.
     * 
     * This is actually a ManagedConnection object.
     * When initialize() calls getOutputStream(), control goes to ManagedConnection.getOutputStream() and then that calls here.
     * 
     * The _socket object this Connection uses to communicate with the remote computer isn't a java.net.Socket object.
     * It's a LimeWire NIOSocket object, which presents the same interface as Socket but uses NIO underneath.
     * 
     * @return The OutputStream you can use to send data to the remote computer
     */
    protected OutputStream getOutputStream() throws IOException {

    	// The connection socket in this object is a NIO socket that has channels and doesn't block
        if (isAsynchronous()) {

        	/*
        	 * The connection socket is a LimeWire NIOSocket object.
        	 * We don't need to buffer it, because the NIOSocket object buffers it internally.
        	 * Buffering it wouldn't be wrong, though, since we can always flush the buffered data.
        	 */

        	// Return the object you can write to
            return _socket.getOutputStream();

        // Not used now that LimeWire has switched to NIO
        } else {

        	/*
        	 * The connection socket is a regular java.net.Socket object.
        	 * We have to buffer it ourselves.
        	 * Do this by wrapping a Java BufferedOutputStream object around it.
        	 */

        	// Get the socket's OutputStream, wrap it inside a Java BufferedOutputStream, and return that
            return new BufferedOutputStream(_socket.getOutputStream());
        }
    }

    /**
     * Returns the stream you can use to read data from this remote computer.
     * 
     * The _socket object this Connection uses to communicate with the remote computer might be one object or another.
     * It might be a regular java.net.Socket object, which has streams that block.
     * Or, it might be a new LimeWire NIOSocket object, which presents the same interface but uses NIO underneath.
     * This method returns a buffered InputStream you can write to either way.
     * 
     * @return The InputStream you can use to read data from the remote computer
     */
    protected InputStream getInputStream() throws IOException {
    	
    	// The connection socket in this object is a NIO socket that has channels and doesn't block
        if (isAsynchronous()) {

        	/*
        	 * The connection socket is a LimeWire NIOSocket object.
        	 * We don't need to buffer it, because the NIOSocket object buffers it internally.
        	 * 
        	 * Here's another reason not to wrap a BufferedInputStream object around this stream.
        	 * We do the handshake with blocking calls, but then switch to asynchronous NIO messaging for Gnutella packets.
        	 * If we had a BufferedInputStream between us and the NIOSocket, it might contain some data when we stopped using it.
        	 * We would have to take extra steps to make sure we got all the data out of it.
        	 */

        	// Return the object you can read from
            return _socket.getInputStream();

        // Not used now that LimeWire has switched to NIO
        } else {

        	/*
        	 * The connection socket is a regular java.net.Socket object.
        	 * We have to buffer it ourselves.
        	 * Do this by wrapping a Java BufferedInputStream object around it.
        	 */

        	// Get the socket's InputStream, wrap it inside a Java BufferedInputStream, and return that
            return new BufferedInputStream(_socket.getInputStream());
        }
    }

    /**
     * True if we started with the remote computer's IP address, false if it connected to our listening socket.
     * The ReplyHandler interface requires this method.
     * 
     * @return True if we initiated the connection to the remote computer, false if it connected to us
     */
    public boolean isOutgoing() {

    	// Return true if we started with the remote computer's IP address, false if it connected to our listening socket
        return OUTGOING;
    }

    /**
     * Not used now that LimeWire has switched to NIO.
     * 
     * Reads one Gnutella packet from the remote computer and returns it.
     * Sets up decompression if it hasn't already been setup yet.
     * Blocks here until the remote computer sends us a packet.
     * 
     * This works just like Message.read(), except it blocks until a Gnutella packet is available.
     * If the remote computer sends us half a packet, readAndUpdateStatistics will throw an IOException. (do)
     * 
     * This method is not thread-safe.
     * Its behavior is undefined if two threads are in a receive call at the same time for a given connection.
     * 
     * @return A Gnutella packet from this remote computer as a Message object
     */
    protected Message receive() throws IOException, BadPacketException {

    	// If we need to setup decompression and haven't yet, do it now
        if (isAsynchronous() &&                           // The connection socket here is actually an NIOSocket object that is simulating blocking for us, and
            isReadDeflated() &&                           // The remote computer told us "Content-Encoding: deflate", it will be sending compressed data, and
            !(_in instanceof UncompressingInputStream)) { // We haven't added an UncompressingInputStream to the read chain yet

        	// Make a new UncompressingInputStream object
            _in = new UncompressingInputStream( // Save it in place of our old source so we'll read through it
                _in,                            // Have it write to the input stream we were writing to before
                _inflater);                     // Give it the Inflater object that can actually decompress data
        }

        /*
         * On the Macintosh, sockets appear to return the same ping reply repeatedly if the connection has been closed remotely.
         * This prevents connections from dying.
         * The following works around the problem.
         * Note that Message.read may still throw IOException below.
         * See note on _closed for more information.
         */

        // We've already called close() on this Connection object
        if (_closed) throw CONNECTION_CLOSED; // Throw the CONNECTION_CLOSED IOException, used for when the remote computer closes the connection

        // Keep calling readAndUpdateStatistics() until it actually gives us a Gnutella packet, and return it
        Message m = null;
        while (m == null) { m = readAndUpdateStatistics(); }
        return m;
    }

    /**
     * Not used now that LimeWire has switched to NIO.
     * 
     * Reads one Gnutella packet from the remote computer and returns it.
     * Sets up decompression if it hasn't already been setup yet.
     * Blocks here until the remote computer sends us a packet.
     * 
     * This works just like Message.read(), except it blocks until a Gnutella packet is available.
     * If the remote computer sends us half a packet, throws an InterruptedIOException.
     * In this case, you should terminate the connection, as half a message may have been read.
     * 
     * This method is not thread-safe.
     * Its behavior is undefined if two threads are in a receive call at the same time for a given connection.
     * 
     * @return A Gnutella packet from this remote computer as a Message object
     */
    public Message receive(int timeout) throws IOException, BadPacketException, InterruptedIOException {

    	// If we need to setup decompression and haven't yet, do it now
        if (isAsynchronous() &&                           // The connection socket here is actually an NIO socket that is simulating blocking for us, and
        	isReadDeflated() &&                           // The remote computer told us "Content-Encoding: deflate", it will be sending compressed data, and
        	!(_in instanceof UncompressingInputStream)) { // We haven't added an UncompressingInputStream to the read chain yet

        	// Make a new UncompressingInputStream object
            _in = new UncompressingInputStream( // Save it in place of our old source so we'll read through it
            	_in,                            // Have it write to the input stream we were writing to before
            	_inflater);                     // Give it the Inflater object that can actually decompress data
        }

        /*
         * On the Macintosh, sockets appear to return the same ping reply repeatedly if the connection has been closed remotely.
         * This prevents connections from dying.
         * The following works around the problem.
         * Note that Message.read may still throw IOException below.
         * See note on _closed for more information.
         */

        // We've already called close() on this Connection object
        if (_closed) throw CONNECTION_CLOSED; // Throw the CONNECTION_CLOSED IOException, used for when the remote computer closes the connection
        
        /*
         * TODO:kfaaborg _socket is a LimeWire NIO socket.
         * The socket it uses is the socket inside it, not the socket it is.
         * Calling getSoTimeout() and setSoTimeout() affect the socket it is, which we're not using.
         * 
         * To fix this, see if it's an NIOSocket, cast it as an NIOSocket, and then call setSoTimeout() on it.
         * NIOSocket does override setSoTimeout and direct the call to the socket inside.
         */

        // Temporarily change the socket timeout
        int oldTimeout = _socket.getSoTimeout(); // Get the socket's current timeout, and save it
        _socket.setSoTimeout(timeout);           // Give the socket a new timeout, the value in milliseconds the caller passed this method

        try {

        	// Call readAndUpdateStatistics() to get one Gnutella packet
            Message m = readAndUpdateStatistics();
            if (m == null) { throw new InterruptedIOException("null message read"); } // The remote computer didn't send us one, throw an exception
            return m; // The remote computer did send us one, return it

        } finally {

        	// Put the socket's timeout back the way we found it
            _socket.setSoTimeout(oldTimeout);
        }
    }

    /**
     * A 23-byte buffer that holds a Gnutella packet header.
     * HEADER_BUF lets Message.read in readAndUpdateStatistics read faster.
     */
    private final byte[] HEADER_BUF = new byte[23];

    /**
     * Not used now that LimeWire has switched to NIO.
     * 
     * Read a Gnutella packet from this remote computer and return it.
     * Also updates our statistics about how much data we've downloaded from this remote computer, before and after compression.
     * 
     * @return A Gnutella packet as a Message object
     */
    private Message readAndUpdateStatistics() throws IOException, BadPacketException {

        // The try/catch block is necessary for two reasons...
        // See the notes in Connection.close above the calls
        // to end() on the Inflater/Deflater and close()
        // on the Input/OutputStreams for the details.
    	// TODO:kfaaborg This doesn't make any sense, there is no try catch block here

    	// Read a Gnutella packet from the remote computer
        Message msg = Message.read(
        	_in,           // The InputStream the Message.read method can get data from
        	HEADER_BUF,    // A 23-byte buffer that can hold a Gnutella packet header
        	Message.N_TCP, // The network int, 1 for TCP, 2 for UDP
        	_softMax);     // The hops + TTL limit we're imposing on packets this remote computer sends us

        // Ask the Inflater object how much it's processed, and update _compressedBytesReceived and _bytesReceived
        updateReadStatistics(msg); // If the remote computer isn't sending compressed data, updateReadStatistics needs the message to measure how big it is

        // Return the Gnutella packet we just read from the remote computer
        return msg;
    }

    /**
     * Asks the Inflater how much data it's processed to update _compressedBytesReceived and _bytesReceived.
     * 
     * Asks the Inflater how many bytes of compressed data we've given it, and how many bytes of normal data it's produced.
     * Tells the compression statistics object how much bigger both of these counts are since the last time we checked.
     * Saves the new totals in _compressedBytesReceived and _bytesReceived.
     * 
     * If the remote computer isn't sending us compressed data, this method needs the Gnutella packet it's given.
     * There is no Inflater to get counts from, so it measures the size of the packet and adds it to _bytesReceived.
     * 
     * @param msg A Gnutella packet that we've just read from the remote computer
     */
    protected void updateReadStatistics(Message msg) throws IOException {

    	/*
    	 * _bytesReceived must be set differently when compressed because the inflater will read more input than a single message,
    	 * making it appear as if the deflated input was actually larger.
    	 */

    	// If the remote computer has been sending compressed data, and we've been decompressing it
        if (isReadDeflated()) {

            try {
            	
            	// Ask the inflater the total amount of compressed data we've given it, and the total amount of decompressed data it's produced
            	long newIn  = _inflater.getTotalIn();  // Number of bytes of compressed data we've downloaded from the remote computer and given to the inflater
                long newOut = _inflater.getTotalOut(); // Number of bytes of normal data the inflater has produced

                // Both numbers will be bigger than they were the last time we checked, add the amount they've grown to the compression statistics
                CompressionStat.GNUTELLA_UNCOMPRESSED_DOWNSTREAM.addData((int)(newOut - _bytesReceived));
                CompressionStat.GNUTELLA_COMPRESSED_DOWNSTREAM.addData((int)(newIn - _compressedBytesReceived));

                // Save the new totals
                _compressedBytesReceived = newIn;  // The number of bytes of compressed data we've read from this remote computer
                _bytesReceived           = newOut; // The size of the data after we decompressed it

            // If the Inflater was ended at an odd time, it can throw a NullPointerException
            } catch (NullPointerException npe) {

            	// Throw the CONNECTION_CLOSED IOException, used when the remote computer closes the connection (do)
                throw CONNECTION_CLOSED;
            }

        // The remote computer is sending us normal data that doesn't need to be compressed
        } else if (msg != null) {

        	// Just add the size of this message to our count of how much data we've downloaded from this remote computer
        	_bytesReceived += msg.getTotalLength();
        }
    }

    /**
     * A 23-byte buffer that holds a Gnutella packet header.
     * OUT_HEADER_BUF lets the Message.write method write faster.
     * We can reuse this buffer because only one thread will send the message at a time.
     */
    private final byte[] OUT_HEADER_BUF = new byte[23];

    /**
     * Not used.
     * ManagedConnection overrides this method and never calls it.
     * 
     * Send a Gnutella packet to this remote computer.
     * This also updates statistics, just like readAndUpdateStatistics does.
     * 
     * The write stream may be buffered, so call flush() to guarantee that the message is actually sent.
     * 
     * This method is not thread safe.
     * Behavior is undefined if two threads are in a send call at the same time for a given connection.
     * 
     * @param m A Gnutella packet as a Message object to send to this remote computer
     */
    public void send(Message m) throws IOException {

    	// If we're using our Apache debug log to keep track of this class
        if (LOG.isTraceEnabled()) {

        	// Compose text like "Connection (CONNECTION: host=216.27.178.74 port=6346) is sending message: " and then the message turned into a string (do)
        	LOG.trace("Connection (" + toString() + ") is sending message: " + m);
        }

        /*
         * The try/catch block is necessary for two reasons.
         * For the details, see the notes in Connection.close above the calls to end() on the Inflater/Deflater.
         * Also, see close() on the Input/OutputStreams.
         */

        try {

        	// Send the message to the remote computer
            m.write(_out, OUT_HEADER_BUF); // The write method can go faster with the 23-byte OUT_HEADER_BUF to use
            updateWriteStatistics(m);      // Add the size of this message, before and after compression, to our upload statistics for this remote computer

        // Write uses the Deflater, which may throw a NullPointerException
        } catch(NullPointerException e) {

        	// We shut down the Deflater because the connection is closed, and the Deflater threw a NullPointerException
            throw CONNECTION_CLOSED; // Throw the connection closed IOException instead
        }
    }

    /**
     * Not used.
     * ManagedConnection overrides this method and never calls it.
     * 
     * Push any data sitting in the objects of the write chain to the remote computer.
     * 
     * The OutputStream _out isn't really an OutputStream object, it's just some object that supports the OutputStream interface.
     * This means we can write data to it, and it has a sink it can send that data to.
     * The sink is another object that does the same thing.
     * All together, these objects form the write chain.
     * These objects have internal buffers, and can hold some data for awhile before sending it.
     * Call flush() to get them all to send all their data to the remote computer.
     */
    public void flush() throws IOException {

        /*
         * The try/catch block is necessary for two reasons.
         * For the details, see the notes in Connection.close above the calls to end() on the Inflater/Deflater.
         * Also, see close() on the Input/OutputStreams.
         */

        try {

        	// Have the object that we are writing data to send everything it has to the remote computer
            _out.flush(); // If it also calls _out.flush(), the call will propegate down the chain

            /*
             * (ask)
             * But, what if a buffer fills up?
             * Will the call move back and forth on the chain enough times to let buffers fill up and still send everything?
             */
            
            /*
             * The next object, _out, is probably the object that compresses data.
             * Flushing it made it compress more data.
             * Count the size of the new data it compressed by calling updateWriteStatistics
             */
            
            // Flushing made the Deflater compress more data, count its size
            updateWriteStatistics(null); // Only needs a message if we're not doing compression

        // If the Deflater is shut down, it will throw a NullPointerException
        } catch (NullPointerException npe) {

        	// The Deflater is shut down because the connection must be closed
            throw CONNECTION_CLOSED; // Throw the IOException CONNECTION_CLOSED instead
        }
    }

    /**
     * Asks the Deflater how much data it's processed to update _bytesSent and _compressedBytesSent.
     * 
     * Asks the Deflater how many bytes of normal data we've given it, and how many bytes of compressed data it's produced.
     * Tells the compression statistics object how much bigger both of these counts are since the last time we checked.
     * Saves the new totals in _bytesSent and _compressedBytesSent.
     * 
     * If we're not sending the remote computer compressed data, this method needs the Gnutella packet it's given.
     * There is no Deflater to get counts from, so it measures the size of the packet and adds it to _bytesSent.
     * 
     * @param msg A Gnutella packet that we've just sent to the remote computer
     */
    protected void updateWriteStatistics(Message m) {

    	// We're compessing the data we send to the remote computer
        if( isWriteDeflated() ) {

        	// Ask the deflater the total amount of normal data we've given it, and the total amount of compressed data it's produced
        	long newIn  = _deflater.getTotalIn();  // Number of bytes of normal data we've put into the compressor
        	long newOut = _deflater.getTotalOut(); // Number of bytes of compressed data the deflater has produced

            // Both numbers will be bigger than they were the last time we checked, add the amount they've grown to the compression statistics
        	CompressionStat.GNUTELLA_UNCOMPRESSED_UPSTREAM.addData((int)(newIn - _bytesSent));
        	CompressionStat.GNUTELLA_COMPRESSED_UPSTREAM.addData((int)(newOut - _compressedBytesSent));

            // Save the new totals
        	_bytesSent = newIn;            // The number of bytes of normal data we've sent to this remote computer
        	_compressedBytesSent = newOut; // The size of the data after we compressed it

        // We're sending normal data to the remote computer, and we just sent the given message
        } else if( m != null) {

        	// Add the size of the message to the total count of bytes we've sent to this remote computer
            _bytesSent += m.getTotalLength();
        }
    }

    /**
     * How much data we've sent this remote computer, after compression.
     */
    public long getBytesSent() {

    	// Return the variable that tells how many bytes we've sent over the Internet to this remote computer
        if (isWriteDeflated())           // We told the remote computer "Content-Encoding: deflate"
            return _compressedBytesSent; // The count from _deflater.getTotalOut
        else            
            return _bytesSent;           // The total size of all the packets we've sent
    }

    /**
     * How much data we've sent this remote computer, before compression.
     * If the outgoing stream is not compressed, this is the same as getBytesSent().
     */
    public long getUncompressedBytesSent() {

    	// The count from _deflater.getTotalIn, or the total size of all the packets we've sent
        return _bytesSent;
    }

    /**
     * How much compressed data this remote computer has sent us.
     */
    public long getBytesReceived() {

    	// Return the variable that tells how many bytes we've received over the Internet from this remote computer
        if (isReadDeflated())                // The remote computer told us "Content-Encoding: deflate"
            return _compressedBytesReceived; // The count from _inflater.getTotalIn
        else                                 // The remote computer is sending data we don't have to decompress
            return _bytesReceived;           // The total size of all the packets we've received
    }

    /**
     * How much data this remote computer has sent us, after decompression.
     * If the stream is not compressed, this is the same as getBytesReceived().
     */
    public long getUncompressedBytesReceived() {

    	// The count from _inflater.getTotalOut, or the total size of all the packets we've received
        return _bytesReceived;
    }

    /**
     * Calculate the fraction saved by compressing the data we're sending this remote computer.
     * 
     * A value of 0 means no compression, or compression that isn't working at all yet.
     * A value of 0.25 means we're saving a quarter of the total bandwidth we'd be using if we weren't compressing anything.
     * A value of 0.9 means the data is compressing really well.
     * 
     * The value may be slightly off until the compressing output stream is flushed.
     * Data isn't counted until the compressing output stream compresses it and writes it out.
     */
    public float getSentSavedFromCompression() {

    	// If we're not compressing data for the remote computer, or we are but we haven't prepared any data to be compressed yet, return 0
        if (!isWriteDeflated() || _bytesSent == 0) return 0; // 0 means no compression

        /*
         * We're sending compressed data to the remote computer.
         * We have a packet that's 100 bytes big, this is _bytesSent.
         * We compress it to send it, now it's 80 bytes big, this is _compressedBytesSent.
         * The return value is 1 - (80 / 100), which is 0.2.
         */

        // Calculate the portion of empty space we made by compressing the data
        return 1 - ((float)_compressedBytesSent / (float)_bytesSent);
    }

    /**
     * Returns the percentage saved from having the incoming data compressed.
     * 
     * A value of 0 means no compression, or compression that isn't working at all yet.
     * A value of 0.25 means the remote computer is saving a quarter of the total bandwidth it would be using if it weren't compressing anything.
     * A value of 0.9 means the data is compressing really well.
     */
    public float getReadSavedFromCompression() {

    	// If the remote computer isn't sending us compressed data, or it is and we haven't decompressed any from it yet, return 0
        if (!isReadDeflated() || _bytesReceived == 0) return 0; // 0 means no compression

        /*
         * The remote computer is sending us compressed data.
         * It sent 80 bytes of data, this is _compressedBytesReceived.
         * We decompressed it to create a 100 byte packet, this is _bytesReceived.
         * The return value is 1 - (80 / 100), which is 0.2.
         */

        // Calculate the portion of empty space the remote computer made by compressing the data
        return 1 - ((float)_compressedBytesReceived / (float)_bytesReceived);
    }

   /**
    * The IP address of the remote computer.
    * 
    * If we connected to the remote computer, this is the IP address we connected to.
    * It's the remote computer's externally contactable Internet IP address.
    * The remote computer is probably behind a NAT, so this is the IP address of the NAT.
    * 
    * If the remote computer connected to us, this is the IP address on the far end of the connection socket.
    * It's the IP address of the remote computer's NAT.
    * The remote computer won't know it's own IP address until we tell it this address.
    * 
    * @return The remote computer's IP address, like "66.229.42.183"
    */
    public String getAddress() {

    	// The constructor saved this value in _host
        return _host;
    }

    /**
     * The port number of the remote computer.
     * 
     * If we connected to the remote computer, this is the port number we connected to.
     * It's the remote computer's externally contactable Internet port number.
     * The remote computer is probably behind a NAT, so this is the port number on the NAT.
     * The remote computer setup port forwarding so packets that hit the NAT with this port number get forwarded to it on the LAN.
     * 
     * If the remote computer connected to us, it didn't connect to us from its listening port.
     * Instead, Java and the operating system on the other side made a high-numbered temporary port for the connection.
     * This is called the ephemeral port.
     * Computers use a different ephemeral port for each connection request so they can tell them apart.
     * When the Connection constructor called getPort() on the connection socket, we got the ephemeral port number the remote computer chose.
     * 
     * The ephemeral port number is no use to us.
     * Separate from the ephemeral port, the remote computer has probably setup port forwarding from its NAT to its listening socket.
     * When the remote computer tells us the port number we can contact it at, we'll call setListeningPort() to save that number here instead.
     * 
     * @return The ephemeral port the remote computer connected to us with, or later the port number we can connect to the remote computer on
     */
    public int getPort() {

    	// The Connection constructor saved this value, and setListeningPort updated it 
        return _port;
    }

    /**
     * When the remote computer tells us its listening port number, save it in _port, overwriting the ephemeral port from the connection socket.
     * Only ConnectionManager.processConnectionHeaders() calls setListeningPort() to do this.
     * 
     * If the remote computer connected to us, _port is the ephemeral port number it connected with.
     * This isn't of any use to us, and isn't the same thing as the remote computer's listening socket.
     * When the remote computer tells us what port number it's externally contactable on, we use this method to save that port here.
     * 
     * @param port The port number the remote computer told us it's listening for new connections on
     */
    void setListeningPort(int port) {

    	// Make sure the given port number isn't 0 or too big to fit in 2 bytes
        if (!NetworkUtils.isValidPort(port)) throw new IllegalArgumentException("invalid port: " + port);

        // Discard the epemeral port number the remote computer connected with, and keep the remote computer's externally contactable port number in _port instead
        this._port = port;
    }

    /**
     * The IP address of the remote computer.
     * 
     * To get the remote computer's IP address as a string, call getInetAddress().getHostAddress().
     * Or, just call getAddress() which returns _host, which is the same thing.
     * 
     * @return The remote computer's IP address as an InetAddress object
     */
    public InetAddress getInetAddress() throws IllegalStateException {

    	// Make sure we have a connection to the remote computer
		if (_socket == null) throw new IllegalStateException("Not initialized");

		// Get the address on the far end of the connection socket
		return _socket.getInetAddress(); // This is the remote computer's externally contactable IP address
    }

    /**
     * Get the connection socket we're talking to this remote computer through.
     * This looks like a java.net.Socket, but is probably a LimeWire NIOSocket instead.
     * 
     * @return The connection socket between us and this remote computer, an object of type NIOSocket
     */
    public Socket getSocket() throws IllegalStateException {

    	// Make sure we have a connection to the remote computer
    	if (_socket == null) throw new IllegalStateException("Not initialized");

    	// Return a reference to the connection socket we're talking to this remote computer through
        return _socket;
    }

    /**
     * Returns the time we finished the handshake with this remote computer.
     * 
     * The class initializes _connectionTime to Long.MAX_VALUE, the largest value a long can hold.
     * This means it hasn't been set yet.
     * The initialize() method sets _connectionTime to System.currentTimeMillis().
     * This is the number of milliseconds between January 1970 and the moment initialize() ran.
     *
     * @return The time this connection was established, or Long.MAX_VALUE if initialize() hasn't run yet.
     */
    public long getConnectionTime() {

    	// Return Long.MAX_VALUE if not initialized, or System.currentTimeMillis() when initialize() ran
        return _connectionTime;
    }

    /**
     * When we get a packet from this remote computer, we'll lower its TTL so its hops + TTL doesn't exceed this limit.
     * 
     * The MessageReceiver interface requires this method.
     * MessageReader.handleRead() calls Connection.getSoftMax() to get the limit.
     * It puts the limit in the Message object it's making from the data it sliced.
     * 
     * @return The hops + TTL limit we're enforcing on packets this remote computer sends us
     */
    public byte getSoftMax() {

    	// The initialize() method set this based on the headers the remote computer sent us
        return _softMax;
    }

    /**
     * True if we've been exchanging Gnutella packets with this remote computer for at least 5 seconds.
     * The ReplyHandler interface requires this method.
     * 
     * @return True if it's been more than 5 seconds since we finished the handshake with this remote computer
     */
    public boolean isStable() {

    	// Return true if it's been more than 5 seconds since we finished the handshake with this remote computer
        return isStable(System.currentTimeMillis()); // Get the time now and pass it to the other isStable method
    }

    /**
     * True if we've been exchanging Gnutella packets with this remote computer for at least 5 seconds.
     * 
     * @param millis The time right now, returned from a recent call to System.currentTimeMillis()
     * @return       True if it's been more than 5 seconds since we finished the handshake with this remote computer
     */
    public boolean isStable(long millis) {

    	// Return true if it's been more than 5 seconds since we finished the handshake with this remote computer
        return ((millis - getConnectionTime()) / 1000) > 5;
    }

    /**
     * The remote computer said "Vendor-Message: 0.2" or higher.
     * This means it can route vendor messages.
     * It won't drop a vendor message that has hops more than 0 and a TTL other than 1.
     * 
     * @return True if the remote computer supports routing vendor messages
     */
    public boolean supportsVMRouting() {

    	// Look for "Vendor-Message: 0.2" or higher in the headers the remote computer sent us
        if (_headers != null) return _headers.supportsVendorMessages() >= 0.2; // The version is 0.2 or higher, return true

        // The remote computer does not support this feature
        return false;
    }

    /**
     * Determine if this remote computer can understand a given type of vendor message.
     * See if this remote computer's Messages Supported vendor message lists it, and gets the version number.
     * 
     * @param vendorID The vendor ID that identifies a vendor message, like LIME.
     * @param selector The vendor message type number that identifies a vendor message, like 4.
     * @return         The vendor message version number listed with that ID and number in this remote computer's Messages Supported vendor message, like 1.
     *                 -1 if not listed.
     */
    public int supportsVendorMessage(byte[] vendorID, int selector) {

        // Search the remote computer's Messages Supported vendor message for the given vendor ID and message type number
    	if (_messagesSupported != null) return _messagesSupported.supportsMessage(vendorID, selector);
    	return -1;
    }

    /**
     * Determine if this remote computer's Messages Supported vendor message indicates support for the UDP Connect Back vendor message.
     * Looks for GTKG 7 to get a version number like 2.
     * 
     * @return The version number in the message listing, or -1 if not found
     */
    public int remoteHostSupportsUDPConnectBack() {

        // Look in the Messages Supported vendor message this remote computer sent us
        if (_messagesSupported != null) return _messagesSupported.supportsUDPConnectBack();
        return -1; // Not listed
    }

    /**
     * Determine if this remote computer's Messages Supported vendor message indicates support for the TCP Connect Back vendor message.
     * Looks for BEAR 7 to get a version number like 1.
     * 
     * @return The version number in the message listing, or -1 if not found
     */
    public int remoteHostSupportsTCPConnectBack() {

        // Look in the Messages Supported vendor message this remote computer sent us
        if (_messagesSupported != null) return _messagesSupported.supportsTCPConnectBack();
        return -1; // Not listed
    }

    /**
     * Determine if this remote computer's Messages Supported vendor message indicates support for the UDP Connect Back Redirect vendor message.
     * Looks for LIME 8 to get a version number like 1.
     * 
     * @return The version number in the message listing, or -1 if not found
     */
    public int remoteHostSupportsUDPRedirect() {

        // Look in the Messages Supported vendor message this remote computer sent us
        if (_messagesSupported != null) return _messagesSupported.supportsUDPConnectBackRedirect();
        return -1; // Not listed
    }

    /**
     * Determine if this remote computer's Messages Supported vendor message indicates support for the TCP Connect Back Redirect vendor message.
     * Looks for LIME 7 to get a version number like 1.
     * 
     * @return The version number in the message listing, or -1 if not found
     */
    public int remoteHostSupportsTCPRedirect() {

        // Look in the Messages Supported vendor message this remote computer sent us
        if (_messagesSupported != null) return _messagesSupported.supportsTCPConnectBackRedirect();
        return -1; // Not listed
    }

    /**
     * Determine if this remote computer's Messages Supported vendor message indicates support for the UDP Crawler Pong vendor message.
     * Looks for LIME 6 to get a version number like 1.
     * 
     * @return The version number in the message listing, or -1 if not found
     */
    public int remoteHostSupportsUDPCrawling() {

        // Look in the Messages Supported vendor message this remote computer sent us
    	if (_messagesSupported != null) return _messagesSupported.supportsUDPCrawling();
    	return -1; // Not listed
    }

    /**
     * Determine if this remote computer's Messages Supported vendor message indicates support for the Hops Flow vendor message.
     * Looks for BEAR 4 to get a version number like 1.
     * 
     * @return The version number in the message listing, or -1 if not found
     */
    public int remoteHostSupportsHopsFlow() {

        // Look in the Messages Supported vendor message this remote computer sent us
        if (_messagesSupported != null) return _messagesSupported.supportsHopsFlow();
        return -1; // Not listed
    }

    /**
     * If this is a connection up to an ultrapeer, determine if this remote computer's Messages Supported vendor message indicates support for the Push Proxy Request vendor message.
     * Looks for LIME 21 to get a version number like 1.
     * 
     * Only returns a version number if the remote computer is an ultrapeer and we're just a leaf.
     * If this connection isn't up to an ultrapeer, always returns -1.
     * 
     * @return The version number in the message listing, or -1 if not found
     */
    public int remoteHostSupportsPushProxy() {

        // Look in the Messages Supported vendor message this remote computer sent us
        if ((_messagesSupported != null) && isClientSupernodeConnection()) return _messagesSupported.supportsPushProxy();
        return -1; // Not listed
    }

    /**
     * Determine if this remote computer can do dynamic querying, where a leaf has its ultrapeers search for it.
     * 
     * Looks in the remote computer's Messages Supported vendor message for BEAR 11 1 QueryStatusRequest.
     * LimeWire doesn't use this packet anymore, but still uses its name to advertise support for the dynamic querying fature.
     * 
     * Here, leaf guidance refers to the practice of a leaf telling its ultrapeer how many hits it has gotten in the search the ultrapeer is performing on its behalf.
     * 
     * @return The version number in the message listing, or -1 if not found
     */
    public int remoteHostSupportsLeafGuidance() {

        // Look in the Messages Supported vendor message this remote computer sent us
        if (_messagesSupported != null) return _messagesSupported.supportsLeafGuidance();
        return -1; // Not listed
    }

    /**
     * Determine if this remote computer's Messages Supported vendor message indicates support for the Header Update vendor message.
     * Looks for LIME 25 to get a version number like 1.
     * 
     * @return The version number in the message listing, or -1 if not found
     */
    public int remoteHostSupportsHeaderUpdate() {

        // Look in the Messages Supported vendor message this remote computer sent us
        if (_messagesSupported != null) return _messagesSupported.supportsHeaderUpdate();
        return -1; // Not listed
    }

    /**
     * Determine if this remote computer told us which advanced features it supports by including "WHAT" in its Capabilities vendor message.
     * Looks in the Capabilities vendor message for "WHAT" with a version of 1 or more.
     * "WHAT" tells which advanced features the computer supports.
     * 
     * @return True if this remote computer sent a Capabilities vendor message with "WHAT" listed with a version of 1 or more.
     *         False if it didn't include "WHAT" or send a Capabilities vendor message at all.
     */
    public boolean getRemoteHostSupportsFeatureQueries() {

        // Look in the Capabilties vendor message this remote computer sent us
        if (_capabilities != null) return _capabilities.supportsFeatureQueries() > 0; // Return true if the Capabilities
        return false;
    }

    /**
     * Find out how many advanced features this remote computer supports.
     * Looks in the Capabilities vendor message for "WHAT", and returns the version number.
     * The return value is probably 1, indicating support for the first advanced feature, What's New search.
     * 
     * @return The number of advanced Gnutella features the computer supports, like 1.
     *         -1 if "WHAT" is not listed, or we don't have a Capabilities vendor message from this remote computer.
     */
    public int getRemoteHostFeatureQuerySelector() {

        // Look in the Capabilties vendor message this remote computer sent us
        if (_capabilities != null) return _capabilities.supportsFeatureQueries();
        return -1;
    }

    /**
     * Determine if this remote computer supports What's New search.
     * Searches the Capabilities vendor message it sent us for the "WHAT" capability, and gets the version number.
     * If the version number is 1 or more, the compuer supports What's New search, returns true.
     * 
     * @return True if this remote computer supports What's New search
     */
    public boolean remoteHostSupportsWhatIsNew() {

        // Look in the Capabilties vendor message this remote computer sent us
        if (_capabilities != null) return _capabilities.supportsWhatIsNew();
        return false;
    }

    /**
     * Get the current update number, according to this remote computer.
     * Searches the Capabilities vendor message it sent us for the "LMUP" capability, and gets the version number.
     * 
     * @return The number of the most recent update information this remote computer has received, like 77
     */
    public int getRemoteHostUpdateVersion() {

        // Look in the Capabilties vendor message this remote computer sent us
        if(_capabilities != null) return _capabilities.supportsUpdate();
        return -1;
    }

    /**
     * True if the remote computer we're connected to is on the same LAN here as we are.
     * 
     * @return True if this is a connection to a nearby computer, false if the remote computer is far away on the Internet.
     */
    protected boolean isLocal() {

    	/*
    	 * _socket is our connection socket to this remote computer.
    	 * getInetAddress gets the IP address on the far end of it.
    	 * If the remote computer is on the Internet, this address will be an Internet IP address.
    	 * 
    	 * If the remote computer is on the same LAN as we are, the IP address will be in a range of LAN addresses, like 192.168.1.102.
    	 * isLocalAddress returns true if the given address starts 127 or is our own address on the LAN.
    	 */

    	// Get the IP address on the far end of the connection socket, and return true if it starts 127 or is our own address on the LAN
        return NetworkUtils.isLocalAddress(_socket.getInetAddress());
    }

    /**
     * Give a header name, and get back the value for that header we told the remote computer in the Gnutella handshake.
     * If we didn't write that header, returns null, not a blank string.
     * If we wrote a header more than once, returns the value we wrote last.
     * 
     * @param name A header name, like "User-Agent", that we may have told the remote computer during the Gnutella handshake.
     * @return     The value we sent, like "LimeWire/4.9.33".
     *             If we didn't send that header, returns null, not a blank string.
     */
    public String getPropertyWritten(String name) {

    	// Search all the headers we sent the remote computer for the given header name, and return the value if we sent it
        return HEADERS_WRITTEN.getProperty(name);
    }

    /**
     * Determine if we can still send a packet with this connection.
     * True until code calls the close() method on this Connection object.
     * The ReplyHandler interface requires this method.
     * 
     * @return True if we can still send a packet through this connection.
     *         False if the connection is being closed.
     */
    public boolean isOpen() {

    	// The close() method sets _closed to true
        return !_closed;
    }

    /**
     * Close this Connection object's connection socket and call close() on the InputStream and OutputStream objects.
     * 
     * Also calls end() on the Deflater and Inflater, allowing them to free their internal resources.
     * Uses the _closed flag to only run once, and sets _closed to true.
     */
    public void close() {

    	// Make sure the code here only runs once
        if (_closed) return; // We've already closed this Connection object
        _closed = true;      // Mark this Connection object as closed

        /*
         * Setting the _closed flag to true also insures that the socket is closed if
         * this method is called asynchronously before the socket is initialized.
         */

        // If we have a connection socket to the remote computer
        if (_socket != null) {

        	// Close it the connection socket, disconnecting from the remote computer
            try { _socket.close(); } catch (IOException e) {} // Catch and don't pass on IOException objects
        }
        
        /*
         * Tell the Inflater and Deflater that we're done with them.
         * These calls are dangerous because we don't know that the stream isn't currently deflating or inflating,
         * and access to the deflater and inflater isn't synchronized, and shouldn't be.
         * This can lead to NullPointerException objects popping up in unexpected places.
         * Fortunately, the calls aren't explicitly necessary
         * because when the deflater and inflater objects are garbage collected they will call end for us
         */

        // Tell the Deflater and Inflater objects that we're not going to use them anymore, so they can free internal resources
        if (_deflater != null) _deflater.end();
        if (_inflater != null) _inflater.end();

        /*
         * Closing _in, and possibly _out too, can cause NullPointerExceptions in Message.read, and possibly other places,
         * because BufferedInputStream can't handle the case where one thread is reading from the stream and another closes it.
         * See BugParade ID: 4505257.
         */

        // Close the input and output stream objects that we have been reading from and writing to
        if (_in  != null) { try { _in.close();  } catch (IOException e) {} }
        if (_out != null) { try { _out.close(); } catch (IOException e) {} }
    }

    /**
     * The name and version of the Gnutella program the remote computer is running.
     * 
     * @return The value of the "User-Agent" header the remote computer sent us, or null if it didn't send that header
     */
    public String getUserAgent() {

    	// Return the value of the "User-Agent" header from the headers the remote computer sent us
		return _headers.getUserAgent(); // Returns null, not blank, if the remote computer never gave us a "User-Agent" header
    }

    /**
     * True if the remote computer is running LimeWire.
     * 
     * @return True if the remote computer said "User-Agent: LimeWire", false if it's running some other Gnutella program
     */
    public boolean isLimeWire() {

    	// Return true if the remote computer sent a header like "User-Agent: LimeWire" and then a slash and version number
        return _headers.isLimeWire();
    }

    /**
     * True if the remote computer is running an old version of LimeWire.
     * Only MessageRouter.updateMessage() calls this.
     * 
     * @return True if the remote computer is running LimeWire 3.3 or earlier, false for 3.4 or some other program entirely
     */
    public boolean isOldLimeWire() {

    	// Return true if the remote computer sent a header like "User-Agent: LimeWire/3.3.1", 3.4 is the first new version
        return _headers.isOldLimeWire();
    }

    /**
     * Determines if we told the remote computer we will be sending compressed data.
     * Searches the headers we sent for "Content-Encoding: deflate".
     * If so, we'll have to compress the data stream before we send it.
     *
     * @return True if we're sending compressed data to the remote computer
     */
    public boolean isWriteDeflated() {

    	// Search the headers we sent for "Content-Encoding: deflate"
        return _headersWritten.isDeflateEnabled();
    }

    /**
     * Determines if the remote computer told us it will be sending compressed data.
     * Searches the headers we received for "Content-Encoding: deflate".
     * If so, we'll have to receive the data stream and then decompress it.
     *
     * @return True if the remote computer is sending compressed data to us
     */
    public boolean isReadDeflated() {

    	// Search the headers from the remote computer for "Content-Encoding: deflate"
        return _headers.isDeflateEnabled();
    }

    /**
     * The remote computer supports the advanced features we're looking for ultrapeers to have.
     * This doesn't mean the remote computer is an ultrapeer.
     * The ReplyHandler interface requires this method.
     * 
     * @return True if the remote computer supports advanced Gnutella features that would make it a good ultrapeer
     */
    public boolean isGoodUltrapeer() {

    	// Search the headers from the remote computer for advanced Gnutella features ultrapeers should have
        return _headers.isGoodUltrapeer();
    }

    /**
     * The remote computer supports the advanced features we're looking for leaves to have.
     * This doesn't mean the remote computer is a leaf.
     * The ReplyHandler interface requires this method.
     * 
     * @return True if the remote computer supports advanced Gnutella features that would make it a good leaf
     */
    public boolean isGoodLeaf() {

    	// Search the headers from the remote computer for advanced Gnutella features leaves should have
        return _headers.isGoodLeaf();
    }

    /**
     * True if the remote computer supports pong caching.
     * The ReplyHandler interface requires this method.
     * 
     * A computer that does pong caching never broadcasts pings forward.
     * It keeps a cache of 6 pongs that traveled 0, 1, 2, 3, 4, and 5 hops to get to it.
     * When it gets a pong, it replies with a pong about itself and the cached 6.
     * 
     * @return True if the remote computer told us "Pong-Caching: 0.1" or higher in the handshake
     */
    public boolean supportsPongCaching() {

    	// Search the headers the remote computer sent us for "Pong-Caching: 0.1" or higher
        return _headers.supportsPongCaching();
    }

    /**
     * True if a ping from this remote computer should be let through, false to not relay it because it recently sent us one.
     * Limits the number of pings we'll relay for this computer to one every 2.5 seconds.
     * If this remote computer sent us a ping recently, allowNewPings will return false.
     * This stops a remote computer from pinging the network to death through us.
     * The ReplyHandler interface requires this method.
     * 
     * First returns true 5 seconds into a new connection, and only once every 2.5 seconds after that.
     * 
     * @return True if this is the first ping from this remote computer in 2.5 seconds, and we are free to forward it to the network
     */
    public boolean allowNewPings() {

    	// Only let one thread into this method at a time
    	synchronized (PING_LOCK) {

    		// Get the time right now
	        long curTime = System.currentTimeMillis(); // The number of milliseconds since January 1970

	        // If we haven't been exchanging Gnutella packets with this remote computer for 5 seconds yet, don't relay pings from it
			if (!isStable(curTime)) return false; // A new connection might drop at any moment

			// If it hasn't been 2.5 seconds since the last ping we let through, don't let them through yet
	        if (curTime < _nextPingTime) return false;

	        /*
	         * We've been exchanging Gnutella packets for at least 5 seconds,
	         * and, we've been blocking pings from this remote computer for 2.5 seconds or more.
	         */

	        // Calculate the time 2.5 seconds from now, this is when we'll let the next ping through
			_nextPingTime = System.currentTimeMillis() + 2500;

			// Let this ping through
	        return true;
    	}
    }

    /**
     * True if we should send a pong to this remote computer, false to not give it one because we did recently.
     * Limits the number of pongs we'll send to this remote computer to one every 12 seconds.
     * 
     * First returns true 5 seconds into a new connection.
     * For the next 5 seconds of the connection, returns true every 0.3 seconds.
     * Once we've been exchanging packets with this remote computer for 10 seconds, only returns true once every 12 seconds.
     * 
     * MessageRouter.handlePingReply() uses this to meter the pongs we send our leaves as an ultrapeer.
     * 
     * @return True if this is the first pong from this remote computer in 12 seconds, and we are free to forward it to the network
     */
    public boolean allowNewPongs() {

    	// Only let one thread into this method at a time
    	synchronized(PONG_LOCK) {

    		// Get the time right now
		    long curTime = System.currentTimeMillis(); // The number of milliseconds since January 1970
		    
		    // If we haven't been exchanging Gnutella packets with this remote computer for 5 seconds yet, don't relay pongs from it
			if (!isStable(curTime)) return false; // A new connection might drop at any moment

			// If we haven't waited long enough yet, don't let this pong through
		    if (curTime < _nextPongTime) return false;
		    
		    /*
		     * If the connection is young, let a lot of pongs from the remote computer through.
		     * Once we've been conected for longer, don't allow so many pongs.
		     */

		    // Set the interval to 0.3 seconds at the start, and 12 seconds after that
			int interval;
			if (curTime - getConnectionTime() < 10000) // If we've been exchanging Gnutella packets for less than 10 seconds
				interval =   300;                      // Let a pong through every 0.3 seconds
			else                                       // If we've been exchanging Gnutella packets for more than 10 seconds
				interval = 12000;                      // Let a pong through every 12 seconds

			// Calculate when we'll next allow a pong through
			_nextPongTime = curTime + interval; // 0.3 or 12 seconds from now

			// Let this pong through
		    return true;
    	}
    }

	/**
	 * The value of the remote computer's "X-Degree" header, the number of ultrapeer connections it tries to keep up as an ultrapeer.
	 * If ultrapeers have more ultrapeer connections, packets can travel fewer hops, and the Gnutella network will be more efficient.
	 * 
	 * @return The value of the "X-Degree" header the remote computer told us
	 */
	public int getNumIntraUltrapeerConnections() {

		// Return the value of the "X-Degree" header the remote computer told us
		return _headers.getNumIntraUltrapeerConnections();
	}

	/**
	 * True if the remote computer said "X-Degree: 15" or higher, meaning that it, as an ultrapeer, will keep that many connections to other ultrapeers.
	 * The Gnutella network will be more efficient if ultrapeers have more connections and packets travel fewer hops.
     * The ReplyHandler interface requires this method.
	 * 
	 * @return True if the remote computer said "X-Degree: 15" or higher
	 */
	public boolean isHighDegreeConnection() {

		// True if the remote computer said "X-Degree: 15" or higher
		return _headers.isHighDegreeConnection();
	}

	/**
	 * True if the remote computer said "X-Ultrapeer-Query-Routing: 0.1", meaning it can exchange query routing tables with other ultrapeers.
	 * This means it can exchange QRP tables not just with leaves, but with ultrapeers too.
	 * Then, it doesn't have to send a query to a connected ultrapeer if it knows it won't have it.
     * The ReplyHandler interface requires this method.
	 *  
	 * @return True if the remote computer said "X-Ultrapeer-Query-Routing: 0.1" or higher
	 */
	public boolean isUltrapeerQueryRoutingConnection() {

		// True if the remote compuer said "X-Ultrapeer-Query-Routing: 0.1" or higher
		return _headers.isUltrapeerQueryRoutingConnection();
    }

    /**
     * True if the remote computer said "X-Ext-Probes: 0.1", meaning it supports probe queries.
     * Probe queries are queries with a TTL of 1, they only make one trip across the Internet.
     * They should not block the send path of subsequent, higher TTL queries. (do)
     * 
     * @return True if the remote computer said "X-Ext-Probes: 0.1" or higher
     */
    public boolean supportsProbeQueries() {

        // True if the remote computer said "X-Ext-Probes: 0.1" or higher
        return _headers.supportsProbeQueries();
    }

    /**
     * True if the remote computer has sent us at least one complete group of headers.
     * If the remote computer connected to us, this means it has sent its stage 1 group of headers.
     * If we initiated the connection, this means the remote computer has sent its stage 2 response.
     * 
     * @return True if the remote computer has sent us a group of headers, false if it hasn't done this yet.
     */
    public boolean receivedHeaders() {
    	
    	/*
    	 * _headers is a HandshakeRespone object.
    	 * It's initialized in the class with code like this:
    	 * 
    	 *   private volatile HandshakeResponse _headers = HandshakeResponse.createEmptyResponse();
    	 *     
    	 * So, it's never null, and this method will always return true.
    	 * 
    	 * TODO:kfaaborg _header is never null, so the receivedHeaders method will always return true.
    	 */

    	// The class initializes _headers, so this method will always return true
        return _headers != null;
    }

	/**
	 * Get the HandshakeResponse object that holds all the headers the remote computer sent us.
	 * If we initiated the connection, this means it will be the remote computer's stage 2 headers.
	 * If the remote computer connected to us, it will be the headers of the remote computer's stage 1 greeting and stage 3 confirmation combined.
	 * 
	 * @return This Connection object's HandshakeResponse object that holds all the headers the remote computer sent us.
	 */
	public HandshakeResponse headers() {

		// Return a reference to the HandshakeResponse object we've been filling in the readHeaders() method
		return _headers;
	}

	/**
	 * The value of the "X-Version" header, meaning the remote computer can give us a digitally signed upgrade command.
	 * 
	 * @return The value of the "X-Version" header the remote computer told us.
	 */
	public String getVersion() {

		// Return the value of the "X-Version" header from the headers the remote computer told us
		return _headers.getVersion();
	}

    /**
     * Determine if the remote computer is a leaf.
     * True if the remote computer said "X-Ultrapeer: false", meaning it's a leaf.
     * The ReplyHandler interface requires this method.
     * 
     * @return True if the remote computer is a leaf
     */
    public boolean isLeafConnection() {

    	// True if the remote computer said "X-Ultrapeer: false", meaning it is a leaf
		return _headers.isLeaf();
    }

    /**
     * True if the remote computer said "X-Ultrapeer: true", meaning it's an ultrapeer.
     * 
     * @return True if the remote computer is an ultrapeer
     */
    public boolean isSupernodeConnection() {

    	// True if the remote computer said "X-Ultrapeer: true", meaning it is an ultrapeer
		return _headers.isUltrapeer();
    }

    /**
     * True if the remote computer is an ultrapeer and we are just a leaf.
     * In the handshake, we said "X-Ultrapeer: false" and the remote computer said "X-Ultrapeer: true".
     * Our connection to this remote computer is a connection up from a leaf to an ultrapeer.
     * 
     * @return True if we're a leaf and the remote computer is an ultrapeer
	 */
    public boolean isClientSupernodeConnection() {

        // The remote computer said "X-Ultrapeer: true"
        if (isSupernodeConnection()) {

        	// Get the value of the "X-Ultrapeer" header we told the remote computer
        	String value = getPropertyWritten(HeaderNames.X_ULTRAPEER); // Looks in HEADERS_WRITTEN for the key "X-Ultrapeer", value will be like "False"
        	if (value != null) {

        		// Read it as a boolean
        		boolean b = Boolean.valueOf(value).booleanValue();

        		// We said "X-Ultrapeer: false"
        		if (b == false) return true; // The remote computer told us it's an ultrapeer and we told it we're a leaf
        	}
        }

        // We have some other relationship with the remote computer
        return false;
    }

    /**
     * True if both the we and the remote computer are ultrapeers.
     * In the handshake, we both said "X-Ultrapeer: true".
     * Our connection to this remote computer is a connection to another ultrapeer.
     * 
     * @return True if we're both ultrapeers
	 */
    public boolean isSupernodeSupernodeConnection() {

        // The remote computer said "X-Ultrapeer: true", it is a leaf
        if (isSupernodeConnection()) {

        	// Get the value of the "X-Ultrapeer" header we told the remote computer
        	String value = getPropertyWritten(HeaderNames.X_ULTRAPEER); // Looks in HEADERS_WRITTEN for the key "X-Ultrapeer", value will be like "False"
        	if (value != null) {

        		// We said "X-Ultrapeer: true", we are an ultrapeer
        		if (Boolean.valueOf(value).booleanValue()) return true; // Both we and the remote computer said "X-Ultrapeer: true"
        	}
        }

        // One of us said "X-Ultrapeer: false"
        return false;
    }    

	/**
	 * The remote computer said "X-Guess: 0.1", it supports GUESS, the way of giving rare searches longer TTLs.
	 * 
	 * @return True if the remote computer supports GUESS
	 */
	public boolean isGUESSCapable() {

		// The remote computer told us "X-Guess: 0.1" or higher
		return _headers.isGUESSCapable();
	}

	/**
	 * The remote computer is an ultrapeer that supports GUESS, the way of giving rare searches longer TTLs.
	 * 
	 * @return True if the remote computer said "X-Ultrapeer: true" and "X-Guess: 0.1" or higher
	 */
	public boolean isGUESSUltrapeer() {

		// The remote computer told us "X-Ultrapeer: true" and "X-Guess: 0.1" or higher
		return _headers.isGUESSUltrapeer();
	}

	/**
	 * The remote computer said "X-Temp-Connection: true" and may disconnect from us soon. (do) is that what this means?
	 * 
	 * @return True if the remote computer told us in the handshake that this is a temporary connection
	 */
    public boolean isTempConnection() {

    	// The remote computer told us "X-Temp-Connection: true"
		return _headers.isTempConnection();
    }

    /**
     * True if we are an ultrapeer and the remote computer is a leaf.
     * We said "X-Ultrapeer: true".
     * The remote computer said "X-Ultrapeer: false" and "X-Query-Routing: 0.1".
     * The ReplyHandler interface requires this method.
     * 
     * @return True if we are an ultrapeer and the remote computer is a leaf
     */
    public boolean isSupernodeClientConnection() {

    	// The remote computer said "X-Ultrapeer: false", meaning it's a leaf
        if (isLeafConnection()) {

        	// Get the value of the "X-Ultrapeer" header we told the remote computer
        	String value = getPropertyWritten(HeaderNames.X_ULTRAPEER); // Looks in HEADERS_WRITTEN for the key "X-Ultrapeer", value will be like "False"
        	if (value != null) {

        		// We said "X-Ultrapeer: true", we are an ultrapeer
        		if (Boolean.valueOf(value).booleanValue()) {

        			// The remote compuer said "X-Query-Routing: 0.1", it can send us a hash map of all the files it is sharing
        			if (isQueryRoutingEnabled()) {

        				// We're an ultrapeer, and the remote computer is a leaf that can do QRP
        				return true;
        			}
        		}
        	}
        }

        // We have some other relationship with the remote computer
        return false;
    }

    /**
     * The remote computer supports GGEP, so we can send it big pong packets with custom information inside.
     * It said "GGEP: 0.5" with any version number.
     * 
     * @return True if we can send Gnutella packets with GGEP extension blocks inside them to this remote computer
     */
    public boolean supportsGGEP() {

    	// The remote computer said "GGEP: 0.5" with any version number
		return _headers.supportsGGEP();
    }

    /**
     * Send a statistic vendor message to this remote computer.
     * The ReplyHandler interface requires this method.
     * (do) what is a StatisticVendorMessage
     * 
     * @param The StatisticVendorMessage Gnutella packet to send
     */
    public void handleStatisticVM(StatisticVendorMessage m) throws IOException {

    	// Send the given Gnutella packet to this remote computer
        send(m);
    }

    /**
     * Send a SIMPP vendor message to the remote computer.
     * SIMPP is the system of signed messages that lets the company LimeWire communicate directly with LimeWire programs running on the Internet.
     * 
     * @param The SimppVM Gnutella packet to send
     */
    public void handleSimppVM(SimppVM m) throws IOException {

    	// Send the given Gnutella packet to this remote computer
        send(m);
    }

    /**
     * True if the remote computer said "X-Query-Routing: 0.1" or higher.
     * This means it suports the Query Routing Protocol, QRP.
     * QRP is the Gnutella feature in which a leaf gives an ultrapeer a hash map that describes everything it's sharing.
     * The ultrapeer uses the hash map to not send the leaf queries for files it knows the leaf won't have.
     * 
     * @return True if the remote computer supports query routing
     */
    boolean isQueryRoutingEnabled() {
    	
    	// The remote computer said "X-Query-Routing: 0.1" or higher
		return _headers.isQueryRoutingEnabled();
    }

    /**
     * Composes text like "CONNECTION: host=216.27.178.74 port=6346" with the IP address and port number of the remote computer.
     * This overrieds the root Object.toString() method.
     */
    public String toString() {

    	// Put the IP address and port number of the remote computer into a string
        return "CONNECTION: host=" + _host  + " port=" + _port;
    }

    /**
     * The remote computer's language of choice, like "en" for English.
     * This is the value of a header like "X-Locale-Pref: en" the remote computer sent us.
     * The ReplyHandler interface requires this method.
     * 
     * The remote computer probably didn't specify a language preference.
     * In that case, this is our own language preference, taken from settings.
     * This makes getLocalePref always return a language, and a compatible one if it didn't specify a preference.
     * 
     * @return Text like "en" from a header like "X-Locale-Pref: en" the remote computer sent us, or our own program settings
     */
    public String getLocalePref() {

    	// The value of the remote computer's "X-Locale-Pref" header, or if it didn't send one, our language choice from settings, like "en"
        return _headers.getLocalePref();
    }

    /*
    // Technically, a Connection object can be equal in various ways...
    // Connections can be said to be equal if the pipe the information is
    // travelling through is the same.
    // Or they can be equal if the remote host is the same, even if the
    // two connections are on different channels.
    // Ideally, our equals method would use the second option, however
    // this has problems with tests because of the setup of various
    // tests, connecting multiple connection objects to a central
    // testing Ultrapeer, uncorrectly labelling each connection
    // as equal.
    // Using pipe equality (by socket) also fails because
    // the socket doesn't exist for outgoing connections until
    // the connection is established, but the equals method is used
    // before then.
    // Until necessary, the equals & hashCode methods are therefore
    // commented out and sameness equality is being used.

    public boolean equals(Object o) {
    	return super.equals(o);
    }

    public int hashCode() {
    	return super.hashCode();
    }

    /////////////////////////// Unit Tests  ///////////////////////////////////

    // Unit test
    public static void main(String args[]) {
    	Assert.that(! notLessThan06("CONNECT"));
    	Assert.that(! notLessThan06("CONNECT/0.4"));
    	Assert.that(! notLessThan06("CONNECT/0.599"));
    	Assert.that(! notLessThan06("CONNECT/XP"));
    	Assert.that(notLessThan06("CONNECT/0.6"));
    	Assert.that(notLessThan06("CONNECT/0.7"));
    	Assert.that(notLessThan06("GNUTELLA CONNECT/1.0"));
    	
    	final Properties props=new Properties();
    	props.setProperty("Query-Routing", "0.3");        
    	HandshakeResponder standardResponder=new HandshakeResponder() {
    		public HandshakeResponse respond(HandshakeResponse response,
    				boolean outgoing) {
    			return new HandshakeResponse(props);
    		}
    	};        
    	HandshakeResponder secretResponder=new HandshakeResponder() {
    		public HandshakeResponse respond(HandshakeResponse response,
    				boolean outgoing) {
    			Properties props2=new Properties();
    			props2.setProperty("Secret", "abcdefg");
    			return new HandshakeResponse(props2);
    		}
    	};
    	ConnectionPair p=null;
    	
    	//1. 0.4 => 0.4
    	p=connect(null, null, null);
    	Assert.that(p!=null);
    	Assert.that(p.in.getProperty("Query-Routing")==null);
    	Assert.that(p.out.getProperty("Query-Routing")==null);
    	disconnect(p);
    	
    	//2. 0.6 => 0.6
    	p=connect(standardResponder, props, secretResponder);
    	Assert.that(p!=null);
    	Assert.that(p.in.getProperty("Query-Routing").equals("0.3"));
    	Assert.that(p.out.getProperty("Query-Routing").equals("0.3"));
    	Assert.that(p.out.getProperty("Secret")==null);
    	Assert.that(p.in.getProperty("Secret").equals("abcdefg"));
    	disconnect(p);
    	
    	//3. 0.4 => 0.6 (Incoming doesn't send properties)
    	p=connect(standardResponder, null, null);
    	Assert.that(p!=null);
    	Assert.that(p.in.getProperty("Query-Routing")==null);
    	Assert.that(p.out.getProperty("Query-Routing")==null);
    	disconnect(p);
    	
    	//4. 0.6 => 0.4 (If the receiving connection were Gnutella 0.4, this
    	//wouldn't work.  But the new guy will automatically upgrade to 0.6.)
    	p=connect(null, props, standardResponder);
    	Assert.that(p!=null);
    	//Assert.that(p.in.getProperty("Query-Routing")==null);
    	Assert.that(p.out.getProperty("Query-Routing")==null);
    	disconnect(p);
    	
    	//5.
    	System.out.println("-Testing IOException reading from closed socket");
    	p=connect(null, null, null);
    	Assert.that(p!=null);
    	p.in.close();
    	try {
    		p.out.receive();
    		Assert.that(false);
    	} catch (BadPacketException failed) {
    		Assert.that(false);
    	} catch (IOException pass) {
    	}
    	
    	//6.
    	System.out.println("-Testing IOException writing to closed socket");
    	p=connect(null, null, null);
    	Assert.that(p!=null);
    	p.in.close();
    	try { Thread.sleep(2000); } catch (InterruptedException e) { }
    	try {
    		//You'd think that only one write is needed to get IOException.
    		//That doesn't seem to be the case, and I'm not 100% sure why.  It
    		//has something to do with TCP half-close state.  Anyway, this
    		//slightly weaker test is good enough.
    		p.out.send(new QueryRequest((byte)3, 0, "las"));
    		p.out.flush();
    		p.out.send(new QueryRequest((byte)3, 0, "las"));
    		p.out.flush();
    		Assert.that(false);
    	} catch (IOException pass) {
    	}
    	
    	//7.
    	System.out.println("-Testing connect with timeout");
    	Connection c=new Connection("this-host-does-not-exist.limewire.com", 6346);
    	int TIMEOUT=1000;
    	long start=System.currentTimeMillis();
    	try {
    		c.initialize(TIMEOUT);
    		Assert.that(false);
    	} catch (IOException e) {
    		//Check that exception happened quickly.  Note fudge factor below.
    		long elapsed=System.currentTimeMillis()-start;  
    		Assert.that(elapsed<(3*TIMEOUT)/2, "Took too long to connect: "+elapsed);
    	}
    }   
    
    private static class ConnectionPair {
    	Connection in;
    	Connection out;
    }
    
    private static ConnectionPair connect(HandshakeResponder inProperties,
    		Properties outRequestHeaders,
    		HandshakeResponder outProperties2) {
    	ConnectionPair ret=new ConnectionPair();
    	com.limegroup.gnutella.tests.MiniAcceptor acceptor=
    		new com.limegroup.gnutella.tests.MiniAcceptor(inProperties);
    	try {
    		ret.out=new Connection("localhost", 6346,
    				outRequestHeaders, outProperties2,
    				true);
    		ret.out.initialize();
    	} catch (IOException e) { }
    	ret.in=acceptor.accept();
    	if (ret.in==null || ret.out==null)
    		return null;
    	else
    		return ret;
    }
    
    private static void disconnect(ConnectionPair cp) {
    	if (cp.in!=null)
    		cp.in.close();
    	if (cp.out!=null)
    		cp.out.close();
    }    
    */
}

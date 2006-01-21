
// Commented for the Learning branch

package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;

/**
 * Represents a group of headers in the handshake.
 * 
 * A HandshakeResponse object holds a single group of handshake headers.
 * It might be a group of headers from the remote computer, or it might be a group of headers we are preparing to send out. 
 * 
 * If you're using a HandshakeResponse object to hold a group of headers that's come in from a remote computer,
 * call the constructor and give it the Properties hash table with the headers that arrived.
 * It will bring them in, parse through them, and then you can call methods to read different values easily. 
 * 
 * If you're using a HandshakeResponse object to compose a group of headers to send out,
 * call the constructor and give it the Properties hash table from LeafHeaders or UltrapeerHeaders. 
 * 
 * The headers are stored in the Properties hash table named HEADERS.
 * The members STATUS_CODE and STATUS_MESSAGE hold the information from the first line, like "200 OK" or "503 Some Error". 
 * 
 * There are methods you can call to read the values of headers stored in the object.
 * For instance, call getMaxTTL to read the value from a header like X-Max-TTL: 3" header.
 * Public static final members like OK and OK_MESSAGE hold values like 200 and "OK" so you don't have to type them elsewhere. 
 * 
 * There are some public static methods here also, with names like createAcceptIncomingResponse and createRejectOutgoingResponse.
 * You can call them without making a HandshakeResponse object. These just call into the HandshakeResponse constructor, and return a new HandshakeResponse object.
 */
public final class HandshakeResponse {
	
	// Continue the handshake with "GNUTELLA/0.6 200 OK"
	/** 200, the status code that indicates the speaker would like to keep the connection */
    public static final int OK = 200;
    /** "OK", the status message that indicates the speaker would like to keep the connection */
    public static final String OK_MESSAGE = "OK";

    // Respond to the crawler with "GNUTELLA/0.6 593 Hi"
    /** 593, the status code we tell the crawler */
    public static final int CRAWLER_CODE = 593;
    /** "Hi", the status message we tell the crawler */
    public static final String CRAWLER_MESSAGE = "Hi";

    // Say we can't accept because "GNUTELLA/0.6 503 I am a shielded leaf node"
    /** 503, the error code that a shielded leaf node should give to incoming connections */
    public static final int SHIELDED = 503;
    /** "I am a shielded leaf node", the error message that a shielded leaf node should give to incoming connections */
    public static final String SHIELDED_MESSAGE = "I am a shielded leaf node";

    // Say we can't accept because we have no open slots "GNUTELLA/0.6 503 Service unavailable"
    /** 503, the error code a computer uses to refuse a new Gnutella connection */
    public static final int SLOTS_FULL = 503;
    /** "Service unavailable", the error message that a computer with no slots should give to incoming connections */
    public static final String SLOTS_FULL_MESSAGE = "Service unavailable";

    // Say we can't accept for some other reason "GNUTELLA/0.6 503 Service Not Available"
    /** 503, the default bad status code to be used while rejecting connections */
    public static final int DEFAULT_BAD_STATUS_CODE = 503;
    /** "Service Not Available", the default bad status message to be used while rejecting connections */
    public static final String DEFAULT_BAD_STATUS_MESSAGE = "Service Not Available";

    // Refuse a foreign language computer with "GNUTELLA/0.6 577 Service Not Available"
    /** 577, the status code we send when the remote computer's "X-Locale-Pref" header doesn't match our language preference */
    public static final int LOCALE_NO_MATCH = 577; // Use this custom error code instead of the very common 503
    /** "Service Not Available", the status message we send when the remote computer's "X-Locale-Pref" header doesn't match our language preference */
    public static final String LOCALE_NO_MATCH_MESSAGE = "Service Not Available";
    
    // A group of handshake headers begins with a line like "GNUTELLA/0.6 200 OK"
    // Here, 200 is the status code and "OK" is the status message
    // The STATUS_CODE and STATUS_MESSAGE members of this class represent that part of the group of handshake headers
    /** The status code of this group of headers, like 200 or 503 */
    private final int STATUS_CODE;
    /** The status message that goes with that status code, like "OK" or "Service Not Available" */
    private final String STATUS_MESSAGE;

    /**
     * The Gnutella handshake headers in the group of headers that this object represents.
     * HEADERS is of type Properties, which is a hash table of strings.
     * The hash table has keys like "User-Agent" and values like "LimeWire/4.9.33" to represent the headers.
     */
    private final Properties HEADERS;

    /** True if HEADERS contains one like "GGEP: 0.5", meaning the remote computer supports big pongs */
    private Boolean _supportsGGEP;

    /** True if the headers this object holds describe a good leaf, a leaf we would want to connect to */
    private final boolean GOOD_LEAF;

    /** True if the headers this object holds describe a good ultrapeer, an ultrapeer we would want to connect to */
    private final boolean GOOD_ULTRAPEER;

    /**
     * The headers this object holds might contain one like "X-Degree: 6".
     * This means that the ultrapeer that said this tries to keep 6 connections to other ultrapeers.
     * The DEGREE variable holds the number 6.
     */
    private final int DEGREE;

    /**
     * True if this object holds the a header like "X-Degree: 15" with the number 15 or higher.
     * This means that the ultrapeer that said this tries to keep 15 or more connections to other ultrapeers.
     * If ultrapeers have more ultrapeer connections, packets can travel fewer hops, and the Gnutella network will be more efficient.
     */
    private final boolean HIGH_DEGREE;

    /**
     * True if the headers this object holds include "X-Ultrapeer-Query-Routing: 0.1" or a higher version number.
     * This means the ultrapeer that said it supports ultrapeer query routing.
     */
    private final boolean ULTRAPEER_QRP;

    /**
     * The number value of the "X-Max-TTL" header.
     * The computer that said this dosn't want packets that live longer than this.
     */
    private final byte MAX_TTL;

    /**
     * True if the headers here include "X-Dynamic-Querying: 0.1" or a higher version number.
     * This means the computer that said it supports dynamic querying.
     */
    private final boolean DYNAMIC_QUERY;

    /**
     * True if the headers here include "X-Ultrapeer: true".
     * This means the computer that said it is an ultrapeer.
     */
    private final boolean ULTRAPEER;

    /**
     * True if the headers here include "X-Ultrapeer: false".
     * This means the computer that said it is a leaf.
     */
    private final boolean LEAF;
    
    /**
     * True if the headers here include "Content-Encoding: deflate".
     * This means that the computer that said it is going to send compressed data.
     */
    private final boolean DEFLATE_ENCODED;
   
    /**
     * True if the headers here include "X-Ext-Probes: 0.1" or a higher version number.
     * This means the computer that said these headers supports probe queries.
     */
    private final boolean PROBE_QUERIES;

    /**
     * True if the headers here include "Pong-Caching: 0.1" or a higher version number.
     * This means the computer that said these headers supports pong caching.
     */
    private final boolean PONG_CACHING;

    /**
     * True if the headers here include "X-Guess: 0.1" or a higher version number.
     * This means the computer that said these headers supports GUESS.
     */
    private final boolean GUESS_CAPABLE;
    
	/**
	 * True if the headers here include "Crawler: 0.1" or a higher version number.
	 * This means the computer that said these headers is the crawler.
	 */
	private final boolean IS_CRAWLER;
	
	/**
	 * True if the headers here include one like "User-Agent: LimeWire/4.9.33" where the value starts "limewire".
	 * This means the computer that said these headers is running LimeWire.
	 */
	private final boolean IS_LIMEWIRE;
    
    /**
     * True if the user agent header is like "User-Agent: LimeWire/3.3.1" where it's LimeWire, but an old version.
     * Any version 2.* is old, as is 3.1, 3.2, and 3.3. Version 3.4 is the first not old version.
     */
    private final boolean IS_OLD_LIMEWIRE;
    
    /**
     * True if the headers here include "X-Requeries: false".
     * This means the computer that said it claims to not requery.
     */
    private final boolean NO_REQUERYING;
    
    /**
     * The headers here might include one like "X-Locale-Pref: en".
     * This string will hold the value, like "en" for English.
     */
    private final String LOCALE_PREF;

    /**
     * When we compose an "X-Try-Ultrapeers" header, we'll send 10 IP addresses.
     * This constant holds that number, 10.
     */
    private static final int NUM_X_TRY_ULTRAPEER_HOSTS = 10;
    
    /**
     * Make a new HandshakeResponse object that represents a group of Gnutella handshake headers.
     * Start it out with the "200 OK" status code and message and the given headers loaded into it.
     * This constructor just calls the main one with this(OK, OK_MESSAGE, headers).
     * 
     * @param headers The headers to load into this HandshakeResponse object
     */
    private HandshakeResponse(Properties headers) {

    	// Call the constructor with "GNUTELLA/0.6 200 OK" and the given headers
    	this(OK, OK_MESSAGE, headers);
    }    

    /**
     * Make a new HandshakeResponse object that represents a group of Gnutella handshake headers.
     * Start it out with the given status code and message, and no headers.
     * This constructor just calls the main one with this(code, message, new Properties()).
     *
     * @param code    The status code for this group of headers, like 200 or 503
     * @param message The status message that goes along with that code, like "OK" or "Service Unavailable"
     */
    private HandshakeResponse(int code, String message) {
    	
    	// Pass the given status code and message to the constructor
        this(code, message, new Properties()); // Give it a new blank Properties hash table of strings
    }

    /**
     * A HandshakeResponse object represents a group of handshake headers.
     * It holds them here, and parses through them to make reading them easy.
     * 
     * This is the main constructor that actually sets up a new HandshakeResponse object.
     * Makes a new HandshakeResponse object with the given status code, status message and headers.
     * 
     * If you're making a new HandshakeResponse object to hold headers from a remote computer, pass them in as a Properties hash table.
     * If you're making a new HandshakeResponse object to compose headers we'll send out, make a new LeafHeaders or UltrapeerHeaders object for headers.
     * When the constructor gets the headers in the Properties hash table, it parses through them and sets all the member variables that represent values and features.
     * 
     * @param code    The status code for this group of headers, like 200 or 503
     * @param message The status message that goes along with that code, like "OK" or "Service Unavailable"
     * @param headers The headers that make up this group, a hash table of strings with keys like "X-Requeries:" and values like "false"
     */
    HandshakeResponse(int code, String message, Properties headers) {

    	// Save the given status code, status message, and handshake headers in this new object
        STATUS_CODE    = code;
        STATUS_MESSAGE = message;
        HEADERS        = headers;

        // Read the headers now to set member variables we can check easily later
        DEGREE        = extractIntHeaderValue(HEADERS, HeaderNames.X_DEGREE, 6);                 // "X-Degree: 6", the number of ultrapeer to ultrapper connections
        HIGH_DEGREE   = getNumIntraUltrapeerConnections() >= 15;                                 // True if "X-Degree: 15" or more
        ULTRAPEER_QRP = isVersionOrHigher(HEADERS, HeaderNames.X_ULTRAPEER_QUERY_ROUTING, 0.1F); // True if "X-Ultrapeer-Query-Routing: 0.1" or higher
        MAX_TTL       = extractByteHeaderValue(HEADERS, HeaderNames.X_MAX_TTL, (byte)4);         // "X-Max-TTL: 3", no packets that live longer allowed
        DYNAMIC_QUERY = isVersionOrHigher(HEADERS, HeaderNames.X_DYNAMIC_QUERY, 0.1F);           // "X-Dynamic-Querying: 0.1", true if supported
        PROBE_QUERIES = isVersionOrHigher(HEADERS, HeaderNames.X_PROBE_QUERIES, 0.1F);           // "X-Ext-Probes: 0.1", true if supported

        // "X-Requeries: false", we want to see this promise from remote computers
        NO_REQUERYING = isFalseValue(HEADERS, HeaderNames.X_REQUERIES);

        // "User-Agent: LimeWire/4.9.33", true if LimeWire
        IS_LIMEWIRE = extractStringHeaderValue(headers, HeaderNames.USER_AGENT).toLowerCase().startsWith("limewire");

        // Determine if these headers describe a good ultrapeer, one we would want to connect to
        GOOD_ULTRAPEER =
        	isHighDegreeConnection()            && // "X-Degree: 15" or higher, tries to keep up lots of ultrapeer to ultrapeer connections
            isUltrapeerQueryRoutingConnection() && // "X-Ultrapeer-Query-Routing: 0.1" or higher
            (getMaxTTL() < 5)                   && // "X-Max-TTL: 4" or lower
            isDynamicQueryConnection();            // "X-Dynamic-Querying: 0.1" or higher

        // Determine if these headers describe a good leaf
        GOOD_LEAF = GOOD_ULTRAPEER &&       // Must have the features of an ultrapeer
        	(IS_LIMEWIRE || NO_REQUERYING); // And be running LimeWire, or have made the no requerying pledge

        // Parse more headers and save the results in member variables
        ULTRAPEER       = isTrueValue(HEADERS, HeaderNames.X_ULTRAPEER);                                        // "X-Ultrapeer: true", these headers describe an ultrapeer
        LEAF            = isFalseValue(HEADERS, HeaderNames.X_ULTRAPEER);                                       // "X-Ultrapeer: false", these headers describe a leaf
        DEFLATE_ENCODED = isStringValue(HEADERS, HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);      // "Content-Encoding: deflate", the data will be sent compressed
        PONG_CACHING    = isVersionOrHigher(headers, HeaderNames.X_PONG_CACHING, 0.1F);                         // "Pong-Caching: 0.1", true if supported
        GUESS_CAPABLE   = isVersionOrHigher(headers, HeaderNames.X_GUESS, 0.1F);                                // "X-Guess: 0.1", true if supported
        IS_CRAWLER      = isVersionOrHigher(headers, HeaderNames.CRAWLER, 0.1F);                                // "Crawler: 0.1", this is the crawler that has contacted us
        IS_OLD_LIMEWIRE = IS_LIMEWIRE && oldVersion(extractStringHeaderValue(headers, HeaderNames.USER_AGENT)); // "User-Agent: Limewire/3.3.1", and old version

        // Read the language preference or store the user's default
        String loc  = extractStringHeaderValue(headers, HeaderNames.X_LOCALE_PREF);           // Read the value of the header like "X-Locale-Pref: en"
        LOCALE_PREF = (loc.equals("")) ? ApplicationSettings.DEFAULT_LOCALE.getValue() : loc; // Save it in LOCALE_PREF, or get the value from settings
    }

    /**
     * Determines if it's an old version of LimeWire that said the given "User-Agent" header value.
     * 
     * @param userAgent The value of the "User-Agent" header, like "LimeWire/4.8.1"
     * @return          True if the version number is less than 3.4, making this old
     */
    private boolean oldVersion(String userAgent) {
    	
    	// Make a StringTokenizer and have it break the user agent value around the slashes and periods in it
    	StringTokenizer tok = new StringTokenizer(userAgent, "/."); // Tokens like "LimeWire", "4", "8", "1"
    	
    	// Variables for parsing information
    	int major = -1; // Start out major and minor variables at -1 to record they haven't been set yet
    	int minor = -1;
    	boolean ret   = false; // This is the value we'll return
    	boolean error = false; // Set to true if reading text and numbers throws an exception
    	
    	// Make sure the string tokenizer broke the text into at least 3 parts
    	if (tok.countTokens() < 3) return false;
    	
    	try {
    		
    		// Pull out the first 3 tokens
    		String str = tok.nextToken();  // "LimeWire", the name of the client
    		str = tok.nextToken();         // "4", the major version
    		major = Integer.parseInt(str);
    		str = tok.nextToken();         // "8", the minor version
    		minor = Integer.parseInt(str);
    		
    	} catch (NumberFormatException nfx) {
    		
    		// Reading some text as a string didn't work
    		error = true;
    	}
    	
    	// If that worked and the version is 2.anything or less than 3.4, it's old
    	if (!error && (major < 3 || (major == 3 && minor < 4))) ret = true; // Return true, it's old
    	
    	// Return true for old, false for recent
    	return ret;
    }
	
    /**
     * Make a new HandshakeResponse object with "200 OK" and no headers.
     * This is like making a new blank HandshakeResponse object.
     * You can add headers to it later.
     * 
     * @return A new empty HandshakeResponse object
     */
    public static HandshakeResponse createEmptyResponse() {
    	
    	// Make a new HandshakeResponse object from a new blank Properties hash table of strings
    	return new HandshakeResponse(new Properties());
    }
	
    /**
     * Make a new HandshakeResponse object that starts "200 OK" and then has the given headers.
     * 
     * @param headers The group of handshake headers we're going to send
     * @return        A new HandshakeResponse object with "200 OK" followed by those headers
     */
    public static HandshakeResponse createResponse(Properties headers) throws IOException {
    	
    	// Give the constructor the headers, the "200 OK" is there by default
    	return new HandshakeResponse(headers);
    }
	
    /**
     * Wrap and parse the handshake headers from the remote computer into a new HandshakeResponse object.
     * A HandshakeResponse object represents a group of headers.
     * 
     * Connection.concludeOutgoingHandshake() calls this to wrap the remote computer's stage 2 headers.
     * Connection.concludeIncomingHandshake() calls this to wrap the remote computer's stage 1 and stage 3 headers combined.
     * 
     * All this method does is split a line like "503 Service unavailable" into 503 and "Service unavailable", and then call the HandshakeResponse constructor.
     * 
     * @param line    The status line that starts the group of headers from the remote computer, like "503 Service unavailable"
     * @param headers The headers the remote computer sent us after that line, already split into a Properties hash table of strings
     * @return        A new HandshakeResponse object holding the remote computer's group of headers
     */
    public static HandshakeResponse createRemoteResponse(String line, Properties headers) throws IOException {
    	
    	// Get the status number from line, pulling 200 from "200 OK"
    	int code = extractCode(line);
    	if (code == -1) throw new IOException("could not parse status code: " + line); // Finding the number didn't work
    	
    	// Get the status text from line, pulling "OK" from "200 OK"
    	String message = extractMessage(line);
    	if (message == null) throw new IOException("could not parse status message: " + line); // Parsing for the text didn't work
    	
    	// Make a new HandshakeResponse object with that status code, message, and headers from the remote computer
    	return new HandshakeResponse(code, message, headers);        
    }
	
    /**
     * Put the given headers under "200 OK", add "X-Try-Ultrapeers", and return them wrapped in a HandshakeResponse object.
     * 
     * We are a leaf or an ultrapeer.
     * A remote computer connected to us and sent stage 1 headers.
     * The respondToIncoming method composed the stage 2 headers of our response.
     * Now, this method just adds the "X-Try-Ultrapeers" header, puts "200 OK" at the top, and returns it as a new HandshakeResponse object.
     * 
     * @param response The stage 1 headers the remote computer sent us when it connected to us
     * @param headers  The stage 2 headers we've composed for our response
     * @return         A new HandshakeResponse object with "200 OK" and "X-Try-Ultrapeers" added to the given headers
     */
    static HandshakeResponse createAcceptIncomingResponse(HandshakeResponse response, Properties headers) {

    	// Put the given headers under "200 OK", add "X-Try-Ultrapeers", and return the group of headers in a new HandshakeResponse
    	return new HandshakeResponse(addXTryHeader(response, headers));
    }

    /**
     * Make a new default HandshakeResponse object with "200 OK" followed by the given headers.
     * 
     * We are an ultrapeer or a leaf.
     * We initiated a connection and sent stage 1 headers, and got the remote computer's stage 2 headers.
     * Now, this method composes the stage 3 headers we'll send to finish the handshake.
     * 
     * All this method does is take the given headers, add "200 OK" to the top, and send them back.
     * The headers are just "X-Ultrapeer: False" and "Content-Encoding: deflate"
     * 
     * @param headers The headers for this group in a Properties hash table of strings
     * @return        A new HandshakeResponse object that starts "200 OK" and then has those headers
     */
    static HandshakeResponse createAcceptOutgoingResponse(Properties headers) {

    	// Make a new default HandshakeResponse object with "200 OK" followed by the given headers
    	return new HandshakeResponse(headers);
    }
	
    /**
     * Composes our stage 2 response to the crawler when it connects to us.
     * 
     * Called when the crawler connects to us.
     * Composes response headers that tell the crawler what it wants to know.
     * 
     * @return A new HandshakeResponse object that has headers that give information about the network to the crawler
     */
    static HandshakeResponse createCrawlerResponse() {
    	
    	// Make a new empty hash table of strings
    	Properties headers = new Properties();
    	
    	// Add headers like "User-Agent: LimeWire/4.9.33" and "X-Ultrapeer: false"
    	headers.put(HeaderNames.USER_AGENT, CommonUtils.getHttpServer());
    	headers.put(HeaderNames.X_ULTRAPEER, "" + RouterService.isSupernode()); // Adding a blank string converts the boolean false into text like "false"

    	// Say how many leaves we're connected to with a header like "Leaves: 97"
    	List leaves = RouterService.getConnectionManager().getInitializedClientConnections();
    	headers.put(HeaderNames.LEAVES, createEndpointString(leaves, leaves.size()));
    	
    	// Say how many ultrapeers we're connected to with a header like "Peers: 3"
    	List ultrapeers = RouterService.getConnectionManager().getInitializedConnections();
    	headers.put(HeaderNames.PEERS, createEndpointString(ultrapeers, ultrapeers.size()));
    	
    	// Return a group of headers that starts "593 Hi" and then has these 3 headers we added above
    	return new HandshakeResponse(HandshakeResponse.CRAWLER_CODE, HandshakeResponse.CRAWLER_MESSAGE, headers);        
    }
	
    /**
     * We are an ultrapeer.
     * The remote computer connected to us and sent stage 1 headers.
     * Now, this method composes the stage 2 headers of our response.
     * 
     * @param hr The HandshakeResponse object that has the remote computer's stage 1 headers
     * @return   A new HandshakeResponse object with our stage 2 headers that will reject the connection
     */
    static HandshakeResponse createUltrapeerRejectIncomingResponse(HandshakeResponse hr) {

    	// Compose and return headers starting "503 Service unavailable" with the "X-Try-Ultrapeers" header
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL, HandshakeResponse.SLOTS_FULL_MESSAGE, addXTryHeader(hr, new Properties()));        
    }

    /**
     * We are an ultrapeer.
     * We initiated a connection and sent stage 1 headers.
     * Then, the remote computer sent its stage 2 headers.
     * Now, this method composes the stage 3 headers we'll send to finish the handshake with a rejection.
     * We don't include "X-Try-Ultrapeers" because we started this connection and the the remote computer didn't request that information.
     *
     * @param A new HandshakeResponse object with our stage 3 headers that will reject the connection
     */
    static HandshakeResponse createRejectOutgoingResponse() {

    	// Compose and return headers starting "503 Service unavailable"
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL, HandshakeResponse.SLOTS_FULL_MESSAGE, new Properties());
    }

    /**
     * We are a leaf.
     * A remote computer connected to us and sent stage 1 headers.
     * Now, this method composes the stage 2 headers of our response.
     * The headers reject the connection, but tell the remote computer some IP addresses it can try.
     * 
     * @param hr The remote computer's stage 1 headers it sent us when it connected to us
     * @return   A new HandshakeResponse object with our stage 2 headers that will reject the connection
     */
    static HandshakeResponse createLeafRejectIncomingResponse(HandshakeResponse hr) {

    	// Compose and return headers starting "503 I am a shielded leaf node" with the "X-Try-Ultrapeers" header
    	return new HandshakeResponse(HandshakeResponse.SLOTS_FULL, HandshakeResponse.SHIELDED_MESSAGE, addXTryHeader(hr, new Properties()));
    }

    /**
     * We are a leaf.
     * This constructor makes a new HandshakeResponse object.
     * It's filled with headers we can use to reject a connection.
     * 
     * @return A new HandshakeResponse object with rejection headers that start "503 I am a shielded leaf node"
     */
    static HandshakeResponse createLeafRejectOutgoingResponse() {

    	// Compose and return headers starting "503 I am a shielded leaf node"
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL, HandshakeResponse.SHIELDED_MESSAGE);        
    }

    /**
     * We are a leaf.
     * We initiated a connection and sent stage 1 headers.
     * Then, the remote computer sent its stage 2 headers, passed in here as response.
     * Now, this method composes the stage 3 headers we'll send to finish the handshake.
     * We're rejecting the handshake because our language and the remote computer's language don't match.
     * The language preference is stated in the headers like "X-Locale-Pref: en".
     * 
     * @return A new HandshakeResponse object with rejection headers that start "577 Service Not Available"
     */
    static HandshakeResponse createLeafRejectLocaleOutgoingResponse() {
    	
    	// Compose and return headers starting "577 Service Not Available"
        return new HandshakeResponse(HandshakeResponse.LOCALE_NO_MATCH, HandshakeResponse.LOCALE_NO_MATCH_MESSAGE);
    }

    /**
	 * Given a collection of IpPort objects, composes text like "101.2.33.440:6346,211.1.2.22:6346,332.4.55.7:2040".
	 * This is the value of the "X-Try-Ultrapeers" header.
	 * Only writes out 10 IP addresses, even if the collection contains more.
	 *
     * @param hosts A Collection of IP addresses and port numbers of computers that are ultrapeers
     * @return      Text that lists the given IP addresses and port numbers like "ip:port,ip:port,ip:port"
	 */
    private static String createEndpointString(Collection hosts) {

    	// Use the other version of this method, passing it 10
        return createEndpointString(hosts, NUM_X_TRY_ULTRAPEER_HOSTS); // 10
    }
    
	/**
	 * Given a collection of IpPort objects, composes text like "101.2.33.440:6346,211.1.2.22:6346,332.4.55.7:2040".
	 * This is the value for the "X-Try-Ultrapeers" header.
	 * Takes a limit number, like 10, to not write out more IPs than that limit.
	 * 
     * @param hosts A Collection of IP addresses and port numbers of computers that are ultrapeers
     * @param limit The maximum number of IP addresses we want to include in the text, like 10
     * @return      Text that lists the given IP addresses and port numbers like "ip:port,ip:port,ip:port"
	 */
	private static String createEndpointString(Collection hosts, int limit) {
		
		// Make a StringBuffer to hold the text we'll compose
		StringBuffer sb = new StringBuffer(); // StringBuffer is like String, except it can be modified

		// Loop once for each host in the collection, but not more than the limit allows
		int i = 0;                        // Counts how many times we'll loop to stay within the limit
        Iterator iter = hosts.iterator(); // The iterator we'll use to move down the hosts in the collection
		while(iter.hasNext() && i < limit) {

			// Get the IpPort the iterator points to, and move it to the next one in the collection
			IpPort host = (IpPort)iter.next();
			
			// Append text like "IP Address:Port" to the string buffer
			sb.append(host.getAddress());
			sb.append(":");
			sb.append(host.getPort());

			// If we're going to loop again, append a comma to the end of the string buffer
			if(iter.hasNext()) sb.append(",");
			
			// TODO: What if iter.hasNext, but i is at the limit, won't we get a trailing comma?
			
			// Record we added one more IP address and port number to the string
			i++;
		}

		// Convert the string buffer into a string, and return it
		return sb.toString();
	}
	
    /**
     * Extracts the status code from the end of the first line of a group of handshake headers.
     *
     * @param line Text from a remote computer like "200 OK" or "503 Service unavailable"
     * @return     The status code from the text, like 200 or 503, or -1 if finding the number doesn't work
     */
    private static int extractCode(String line) {
    	
    	// Find how far into the string the first space is located
        int statusMessageIndex = line.indexOf(" ");
        if (statusMessageIndex == -1) return -1; // No space found, return -1
        
        try {
        	
        	// Pull out the text from the start to that distance, trim spaces from it, read it as a number, and return it
            return Integer.parseInt(line.substring(0, statusMessageIndex).trim());
            
        } catch(NumberFormatException e) {

        	// Trying to read the text as a number caused an exception, report the error with -1
            return -1;
        }
    }

    /**
     * Extracts the status message from the end of the first line of the group of handshake headers.
     *
     * @param line Text from a remote computer like "200 OK" or "503 Service unavailable"
     * @return     The status message from that text, like "OK" or "Service unavailable"
     *             If parsing for the text doesn't work, returns null, not a blank string
     */
    private static String extractMessage(String line) {

    	// Return the text after the first space
        int statusMessageIndex = line.indexOf(" ");       // Find how far into the string the first space is located
        if (statusMessageIndex == -1) return null;        // No space found, return null
        return line.substring(statusMessageIndex).trim(); // Return the text from that point, trimming any space from the end
    }

    /**
     * Compose and add the "X-Try-Ultrapeers" header.
     * We list hosts with free slots that we've heard from recently.
     * 
     * @param hr      The handshake headers from a remote computer
     * @param headers The headers we're preparing to reply
     * @return        Those same headers with "X-Try-Ultrapeers" added
     */
    private static Properties addXTryHeader(HandshakeResponse hr, Properties headers) {

    	// Get a collection of 10 ultrapeer IP addresses from the RouterService
        Collection hosts = RouterService.getPreferencedHosts(
        		hr.isUltrapeer(),   // True if the remote computer said "X-Ultrapeer: true"
        		hr.getLocalePref(), // The preferred language of the remote user, like "en" for English
        		10);                // We want information about up to 10 ultrapeers

        // Add or overwrite the header "X-Try-Ultrapeers" with text from createEndpointString()
        headers.put(HeaderNames.X_TRY_ULTRAPEERS, createEndpointString(hosts));
        
        // Return the headers with the newly added or changed value for "X-Try-Ultrapeers"
        return headers;
    }

    /**
     * Returns the status code from the first line of the group of headers this object represents.
     * This is a number like 200 or 503.
     * 
     * @return The status code
     */
    public int getStatusCode() {
    	
    	// The constructor saved the number in the STATUS_CODE int
        return STATUS_CODE;
    }
    
    /**
     * Returns the status message from the first line of the group of headers this object represents.
     * This is text like "OK" or "Service Not Available".
     * 
     * @return The status message text
     */
    public String getStatusMessage(){
    	
    	// The constructor saved this text in the STATUS_MESSAGE string
        return STATUS_MESSAGE;
    }
    
    /**
     * Determines if this group of headers starts with a refusal.
     * When a computer rejects a connection, it sends a group of headers that begin with a line like "GNUTELLA/0.6 503 Some Reason".
     * This method just checks for anything other than 200.
     * 
     * @return True if the status code is anything other than 200 OK
     */
    public boolean notOKStatusCode() {

    	// If the status code is not 200 OK, return true
        if (STATUS_CODE != OK) return true;
        else                   return false; // The status code is 200 OK
    }

    /**
     * Determines if this group of headers starts with 200 OK.
     * When a computer accepts a connection, it sends a group of headers that begin with the line "GNUTELLA/0.6 200 OK".
     * 
     * @return True if the status code is 200 OK
     */
    public boolean isAccepted() {
    	
    	// Compare the int STATUS_CODE with 200
        return STATUS_CODE == OK;
    }

    /**
     * The status code and status message as text, like "200 OK" or "503 Service Not Available".
     * 
     * @return The status code and status message with a space between them
     */
    public String getStatusLine() {

    	// Make a new string with the status code and status message separated by a space
        return new String(STATUS_CODE + " " + STATUS_MESSAGE);
    }

    /**
     * Returns the headers stored in this object.
     * 
     * @return The Properties hash table of strings with keys like "X-Ultrapeer-Needed" and values like "false"
     */
    public Properties props() {
    	
    	// Return a reference to the Properties hash table named HEADERS
        return HEADERS;
    }

	/**
	 * Gets the value of a given header name.
	 * Returns null if not found, not a blank string.
	 * 
	 * @param prop The header name to look for, like "X-Name"
	 * @return     The text value of that header, or null if not found
	 */
	public String getProperty(String prop) {
		
		// Look up the value for the given header name in the Properties hash table of strings named HEADERS
		return HEADERS.getProperty(prop);
	}

    /**
     * Reads the value of the "User-Agent" header from the handshake headers stored in this object.
     * If there is no "User-Agent" header, this method doesn't return a blank string, it returns null.
     * 
     * @return The value of the "User-Agent" header, or null if the header isn't here
     */
    public String getUserAgent() {
    	
    	// Look up the value for the "User-Agent" key in the Properties hash table of strings named HEADERS
        return HEADERS.getProperty(HeaderNames.USER_AGENT);
    }

    /**
     * The computer that said these headers doesn't want to run into a packet with a TTL bigger than this number.
     * We must not send it a packet with a bigger TTL.
     * This also means the packets we get from it shouldn't have a higher TTL than this.
     * Reads the number from the "X-Max-TTL" header.
     * 
     * @return The value of "X-Max-TTL" in the headers stored here
     */
    public byte getMaxTTL() {
    	
    	// We've already parsed for the value of the "X-Max-TTL" header
        return MAX_TTL;
    }
    
    /**
     * Read the value of the "X-Try-Ultrapeers" header.
     * If the header isn't here, returns blank.
     *
     * @return The value of "X-Try-Ultrapeers", which is a bunch of IP addresses separated by commas
     */
    public String getXTryUltrapeers() {

    	// Read the value of the "X-Try-Ultrapeers" header
        return extractStringHeaderValue(HEADERS, HeaderNames.X_TRY_ULTRAPEERS);
    }

    /**
     * Determines if the "X-Try-Ultrapeer" header is here.
     * This checks for the existance of the header.
     * If the header is here but has no value, the method still returns true.
     * 
     * @return True if the "X-Try-Ultrapeer" header is here
     */
    public boolean hasXTryUltrapeers() {

    	// Look for the "X-Try-Ultrapeer" header, it's value doesn't matter
    	return headerExists(HEADERS, HeaderNames.X_TRY_ULTRAPEERS);
    }

    /**
     * Determines if the computer that said these headers told us it doesn't need any more ultrapeer connections.
     * This is called leaf guidance.
     * Looks for the header "X-Ultrapeer-Needed: false".
     * 
     * @return True if the headers include "X-Ultrapeer-Needed: false"
     */
    public boolean hasLeafGuidance() {

    	// Look for the "X-Ultrapeer-Needed" header, and make sure its value is false
        return isFalseValue(HEADERS, HeaderNames.X_ULTRAPEER_NEEDED);
    }

	/**
	 * Determines how many connections to ultrapeers the ultrapeer that said these headers says it tries to maintain.
	 * Looks for a header like "X-Degree: 6".
	 * 
	 * @return The value of the "X-Degree" header, the number of ultrapeer to ultrapeer connections
	 */
	public int getNumIntraUltrapeerConnections() {
		
		// We've already parsed for "X-Degree: 6" and saved the number in the member int named DEGREE
		return DEGREE;
	}

	/**
	 * True if the ultrapeer that said these headers said it tries to keep 15 or more connections to other ultrapeers.
	 * If ultrapeers have more ultrapeer connections, packets can travel fewer hops, and the Gnutella network will be more efficient.
	 * 
	 * @return True if there is a header here that says "X-Degree: 15" or higher
	 */
	public boolean isHighDegreeConnection() {

		// We've already parsed for "X-Degree: 15" or higher
        return HIGH_DEGREE;
	}
	
	/**
	 * Determines if it was LimeWire that said these headers.
	 * Looks for "User-Agent: LimeWire/4.9.33" where the value starts "limewire".
	 * 
	 * @return True if these headers were said by LimeWire
	 */
	public boolean isLimeWire() {

		// We've already parsed for this
		return IS_LIMEWIRE;
    }
    
    /**
     * Determines if the computer that said these headers is running an old version of LimeWire.
     * Looks for a header like "User-Agent: LimeWire/3.3.1".
     * Any version 2.* is old, as is 3.1, 3.2, and 3.3. Version 3.4 is the first new version.
     * 
     * @return True if the headers indicate an old version of LimeWire
     */
    public boolean isOldLimeWire() {

    	// We've already parsed the "User-Agent" header and compared the version number
        return IS_OLD_LIMEWIRE;
    }

    /**
     * Determines if the headers describe a computer that is a leaf we'd want to connect to.
     * 
     * @return True if the computer is a leaf that supports a lot of features
     */
    public boolean isGoodLeaf() {

    	// We've already parsed for these headers and set GOOD_LEAF
        return GOOD_LEAF;
    }

    /**
     * Determines if the computer that said these headers is going to send us compressed data.
     * Looks for the header "Content-Encoding: deflate".
     * 
     * @return True if the remote computer is going to send compressed data
     */
    public boolean isDeflateEnabled() {

    	// We've already parsed for "Content-Encoding: deflate" and set DEFLATE_ENCODED
        return DEFLATE_ENCODED;
    }

    /**
     * Determines if we should compress the data we send to the computer that sent these headers.
     * Checks the program's settings to make sure that compression is OK with the user.
     * Checks the headers here to make sure the remote computer said "Accept-Encoding: deflate".
     * 
     * @return True if sending compressed data is OK with the user here and the remote computer there
     */
    public boolean isDeflateAccepted() {

        // Return true if compressing data is OK with the user and the remote computer can accept it
        return ConnectionSettings.ENCODE_DEFLATE.getValue() &&                                       // Program settings allow compressing data
               containsStringValue(HEADERS, HeaderNames.ACCEPT_ENCODING, HeaderNames.DEFLATE_VALUE); // "Accept-Encoding: deflate" is a header here
    }
    
    /**
     * Determines if the headers describe a computer that has the features to make a good ultrapeer.
     * 
     * @return True if the computer supports the advanced features that would make it a good ultrapeer
     */
    public boolean isGoodUltrapeer() {
    	
    	// We've already parsed for these headers and set GOOD_ULTRAPEER
    	return GOOD_ULTRAPEER;
    }

	/**
	 * Determines if the computer that said these headers supports query routing between ultrapeers at 1 hop.
	 * Computers that support this exchange query routing tables with the ultrapeers they are connected to.
	 * Then, they don't have to send a query to a connected ultrapeer if they know it won't have it.
	 * Looks for the header "X-Ultrapeer-Query-Routing: 0.1" or higher.
	 * 
	 * @return True if the headers say "X-Ultrapeer-Query-Routing: 0.1" or higher
	 */
	public boolean isUltrapeerQueryRoutingConnection() {
		
		// We've already parsed for this and set ULTRAPEER_QRP
        return ULTRAPEER_QRP;
    }

	/**
	 * Determines if the computer that said these headers is a leaf.
	 * Looks for the header "X-Ultrapeer: false".
	 * Just because a computer is a leaf doesn't mean it's shielded.
	 * Being a leaf and being in shielded leaf mode are not the same thing.
	 * 
	 * @return True if the computer said it's a leaf
	 */
    public boolean isLeaf() {

    	// We've already parsed for "X-Ultrapeer: false" and set LEAF
        return LEAF;
    }

    /**
     * Determines if the computer that said these headers is an ultrapeer.
     * Looks for the header "X-Ultrapeer: true".
     * 
     * @return True if the computer said it's an ultrapeer
     */
    public boolean isUltrapeer() {
    	
    	// We've already parsed for "X-Ultrapeer: true" and set ULTRAPEER
        return ULTRAPEER;
    }


	/**
	 * Determines if the computer that said these headers can do GUESS.
	 * Looks for a header like "X-Guess: 0.1" or higher.
	 *
	 * @return True if the computer says it can do GUESS
	 */
	public boolean isGUESSCapable() {

		// We've already parsed for "X-Guess: 0.1" and set GUESS_CAPABLE
		return GUESS_CAPABLE;
	}

	/**
	 * Determines if the computer that said these headers is an ultrapeer that can do GUESS.
	 * Looks for "X-Guess: 0.1" and "X-Ultrapeer: true".
	 * 
	 * @return True if both headers are found
	 */
	public boolean isGUESSUltrapeer() {
		
		// Look for "X-Guess: 0.1" or higher and "X-Ultrapeer: true"
		return isGUESSCapable() && isUltrapeer();
	}

    /**
     * Determines if this connection is temporary.
     * Looks for the "X-Temp-Connection: true" header.
     * Returns false if the value is false, or the header isn't there.
     * 
     * @return True if the headers include "X-Temp-Connection: true"
     */
    public boolean isTempConnection() {

    	// Look for the "X-Temp-Connection" header
        String value = HEADERS.getProperty(HeaderNames.X_TEMP_CONNECTION);
        if (value == null) return false; // Not found

        // Return the value of the header, true or false
        return Boolean.valueOf(value).booleanValue();
    }

    /**
     * Determines if the computer that said these headers supports GGEP.
     * This means the computer can read and write GGEP blocks in packets, usually pongs, making big pongs.
     * Before sending a big pong to a remote computer, make sure supportsGGEP is true for it.
     * Looks for a header like "GGEP: 0.5".
     * The version number doesn't matter, returns true if the header is mentioned with any version number.
     * 
     * @return True if the headers include "GGEP:"
     */
    public boolean supportsGGEP() {
    	
    	// If we haven't setup the member variable _supportsGGEP, set it up now
        if (_supportsGGEP == null) {
        	
        	// Look for a header like "GGEP: 0.5" and get the value "0.5"
			String value = HEADERS.getProperty(HeaderNames.GGEP);
			
			// The version number doesn't matter, if getProperty found any value, make _supportsGGEP true
            _supportsGGEP = new Boolean(value != null);
		}
        
        // Return true if one of the headers is "GGEP", false if not mentioned
        return _supportsGGEP.booleanValue();
    }

	/**
	 * Determines what version of vendor specific messages the computer that said these headers supports.
	 * Looks for the header "Vendor-Message: 0.1" and parses out the 0.1.
	 * If the vendor message header isn't here, returns 0.
	 * 
	 * @return The value of "Vendor-Message: 0.1" as a floating point number, like 0.1, or 0 for not found
	 */
	public float supportsVendorMessages() {
		
		// Look for the "Vendor-Message" header
		String value = HEADERS.getProperty(HeaderNames.X_VENDOR_MESSAGE);

		// If we found the header in the hash table and it has a value with some text
		if ((value != null) && !value.equals("")) {

			try {
				
				// Read the text value like "0.1" as a floating point number
                return Float.parseFloat(value);

			} catch (NumberFormatException nfe) {

				// Trying to read the text as a number caused an exception, return 0 for not found
				return 0;
            }
		}
		
		// Return 0 for not found
		return 0;
	}

    /**
     * Determines if the computer that said these headers supports pong caching.
     *
     * @return True if the headers include "Pong-Caching: 0.1" or higher
     */
    public boolean supportsPongCaching() {
    	
    	// We've already parsed for this and set PONG_CACHING
        return PONG_CACHING;
    }
    
    /**
     * Looks for a header like "X-Version: 1.2.3", meaning the remote computer can give us a digitally signed copy of this version of LimeWire. (do)
     * 
     * @return The "1.2.3" value of the header as a string
     */
	public String getVersion() {

		// Look for the "X-Version" header, and return its value
		return HEADERS.getProperty(HeaderNames.X_VERSION); // Returns a blank string if the header isn't found
	}

    /**
     * Determines if the computer that said these headers supports QRP, the query routing protocol.
     * QRP is only used between leaves and ultrapeers.
     * 
     * @return True if the headers include "X-Query-Routing: 0.1" or a higher version number
     */
    public boolean isQueryRoutingEnabled() {

    	// Look for a header like "X-Query-Routing: 0.1" or higher
        return isVersionOrHigher(HEADERS, HeaderNames.X_QUERY_ROUTING, 0.1F);
    }

    /**
     * Determines if the computer that said these headers uses dynamic querying.
     * True if the headers here include "X-Dynamic-Querying: 0.1" or a higher version number.
     * 
     * @return True if the headers include "X-Dynamic-Querying: 0.1" or a higher version number
     */
    public boolean isDynamicQueryConnection() {

    	// We already parsed for this and set DYNAMIC_QUERY
    	return DYNAMIC_QUERY;
    }

    /**
     * Determines if the computer that said these headers supports TTL = 1 probe queries.
     * Looks for the header "X-Ext-Probes: 0.1" or a higher version number.
     * Probe queries are treated separately from other queries.
     * If we get a second probe query with the same GUID, we don't consider it to be a duplicate.
     * 
     * @return True if the headers say "X-Ext-Probes: 0.1" or a higher version number
     */
    public boolean supportsProbeQueries() {

    	// We already parsed for this and set PROBE_QUERIES
    	return PROBE_QUERIES;
    }
    
	/**
	 * Determines if this group of handshake headers is from the crawler.
	 * The crawler sends a header like "Crawler: 0.1" or a higher version number.
	 * 
	 * @return True if it's the crawler that sent these headers, including one like "Crawler: 0.1"
	 */
	public boolean isCrawler() {
		
		// We already parsed for this and set IS_CRAWLER
		return IS_CRAWLER;
	}

    /**
     * The headers here might include one like "X-Locale-Pref: en".
     * This method will return the value of that header, like "en" for English.
     * 
     * The remote computer probably didn't send a "X-Local-Pref" header at all.
     * In that case, this is the default value from our settings, like "en" for the English version language of LimeWire.
     * This artificially makes it look like a remote computer that doesn't have a locale preference actually matches our language. (do)
     * 
     * @return The value of the "X-Locale-Pref: en" header, like "en" for English
     */
    public String getLocalePref() {
    	
    	// We already parsed for and saved this value in the LOCALE_PREF member string
        return LOCALE_PREF;
    }

    /**
     * Determines if a header name is present in a Properties hash table of headers.
     * 
     * @param headers    The Properties hash table of headers
     * @param headerName The header name to look for, like "X-Name"
     * @return           True if the header is there, false if it's not
     */
    private static boolean headerExists(Properties headers, String headerName) {

    	// Look up the value of the given header just to see if it's there
        String value = headers.getProperty(headerName);

        // If we found a value, the header must be there, return true
        return value != null;
    }

    /**
     * Determines if a header is present and has the value true.
     * Looks for a header like "X-Name: True", case insensitive.
     *
     * @param headers    The Properties hash table of headers with keys like "X-Name" and values like "true"
     * @param headerName The header name to look up, like "X-Name"
     * @return           True if the header is there and has the value true, false if it's not there or has the value false
     */
    private static boolean isTrueValue(Properties headers, String headerName) {

    	// Look up the value of the given header name from the Properties hash table of strings
        String value = headers.getProperty(headerName);
        if (value == null) return false; // Not found
        
        // If the value is true, return true
        return Boolean.valueOf(value).booleanValue(); // This works with both "true" and "True"
    }

    /**
     * Determines if a header is present and has the value false.
     * Looks for a header like "X-Name: False", case insensitive.
     *
     * @param headers    The Properties hash table of headers with keys like "X-Name" and values like "false"
     * @param headerName The header name to look up, like "X-Name"
     * @return           True if the header is there and has the value false, false if it's not there or has the value true
     */
    private static boolean isFalseValue(Properties headers, String headerName) {

    	// Look up the value of the given header name from the Properties hash table of strings
        String value = headers.getProperty(headerName);
        if (value == null) return false; // Not found

        // If the value is false, return true
        return value.equalsIgnoreCase("false");
    }
    
    /**
     * Looks for a header value.
     * Isn't case sensitive.
     * This method will find "value" in a header like "X-Name: Value".
     *
     * @param headers     The Properties hash table of headers with keys like "X-Name" and values like "value"
     * @param headerName  The header name we're looking up, like "X-Name"
     * @param headerValue The header value to look for, like "value"
     * @return            True if the header name is there and has that value
     */
    private static boolean isStringValue(Properties headers, String headerName, String headerValue) {

    	// Look up the value of the given header name from the Properties hash table of strings
        String value = headers.getProperty(headerName);
        if (value == null) return false; // Not found

        // Return true if the strings match ignoring case, false if they are different
        return value.equalsIgnoreCase(headerValue);
    }

    /**
     * Looks for a header value.
     * Supports multiple values separated by commas, and isn't case sensitive.
     * This method can find "value2" in a header like "X-Name: Value1, Value2, Value3".
     *
     * @param headers     The Properties hash table of headers with keys like "X-Name" and values like "value1, value2, value3"
     * @param headerName  The header name we're looking up, like "X-Name"
     * @param headerValue The header value to look for, like "value2"
     * @return            True if the header name is there and has a value like that in its list
     */
    private static boolean containsStringValue(Properties headers, String headerName, String headerValue) {

    	// Look up the value of the given header name from the Properties hash table of strings
        String value = headers.getProperty(headerName);
        if (value == null) return false; // Not found

        // If the value is what we want, report found now to avoid making the StringTokenizer
        if (value.equalsIgnoreCase(headerValue)) return true;

        // Break value, which is like "value1, value2, value3", around comma into separate strings
        StringTokenizer st = new StringTokenizer(value, ","); // This also trims spaces off them

        // Loop once for each string that the StringTokenizer broke apart
        while (st.hasMoreTokens()) {

        	// If this is the value we're looking for, return true
            if(st.nextToken().equalsIgnoreCase(headerValue)) return true;
        }

        // We couldn't find the value we wanted
        return false;
    }    

    /**
     * Looks for a header in a hash table of them, reads its value like "0.1", and determines if that's high enough.
     * 
     * @param headers      The Properties hash table of strings that contains header names and values
     * @param headerName   The header name to look up, like "X-Query-Routing"
     * @param minVersion   A float like 0.2 to make sure the value is "0.2", "0.3", or higher
     * @return             True if the header is there and the version is the same or higher
     */
    private static boolean isVersionOrHigher(Properties headers, String headerName, float minVersion) {
    	
    	// Look up the value of the given header name from the Properties hash table of strings
        String value = headers.getProperty(headerName);
        if (value == null) return false; // Not found, feature not supported, return false

        try {

        	// The value is a string like "0.1", read it as a floating point number
            Float f = new Float(value);

            // Return true if it's the version we want or an even newer version
            return f.floatValue() >= minVersion;

        } catch (NumberFormatException e) {

        	// Turning the text into a number caused an exception, report feature not supported
            return false;
        }        
    }

    /**
     * Looks for a header in a hash table of them, reads its numerical value, and returns it as an int.
     * If the header isn't there or can't be parsed, this method returns the default value you pass it.
     * 
     * @param headers      The Properties hash table of strings that contains header names and values
     * @param headerName   The header name to look up, like "X-Degree"
     * @param defaultValue The value you want this method to return if it can't find that header
     * @return             The numberical value of that header returned as an int
     */
    private static int extractIntHeaderValue(Properties headers, String headerName, int defaultValue) {

    	// Look up the value of the given header name from the Properties hash table of strings
        String value = headers.getProperty(headerName);
        if (value == null) return defaultValue; // Not found, return default value

		try {

			// The string value is like "3", turn it into the int 3 and return it as that number
			return Integer.valueOf(value).intValue();

		} catch(NumberFormatException e) {

			// Turning the text into a number caused an exception, just return the default value
			return defaultValue;
		}
    }

    /**
     * Looks for a header in a hash table of them, reads its numerical value, and returns it as a byte.
     * If the header isn't there or can't be parsed, this method returns the default value you pass it.
     * 
     * @param headers      The Properties hash table of strings that contains header names and values
     * @param headerName   The header name to look up, like "X-Max-TTL"
     * @param defaultValue The value you want this method to return if it can't find that header
     * @return             The numberical value of that header returned as a byte
     */
    private static byte extractByteHeaderValue(Properties headers, String headerName, byte defaultValue) {
    	
    	// Look up the value of the given header name from the Properties hash table of strings
        String value = headers.getProperty(headerName);
        if (value == null) return defaultValue; // Not found, return default value
        
		try {

			// The string value is like "3", turn it into the byte 3 and return it as that number
			return Byte.valueOf(value).byteValue();
			
		} catch(NumberFormatException e) {
			
			// Turning the text into a number caused an exception, just return the default value
			return defaultValue;
		}
    }

    /**
     * Looks for a header in a hash table of them, reads its text value, and returns it.
     * If the header isn't there, returns a blank string.
     * 
     * @param headers    The Properties hash table of strings that contains header names and values
     * @param headerName The header name to look up, like "User-Agent"
     * @return           The text value of the header, or an empty string if the header isn't there
     */
    private static String extractStringHeaderValue(Properties headers, String headerName) {
    	
    	// Look up the value of that header name from the Properties hash table of strings, and return it
        String value = headers.getProperty(headerName);
        if (value == null) return ""; // Not found, return blank string
        return value;
    }
    
    /**
     * If you have a HandshakeResponse object and call toString() on it, this method will do it.
     * Composes text like "<200, OK>" followed by the headers as Properties.toString().
     */
    public String toString() {
    	
    	// Compose and return text like "<200, OK>" followed by the text that Properties.toString() puts out
    	return "<" + STATUS_CODE + ", " + STATUS_MESSAGE + ">" + HEADERS;
    }
}

padkage com.limegroup.gnutella.handshaking;

import java.io.IOExdeption;
import java.util.Colledtion;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.settings.ApplicationSettings;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.IpPort;

/**
 * This dlass contains the necessary information to form a response to a 
 * donnection handshake.  It contains a status code, a status message, and
 * the headers to use in the response.
 *
 * There are only two ways to dreate a HandshakeResponse.
 *
 * 1) Create an instande which defaults the status code and status message to
 *    ae "200 OK".  Only the hebders used in the response need to be passed in.
 * 
 * 2) Create an instande with a custom status code, status message, and the
 *    headers used in the response.
 */
pualid finbl class HandshakeResponse {

    /**
     * The "default" status dode in a connection handshake indicating that
     * the handshake was sudcessful and the connection can be established.
     */
    pualid stbtic final int OK = 200;
    
    /**
     * The "default" status message in a donnection handshake indicating that
     * the handshake was sudcessful and the connection can be established.
     */
    pualid stbtic final String OK_MESSAGE = "OK";
    
    /**
     * HTTP response dode for the crawler.
     */
    pualid stbtic final int CRAWLER_CODE = 593;
    
    /**
     * HTTP response message for the drawler.
     */
    pualid stbtic final String CRAWLER_MESSAGE = "Hi";

    /** The error dode that a shielded leaf node should give to incoming
     *  donnections.  */
    pualid stbtic final int SHIELDED = 503;
    /** The error message that a shielded leaf node should give to indoming
     *  donnections.  */
    pualid stbtic final String SHIELDED_MESSAGE = "I am a shielded leaf node";

    /** The error dode that a node with no slots should give to incoming
     *  donnections.  */
    pualid stbtic final int SLOTS_FULL = 503;
    /** The error message that a node with no slots should give to indoming
     *  donnections.  */
    pualid stbtic final String SLOTS_FULL_MESSAGE = "Service unavailable";
    
    /**
     * Default bad status dode to be used while rejecting connections
     */
    pualid stbtic final int DEFAULT_BAD_STATUS_CODE = 503;
    
    /**
     * Default bad status message to be used while rejedting connections
     */
    pualid stbtic final String DEFAULT_BAD_STATUS_MESSAGE 
        = "Servide Not Available";
    
    /**
     * ??? TODO: dheck about this error code...
     */
    pualid stbtic final int LOCALE_NO_MATCH = 577;
    pualid stbtic final String LOCALE_NO_MATCH_MESSAGE 
        = "Servide Not Available";

    /**
     * HTTP-like status dode used when handshaking (e.g., 200, 401, 503).
     */
    private final int STATUS_CODE;

    /**
     * Message used with status dode when handshaking (e.g., "OK, "Service Not
     * Available").  The status message together with the status dode make up 
     * the status line (i.e., first line) of an HTTP-like response to a 
     * donnection handshake.
     */
    private final String STATUS_MESSAGE;

    /**
     * Headers to use in the response to a donnection handshake.
     */
    private final Properties HEADERS;

    /** 
	 * is the GGEP header set?  
	 */
    private Boolean _supportsGGEP;
    
    /**
     * Cadhed boolean for whether or not this is considered a considered a
     * "good" leaf donnection.
     */
    private final boolean GOOD_LEAF;

    /**
     * Cadhed boolean for whether or not this is considered a considered a
     * "good" ultrapeer donnection.
     */
    private final boolean GOOD_ULTRAPEER;

    /**
     * Cadhed value for the number of Ultrapeers this Ultrapeer attempts
     * to donnect to.
     */
    private final int DEGREE;

    /**
     * Cadhed value for whether or not this is a high degree connection.
     */
    private final boolean HIGH_DEGREE;

    /**
     * Cadhed value for whether or not this is an Ultrapeer connection that
     * supports Ultrapeer query routing.
     */
    private final boolean ULTRAPEER_QRP;

    /**
     * Cadhed value for the maximum TTL to use along this connection.
     */
    private final byte MAX_TTL;

    /**
     * Cadhed value for whether or not this connection supports dynamic
     * querying.
     */
    private final boolean DYNAMIC_QUERY;

    /**
     * Cadhed value for whether or not this connection reported
     * X-Ultrapeer: true in it's handshake headers.
     */
    private final boolean ULTRAPEER;

    /**
     * Cadhed value for whether or not this connection reported
     * X-Ultrapeer: false in it's handshake headers.
     */
    private final boolean LEAF;
    
    /**
     * Cadhed value for whether or not the connection reported
     * Content-Endoding: deflate
     */
    private final boolean DEFLATE_ENCODED;

    /**
     * Constant for whether or not this donnection supports probe
     * queries.
     */
    private final boolean PROBE_QUERIES;

    /**
     * Constant for whether or not this node supports pong daching.
     */
    private final boolean PONG_CACHING;

    /**
     * Constant for whether or not this node supports GUESS.
     */
    private final boolean GUESS_CAPABLE;
    
	/**
	 * Constant for whether or not this is a drawler.
	 */
	private final boolean IS_CRAWLER;
	
	/**
	 * Constant for whether or not this node is a LimeWire (or derivative)
	 */
	private final boolean IS_LIMEWIRE;
    
    /**
     * Constant for whether or nor this node is an older limewire. 
     */
    private final boolean IS_OLD_LIMEWIRE;
    
    /**
     * Constant for whether or not the dlient claims to do no requerying.
     */
    private final boolean NO_REQUERYING;
    
    /**
     * Lodale 
     */
    private final String LOCALE_PREF;

    /**
     * Constant for the number of hosts to return in X-Try-Ultrapeer headers.
     */
    private statid final int NUM_X_TRY_ULTRAPEER_HOSTS = 10;

    /**
     * Creates a <tt>HandshakeResponse</tt> whidh defaults the status code and 
     * status message to be "200 Ok" and uses the desired headers in the 
     * response. 
     * 
     * @param headers the headers to use in the response. 
     */
    private HandshakeResponse(Properties headers) {
        this(OK, OK_MESSAGE, headers);
    }    

    /**
     * Creates a new <tt>HandshakeResponse</tt> instande with the specified 
     * response dode and message and with no extra connection headers.
     *
     * @param dode the status code for the response
     * @param message the status message
     */
    private HandshakeResponse(int dode, String message) {
        this(dode, message, new Properties());
    }
    /**
     * Creates a HandshakeResponse with the desired status dode, status message, 
     * and headers to respond with.
     * @param dode the response code to use.
     * @param message the response message to use.
     * @param headers the headers to use in the response.
     */
    HandshakeResponse(int dode, String message, Properties headers) { 
        STATUS_CODE = dode;
        STATUS_MESSAGE = message;
        HEADERS = headers;
        DEGREE = extradtIntHeaderValue(HEADERS, HeaderNames.X_DEGREE, 6);         
        HIGH_DEGREE = getNumIntraUltrapeerConnedtions() >= 15;
        ULTRAPEER_QRP = 
            isVersionOrHigher(HEADERS, 
                              HeaderNames.X_ULTRAPEER_QUERY_ROUTING, 0.1F);
        MAX_TTL = extradtByteHeaderValue(HEADERS, HeaderNames.X_MAX_TTL, 
                                         (ayte)4);
        DYNAMIC_QUERY = 
            isVersionOrHigher(HEADERS, HeaderNames.X_DYNAMIC_QUERY, 0.1F);
        PROBE_QUERIES = 
            isVersionOrHigher(HEADERS, HeaderNames.X_PROBE_QUERIES, 0.1F);
        NO_REQUERYING = isFalseValue(HEADERS, HeaderNames.X_REQUERIES);

        IS_LIMEWIRE =
            extradtStringHeaderValue(headers, HeaderNames.USER_AGENT).
                toLowerCase().startsWith("limewire");

        
        GOOD_ULTRAPEER = isHighDegreeConnedtion() &&
            isUltrapeerQueryRoutingConnedtion() &&
            (getMaxTTL() < 5) &&
            isDynamidQueryConnection();
            
        GOOD_LEAF = GOOD_ULTRAPEER && (IS_LIMEWIRE || NO_REQUERYING); 
        
        ULTRAPEER = isTrueValue(HEADERS, HeaderNames.X_ULTRAPEER);
        LEAF = isFalseValue(HEADERS, HeaderNames.X_ULTRAPEER);
        DEFLATE_ENCODED = isStringValue(HEADERS,
            HeaderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
        PONG_CACHING = 
            isVersionOrHigher(headers, HeaderNames.X_PONG_CACHING, 0.1F);
        GUESS_CAPABLE = 
            isVersionOrHigher(headers, HeaderNames.X_GUESS, 0.1F);
        IS_CRAWLER = 
        	isVersionOrHigher(headers, HeaderNames.CRAWLER, 0.1F);
        IS_OLD_LIMEWIRE = IS_LIMEWIRE && 
        oldVersion(extradtStringHeaderValue(headers, HeaderNames.USER_AGENT));

        String lod  = extractStringHeaderValue(headers, 
                                               HeaderNames.X_LOCALE_PREF);
        LOCALE_PREF = (lod.equals(""))?
            ApplidationSettings.DEFAULT_LOCALE.getValue():
            lod;
    }
    
    /**
     * @return true if the version of limewire we are donnected to is old
     */
    private boolean oldVersion(String userAgent) {
        StringTokenizer tok = new StringTokenizer(userAgent,"/.");
            int major = -1;
            int minor = -1;
            aoolebn ret = false;
            aoolebn error = false;
            if(tok.dountTokens() < 3) //not limewire
                return false;
            try {
                String str = tok.nextToken();//"limewire"
                str = tok.nextToken();
                major = Integer.parseInt(str);
                str = tok.nextToken();
                minor = Integer.parseInt(str);
            } datch (NumberFormatException nfx) {
                error = true;
            } 
            if(!error && (major<3 || (major==3 && minor < 4)) )
                ret  = true;
            return ret;
    }

    /**
     * Creates an empty response with no headers.  This is useful, for 
     * example, during donnection handshaking when we haven't yet read
     * any headers.
     *
     * @return a new, empty <tt>HandshakeResponse</tt> instande
     */
    pualid stbtic HandshakeResponse createEmptyResponse() {
        return new HandshakeResponse(new Properties());
    }
    
    /**
     * Construdts the response from the other host during connection
     * handshaking.
     *
     * @return a new <tt>HandshakeResponse</tt> instande with the headers
     *  sent ay the other host
     */
    pualid stbtic HandshakeResponse 
        dreateResponse(Properties headers) throws IOException {
        return new HandshakeResponse(headers);
    }
    
    /**
     * Construdts the response from the other host during connection
     * handshaking.  The returned response dontains the connection headers
     * sent ay the remote host.
     *
     * @param line the status line redeived from the connecting host
     * @param headers the headers redeived from the other host
     * @return a new <tt>HandshakeResponse</tt> instande with the headers
     *  sent ay the other host
     * @throws <tt>IOExdeption</tt> if the status line could not be parsed
     */
    pualid stbtic HandshakeResponse 
        dreateRemoteResponse(String line,Properties headers) throws IOException{
        int dode = extractCode(line);
        if(dode == -1) {
            throw new IOExdeption("could not parse status code: "+line);
        }
        String message = extradtMessage(line);
        if(message == null) {
            throw new IOExdeption("could not parse status message: "+line);
        }
        return new HandshakeResponse(dode, message, headers);        
    }
    
    /**
     * Creates a new <tt>HandshakeResponse</tt> instande that accepts the
     * potential donnection.
     *
     * @param headers the <tt>Properties</tt> instande containing the headers
     *  to send to the node we're adcepting
     */
    statid HandshakeResponse createAcceptIncomingResponse(
        HandshakeResponse response, Properties headers) {
        return new HandshakeResponse(addXTryHeader(response, headers));
    }


    /**
     * Creates a new <tt>HandshakeResponse</tt> instande that accepts the
     * outgoing donnection -- the final third step in the handshake.  This
     * passes no headers, as all nedessary headers have already been 
     * exdhanged.  The only possible exception is the potential inclusion
     * of X-Ultrapeer: false.
     *
     * @param headers the <tt>Properties</tt> instande containing the headers
     *  to send to the node we're adcepting
     */
    statid HandshakeResponse createAcceptOutgoingResponse(Properties headers) {
        return new HandshakeResponse(headers);
    }

	/**
	 * Creates a new <tt>HandshakeResponse</tt> instande that responds to a
	 * spedial crawler connection with connected leaves and Ultrapeers.  See the 
	 * Files>>Development sedtion on the GDF.
	 *
	 * @param headers the <tt>Properties</tt> instande containing the headers
	 *  to send to the node we're rejedting
	 */
	statid HandshakeResponse createCrawlerResponse() {
		Properties headers = new Properties();
		
        // add our user agent
        headers.put(HeaderNames.USER_AGENT, CommonUtils.getHttpServer());
        headers.put(HeaderNames.X_ULTRAPEER, ""+RouterServide.isSupernode());
        
		// add any leaves
        List leaves = 
            RouterServide.getConnectionManager().
                getInitializedClientConnedtions();
		headers.put(HeaderNames.LEAVES, 
            dreateEndpointString(leaves, leaves.size()));

		// add any Ultrapeers
        List ultrapeers = 
            RouterServide.getConnectionManager().getInitializedConnections();
		headers.put(HeaderNames.PEERS,
			dreateEndpointString(ultrapeers, ultrapeers.size()));
			
		return new HandshakeResponse(HandshakeResponse.CRAWLER_CODE,
			HandshakeResponse.CRAWLER_MESSAGE, headers);        
	}
	
    /**
     * Creates a new <tt>HandshakeResponse</tt> instande that rejects the
     * potential donnection.  This includes the X-Try-Ultrapeers header to
     * tell the remote host about other nodes to donnect to.  We return the
     * hosts we most redently knew to have free leaf or ultrapeer connection
     * slots.
     *
     * @param hr the <tt>HandshakeResponse</tt> dontaining the connection
     *  headers of the donnecting host
     * @return a <tt>HandshakeResponse</tt> with the appropriate response 
     *  headers
     */
    statid HandshakeResponse 
        dreateUltrapeerRejectIncomingResponse(HandshakeResponse hr) {
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL,
            HandshakeResponse.SLOTS_FULL_MESSAGE,
            addXTryHeader(hr, new Properties()));        
    }


    /**
     * Creates a new <tt>HandshakeResponse</tt> instande that rejects the
     * potential donnection.  The returned <tt>HandshakeResponse</tt> DOES
     * NOT indlude the X-Try-Ultrapeers header because this is an outgoing
     * donnection, and we should not send host data that the remote client
     * does not request.
     *
     * @param headers the <tt>Properties</tt> instande containing the headers
     *  to send to the node we're rejedting
     */
    statid HandshakeResponse createRejectOutgoingResponse() {
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL,
                                     HandshakeResponse.SLOTS_FULL_MESSAGE,
                                     new Properties());        
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instande that rejects the
     * potential donnection to a leaf.  We add hosts that we know about with
     * free donnection slots to the X-Try-Ultrapeers header.
     *
     * @param headers the <tt>Properties</tt> instande containing the headers
     *  to send to the node we're rejedting
     * @param hr the <tt>HandshakeResponse</tt> dontaining the headers of the
     *  remote host
     * @return a new <tt>HandshakeResponse</tt> instande rejecting the 
     *  donnection and with the specified connection headers
     */
    statid HandshakeResponse 
        dreateLeafRejectIncomingResponse(HandshakeResponse hr) {
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL,
            HandshakeResponse.SHIELDED_MESSAGE,
            addXTryHeader(hr, new Properties()));  
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instande that rejects an 
     * outgoing leaf donnection.  This occurs when we, as a leaf, reject a 
     * donnection on the third stage of the handshake.
     *
     * @return a new <tt>HandshakeResponse</tt> instande rejecting the 
     *  donnection and with no extra headers
     */
    statid HandshakeResponse createLeafRejectOutgoingResponse() {
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL,
                                     HandshakeResponse.SHIELDED_MESSAGE);        
    }

    statid HandshakeResponse createLeafRejectLocaleOutgoingResponse() {
        return new HandshakeResponse(HandshakeResponse.LOCALE_NO_MATCH,
                                     HandshakeResponse.LOCALE_NO_MATCH_MESSAGE);
    }

    /**
     * Creates a new String of hosts, limiting the number of hosts to add to
     * the default value of 10.  This is partidularly used for the 
     * X-Try-Ultrapeers header.
     * 
     * @param iter a <tt>Colledtion</tt> of <tt>IpPort</tt> instances
     * @return a string of the form IP:port,IP:port,... from the given list of 
     *  hosts
     */
    private statid String createEndpointString(Collection hosts) {
        return dreateEndpointString(hosts, NUM_X_TRY_ULTRAPEER_HOSTS);
    }
    
	/**
	 * Utility method that takes the spedified list of hosts and returns a
	 * string of the form:<p>
	 *
	 * IP:port,IP:port,IP:port
	 *
     * @param iter a <tt>Colledtion</tt> of <tt>IpPort</tt> instances
	 * @return a string of the form IP:port,IP:port,... from the given list of 
     *  hosts
	 */
	private statid String createEndpointString(Collection hosts, int limit) {
		StringBuffer sa = new StringBuffer();
        int i = 0;
        Iterator iter = hosts.iterator();
		while(iter.hasNext() && i<limit) {
            IpPort host = (IpPort)iter.next();
			sa.bppend(host.getAddress());
			sa.bppend(":");
			sa.bppend(host.getPort());
			if(iter.hasNext()) {
				sa.bppend(",");
			}
            i++;
		}
		return sa.toString();
	}

	
    /**
     * Utility method to extradt the connection code from the connect string,
     * sudh as "200" in a "200 OK" message.
     *
     * @param line the full donnection string, such as "200 OK."
     * @return the status dode for the connection string, or -1 if the code
     *  dould not ae pbrsed
     */
    private statid int extractCode(String line) {
        //get the status dode and message out of the status line
        int statusMessageIndex = line.indexOf(" ");
        if(statusMessageIndex == -1) return -1;
        try {
            return Integer.parseInt(line.substring(0, statusMessageIndex).trim());
        } datch(NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Utility method to extradt the connection message from the connect string,
     * sudh as "OK" in a "200 OK" message.
     *
     * @param line the full donnection string, such as "200 OK."
     * @return the status message for the donnection string
     */
    private statid String extractMessage(String line) {
        //get the status dode and message out of the status line
        int statusMessageIndex = line.indexOf(" ");
        if(statusMessageIndex == -1) return null;
        return line.suastring(stbtusMessageIndex).trim();
    }

    /**
     * Utility method for dreating a set of headers with the X-Try-Ultrapeers
     * header set adcording to the headers from the remote host.
     * 
     * @param hr the <tt>HandshakeResponse</tt> of the indoming request
     * @return a new <tt>Properties</tt> instande with the X-Try-Ultrapeers
     *  header set adcording to the incoming headers from the remote host
     */
    private statid Properties addXTryHeader(HandshakeResponse hr, Properties headers) {
        Colledtion hosts =
            RouterServide.getPreferencedHosts(
                hr.isUltrapeer(), hr.getLodalePref(),10);
        
        headers.put(HeaderNames.X_TRY_ULTRAPEERS,
                    dreateEndpointString(hosts));
        return headers;
    }

    /** 
     * Returns the response dode.
     */
    pualid int getStbtusCode() {
        return STATUS_CODE;
    }
    
    /**
     * Returns the status message. 
     * @return the status message (e.g. "OK" , "Servide Not Available" etc.)
     */
    pualid String getStbtusMessage(){
        return STATUS_MESSAGE;
    }
    
    /**
     * Tells if the status returned was OK or not.
     * @return true, if the status returned was not the OK status, false
     * otherwise
     */
    pualid boolebn notOKStatusCode(){
        if(STATUS_CODE != OK)
            return true;
        else
            return false;
    }
    

    /**
     * Returns whether or not this donnection was accepted -- whether
     * or not the donnection returned Gnutella/0.6 200 OK
     *
     * @return <tt>true</tt> if the server returned Gnutella/0.6 200 OK,
     *  otherwise <tt>false</tt>
     */
    pualid boolebn isAccepted() {
        return STATUS_CODE == OK;
    }

    /**
     * Returns the status dode and status message together used in a 
     * status line. (e.g., "200 OK", "503 Servide Not Available")
     */
    pualid String getStbtusLine() {
        return new String(STATUS_CODE + " " + STATUS_MESSAGE);
    }

    /**
     * Returns the headers as a <tt>Properties</tt> instande.
     */
    pualid Properties props() {
        return HEADERS;
    }

	/**
	 * Adcessor for an individual property.
	 */
	pualid String getProperty(String prop) {
		return HEADERS.getProperty(prop);
	}

    /** Returns the vendor string reported ay this donnection, i.e., 
     *  the USER_AGENT property, or null if it wasn't set.
     *  @return the vendor string, or null if unknown */
    pualid String getUserAgent() {
        return HEADERS.getProperty(HeaderNames.USER_AGENT);
    }

    /**
     * Returns the maximum TTL that queries originating from us and 
     * sent from this donnection should have.  If the max TTL header is
     * not present, the default TTL is assumed.
     *
     * @return the maximum TTL that queries sent to this donnection
     *  should have -- this will always be 5 or less
     */
    pualid byte getMbxTTL() {
        return MAX_TTL;
    }
    
    /**
     * Adcessor for the X-Try-Ultrapeers header.  If the header does not
     * exist or is empty, this returns the emtpy string.
     *
     * @return the string of X-Try-Ultrapeer hosts, or the empty string
     *  if they do not exist
     */
    pualid String getXTryUltrbpeers() {
        return extradtStringHeaderValue(HEADERS, HeaderNames.X_TRY_ULTRAPEERS);
    }

    /**
     * This is a donvenience method to see if the connection passed 
     * the X-Try-Ultrapeer header.  This simply dhecks the existence of the
     * header -- if the header was sent but is empty, this still returns
     * <tt>true</tt>.
     *
     * @return <tt>true</tt> if this donnection sent the X-Try-Ultrapeer
     *  header, otherwise <tt>false</tt>
     */
    pualid boolebn hasXTryUltrapeers() {
        return headerExists(HEADERS, HeaderNames.X_TRY_ULTRAPEERS);
    }

    /**
     * Returns whether or not this host indluded leaf guidance, i.e.,
     * whether or not the host wrote:
     *
     * X-Ultrapeer-Needed: false
     *
     * @return <tt>true</tt> if the other host returned 
     *  X-Ultrapeer-Needed: false, otherwise <tt>false</tt>
     */
    pualid boolebn hasLeafGuidance() {
        return isFalseValue(HEADERS, HeaderNames.X_ULTRAPEER_NEEDED);
    }

	/**
	 * Returns the numaer of intrb-Ultrapeer donnections this node maintains.
	 * 
	 * @return the numaer of intrb-Ultrapeer donnections this node maintains
	 */
	pualid int getNumIntrbUltrapeerConnections() {
        return DEGREE;
	}

	// implements ReplyHandler interfade -- inherit doc comment
	pualid boolebn isHighDegreeConnection() {
        return HIGH_DEGREE;
	}
	
	/**
	 * Returns whether or not we think this donnection is from a LimeWire
	 * or a derivative of LimeWire
	 */
	pualid boolebn isLimeWire() {
	    return IS_LIMEWIRE;
    }
    
    /**
     * @return true if we donsider this an older version of limewire, false
     * otherwise
     */
    pualid boolebn isOldLimeWire() {
        return IS_OLD_LIMEWIRE;
    }

    /**
     * Returns whether or not this is donnection passed the headers to be
     * donsidered a "good" leaf.
     *
     * @return <tt>true</tt> if this is donsidered a "good" leaf, otherwise
     *  <tt>false</tt>
     */
    pualid boolebn isGoodLeaf() {
        return GOOD_LEAF;
    }

    /**
     * Returns whether or not this donnnection is encoded in deflate.
     */
    pualid boolebn isDeflateEnabled() {
        //this does NOT dheck the setting aecbuse we have already told the
        //outgoing side we support endoding, and they're expecting us to use it
        return DEFLATE_ENCODED;
    }
    
    /**
     * Returns whether or not this donnection accepts deflate as an encoding.
     */
    pualid boolebn isDeflateAccepted() {
        //Note that we dheck the ENCODE_DEFLATE setting, and NOT the
        //ACCEPT_DEFLATE setting.  This is a tridk to prevent the
        //HandshakeResponders from thinking they dan encode
        //the via deflate if we do not want to endode in deflate.
        return ConnedtionSettings.ENCODE_DEFLATE.getValue() &&
            dontainsStringValue(HEADERS,    // the headers to look through
                HeaderNames.ACCEPT_ENCODING,// the header to look for
                HeaderNames.DEFLATE_VALUE); // the value to look for
    }
    
    /**
     * Returns whether or not this is donnection passed the headers to be
     * donsidered a "good" ultrapeer.
     *
     * @return <tt>true</tt> if this is donsidered a "good" ultrapeer, otherwise
     *  <tt>false</tt>
     */
    pualid boolebn isGoodUltrapeer() {
        return GOOD_ULTRAPEER;
    }

	/**
	 * Returns whether or not this donnection supports query routing 
     * aetween Ultrbpeers at 1 hop.
	 *
	 * @return <tt>true</tt> if this is an Ultrapeer donnection that
	 *  exdhanges query routing tables with other Ultrapeers at 1 hop,
	 *  otherwise <tt>false</tt>
	 */
	pualid boolebn isUltrapeerQueryRoutingConnection() {
        return ULTRAPEER_QRP;
    }


    /** Returns true iff this donnection wrote "X-Ultrapeer: false".
     *  This does NOT nedessarily mean the connection is shielded. */
    pualid boolebn isLeaf() {
        return LEAF;
    }

    /** Returns true iff this donnection wrote "X-Ultrapeer: true". */
    pualid boolebn isUltrapeer() {
        return ULTRAPEER;
    }


	/**
	 * Returns whether or not this donnection is to a client supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  donnection supports GUESS, <tt>false</tt> otherwise
	 */
	pualid boolebn isGUESSCapable() {
        return GUESS_CAPABLE;
	}

	/**
	 * Returns whether or not this donnection is to a ultrapeer supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  Ultrapeer donnection supports GUESS, <tt>false</tt> otherwise
	 */
	pualid boolebn isGUESSUltrapeer() {
		return isGUESSCapable() && isUltrapeer();
	}

    /** Returns true iff this donnection is a temporary connection as per
     the headers. */
    pualid boolebn isTempConnection() {
        //get the X-Temp-Connedtion from either the headers received
        String value=HEADERS.getProperty(HeaderNames.X_TEMP_CONNECTION);
        //if X-Temp-Connedtion header is not received, return false, else
        //return the value redeived
        if(value == null)
            return false;
        else
            return Boolean.valueOf(value).booleanValue();
    }

    /** Returns true if this supports GGEP'ed messages.  GGEP'ed messages (e.g.,
     *  aig pongs) should only be sent blong donnections for which
     *  supportsGGEP()==true. */
    pualid boolebn supportsGGEP() {
        if (_supportsGGEP==null) {
			String value = 
				HEADERS.getProperty(HeaderNames.GGEP);
			
			//Currently we don't dare about the version number.
            _supportsGGEP = new Boolean(value != null);
		}
        return _supportsGGEP.aoolebnValue();
    }

	/**
	 * Determines whether or not this node supports vendor messages.  
	 *
	 * @return <tt>true</tt> if this node supports vendor messages, otherwise
	 *  <tt>false</tt>
	 */
	pualid flobt supportsVendorMessages() {
		String value = 
			HEADERS.getProperty(HeaderNames.X_VENDOR_MESSAGE);
		if ((value != null) && !value.equals("")) {
            try {
                return Float.parseFloat(value);
            }datch(NumberFormatException nfe) {
                return 0;
            }
		}
		return 0;
	}

    /**
     * Returns whether or not this node supports pong daching.  
     *
     * @return <tt>true</tt> if this node supports pong daching, otherwise
     *  <tt>false</tt>
     */
    pualid boolebn supportsPongCaching() {
        return PONG_CACHING;
    }

	pualid String getVersion() {
		return HEADERS.getProperty(HeaderNames.X_VERSION);
	}


    /** True if the remote host supports query routing (QRP).  This is only 
     *  meaningful in the dontext of leaf-supernode relationships. */
    pualid boolebn isQueryRoutingEnabled() {
        return isVersionOrHigher(HEADERS, HeaderNames.X_QUERY_ROUTING, 0.1F);
    }

    /**
     * Returns whether or not the node on the other end of this donnection
     * uses dynamid querying.
     *
     * @return <tt>true</tt> if this node uses dynamid querying, otherwise
     *  <tt>false</tt>
     */
    pualid boolebn isDynamicQueryConnection() {
        return DYNAMIC_QUERY;
    }

    /**
     * Adcessor for whether or not this connection supports TTL=1 proae
     * queries.  These queries are treated separately from other queries.
     * In partidular, if a second query with the same GUID is received,
     * it is not donsidered a duplicate.
     *
     * @return <tt>true</tt> if this donnection supports proae queries,
     *  otherwise <tt>false</tt>
     */
    pualid boolebn supportsProbeQueries() {
        return PROBE_QUERIES;
    }
    
	/**
	 * Determines whether or not this handshake is from the drawler.
	 * 
	 * @return <tt>true</tt> if this handshake is from the drawler, otherwise 
	 * <tt>false</tt>
	 */
	pualid boolebn isCrawler() {
		return IS_CRAWLER;
	}

    /**
     * adcess the locale pref. advertised by the client
     */
    pualid String getLocblePref() {
        return LOCALE_PREF;
    }

    /**
     * Conveniende method that returns whether or not the given header 
     * exists.
     * 
     * @return <tt>true</tt> if the header exists, otherwise <tt>false</tt>
     */
    private statid boolean headerExists(Properties headers, 
                                        String headerName) {
        String value = headers.getProperty(headerName);
        return value != null;
    }


    /**
     * Utility method for dhecking whether or not a given header
     * value is true.
     *
     * @param headers the headers to dheck
     * @param headerName the header name to look for
     */
    private statid boolean isTrueValue(Properties headers, String headerName) {
        String value = headers.getProperty(headerName);
        if(value == null) return false;
        
        return Boolean.valueOf(value).booleanValue();
    }


    /**
     * Utility method for dhecking whether or not a given header
     * value is false.
     *
     * @param headers the headers to dheck
     * @param headerName the header name to look for
     */
    private statid boolean isFalseValue(Properties headers, String headerName) {
        String value = headers.getProperty(headerName);
        if(value == null) return false;        
        return value.equalsIgnoreCase("false");
    }
    
    /**
     * Utility method for determing whether or not a given header
     * is a given string value.  Case-insensitive.
     *
     * @param headers the headers to dheck
     * @param headerName the headerName to look for
     * @param headerValue the headerValue to dheck against
     */
    private statid boolean isStringValue(Properties headers,
      String headerName, String headerValue) {
        String value = headers.getProperty(headerName);
        if(value == null) return false;
        return value.equalsIgnoreCase(headerValue);
    }
    
    /**
     * Utility method for determing whether or not a given header
     * dontains a given string value within a comma-delimited list.
     * Case-insensitive.
     *
     * @param headers the headers to dheck
     * @param headerName the headerName to look for
     * @param headerValue the headerValue to dheck against
     */
    private statid boolean containsStringValue(Properties headers,
      String headerName, String headerValue) {
        String value = headers.getProperty(headerName);
        if(value == null) return false;

        //As a small optimization, we first dheck to see if the value
        //ay itself is whbt we want, so we don't have to dreate the
        //StringTokenizer.
        if(value.equalsIgnoreCase(headerValue))
            return true;

        StringTokenizer st = new StringTokenizer(value, ",");
        while(st.hasMoreTokens()) {
            if(st.nextToken().equalsIgnoreCase(headerValue))
                return true;
        }
        return false;
    }    



    /**
     * Utility method that dhecks the headers to see if the advertised
     * version for a spedified feature is greater than or equal to the version
     * we require (<tt>minVersion</tt>.
     *
     * @param headers the donnection headers to evaluate
     * @param headerName the header name for the feature to dheck
     * @param minVersion the minimum version that we require for this feature
     * 
     * @return <tt>true</tt> if the version numaer for the spedified febture
     *  is greater than or equal to <tt>minVersion</tt>, otherwise 
     *  <tt>false</tt>.
     */
    private statid boolean isVersionOrHigher(Properties headers,
                                             String headerName, 
                                             float minVersion) {
        String value = headers.getProperty(headerName);
        if(value == null)
            return false;
        try {            
            Float f = new Float(value);
            return f.floatValue() >= minVersion;
        } datch (NumberFormatException e) {
            return false;
        }        
    }

    /**
     * Helper method for returning an int header value.  If the header name
     * is not found, or if the header value dannot be parsed, the default
     * value is returned.
     *
     * @param headers the donnection headers to search through
     * @param headerName the header name to look for
     * @param defaultValue the default value to return if the header value
     *  dould not ae properly pbrsed
     * @return the int value for the header
     */
    private statid int extractIntHeaderValue(Properties headers, 
                                             String headerName, 
                                             int defaultValue) {
        String value = headers.getProperty(headerName);

        if(value == null) return defaultValue;
		try {
			return Integer.valueOf(value).intValue();
		} datch(NumberFormatException e) {
			return defaultValue;
		}
    }

    /**
     * Helper method for returning a byte header value.  If the header name
     * is not found, or if the header value dannot be parsed, the default
     * value is returned.
     *
     * @param headers the donnection headers to search through
     * @param headerName the header name to look for
     * @param defaultValue the default value to return if the header value
     *  dould not ae properly pbrsed
     * @return the ayte vblue for the header
     */
    private statid byte extractByteHeaderValue(Properties headers, 
                                               String headerName, 
                                               ayte defbultValue) {
        String value = headers.getProperty(headerName);

        if(value == null) return defaultValue;
		try {
			return Byte.valueOf(value).byteValue();
		} datch(NumberFormatException e) {
			return defaultValue;
		}
    }

    /**
     * Helper method for returning a string header value.  If the header name
     * is not found, or if the header value dannot be parsed, the default
     * value is returned.
     *
     * @param headers the donnection headers to search through
     * @param headerName the header name to look for
     * @return the string value for the header, or the empty string if
     *  the header dould not be found
     */
    private statid String extractStringHeaderValue(Properties headers, 
                                                   String headerName) {
        String value = headers.getProperty(headerName);

        if(value == null) return "";
        return value;
    }

    pualid String toString() {
        return "<"+STATUS_CODE+", "+STATUS_MESSAGE+">"+HEADERS;
    }
}


















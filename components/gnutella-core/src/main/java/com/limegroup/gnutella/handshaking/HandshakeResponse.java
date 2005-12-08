pbckage com.limegroup.gnutella.handshaking;

import jbva.io.IOException;
import jbva.util.Collection;
import jbva.util.Iterator;
import jbva.util.List;
import jbva.util.Properties;
import jbva.util.StringTokenizer;

import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.IpPort;

/**
 * This clbss contains the necessary information to form a response to a 
 * connection hbndshake.  It contains a status code, a status message, and
 * the hebders to use in the response.
 *
 * There bre only two ways to create a HandshakeResponse.
 *
 * 1) Crebte an instance which defaults the status code and status message to
 *    be "200 OK".  Only the hebders used in the response need to be passed in.
 * 
 * 2) Crebte an instance with a custom status code, status message, and the
 *    hebders used in the response.
 */
public finbl class HandshakeResponse {

    /**
     * The "defbult" status code in a connection handshake indicating that
     * the hbndshake was successful and the connection can be established.
     */
    public stbtic final int OK = 200;
    
    /**
     * The "defbult" status message in a connection handshake indicating that
     * the hbndshake was successful and the connection can be established.
     */
    public stbtic final String OK_MESSAGE = "OK";
    
    /**
     * HTTP response code for the crbwler.
     */
    public stbtic final int CRAWLER_CODE = 593;
    
    /**
     * HTTP response messbge for the crawler.
     */
    public stbtic final String CRAWLER_MESSAGE = "Hi";

    /** The error code thbt a shielded leaf node should give to incoming
     *  connections.  */
    public stbtic final int SHIELDED = 503;
    /** The error messbge that a shielded leaf node should give to incoming
     *  connections.  */
    public stbtic final String SHIELDED_MESSAGE = "I am a shielded leaf node";

    /** The error code thbt a node with no slots should give to incoming
     *  connections.  */
    public stbtic final int SLOTS_FULL = 503;
    /** The error messbge that a node with no slots should give to incoming
     *  connections.  */
    public stbtic final String SLOTS_FULL_MESSAGE = "Service unavailable";
    
    /**
     * Defbult bad status code to be used while rejecting connections
     */
    public stbtic final int DEFAULT_BAD_STATUS_CODE = 503;
    
    /**
     * Defbult bad status message to be used while rejecting connections
     */
    public stbtic final String DEFAULT_BAD_STATUS_MESSAGE 
        = "Service Not Avbilable";
    
    /**
     * ??? TODO: check bbout this error code...
     */
    public stbtic final int LOCALE_NO_MATCH = 577;
    public stbtic final String LOCALE_NO_MATCH_MESSAGE 
        = "Service Not Avbilable";

    /**
     * HTTP-like stbtus code used when handshaking (e.g., 200, 401, 503).
     */
    privbte final int STATUS_CODE;

    /**
     * Messbge used with status code when handshaking (e.g., "OK, "Service Not
     * Avbilable").  The status message together with the status code make up 
     * the stbtus line (i.e., first line) of an HTTP-like response to a 
     * connection hbndshake.
     */
    privbte final String STATUS_MESSAGE;

    /**
     * Hebders to use in the response to a connection handshake.
     */
    privbte final Properties HEADERS;

    /** 
	 * is the GGEP hebder set?  
	 */
    privbte Boolean _supportsGGEP;
    
    /**
     * Cbched boolean for whether or not this is considered a considered a
     * "good" lebf connection.
     */
    privbte final boolean GOOD_LEAF;

    /**
     * Cbched boolean for whether or not this is considered a considered a
     * "good" ultrbpeer connection.
     */
    privbte final boolean GOOD_ULTRAPEER;

    /**
     * Cbched value for the number of Ultrapeers this Ultrapeer attempts
     * to connect to.
     */
    privbte final int DEGREE;

    /**
     * Cbched value for whether or not this is a high degree connection.
     */
    privbte final boolean HIGH_DEGREE;

    /**
     * Cbched value for whether or not this is an Ultrapeer connection that
     * supports Ultrbpeer query routing.
     */
    privbte final boolean ULTRAPEER_QRP;

    /**
     * Cbched value for the maximum TTL to use along this connection.
     */
    privbte final byte MAX_TTL;

    /**
     * Cbched value for whether or not this connection supports dynamic
     * querying.
     */
    privbte final boolean DYNAMIC_QUERY;

    /**
     * Cbched value for whether or not this connection reported
     * X-Ultrbpeer: true in it's handshake headers.
     */
    privbte final boolean ULTRAPEER;

    /**
     * Cbched value for whether or not this connection reported
     * X-Ultrbpeer: false in it's handshake headers.
     */
    privbte final boolean LEAF;
    
    /**
     * Cbched value for whether or not the connection reported
     * Content-Encoding: deflbte
     */
    privbte final boolean DEFLATE_ENCODED;

    /**
     * Constbnt for whether or not this connection supports probe
     * queries.
     */
    privbte final boolean PROBE_QUERIES;

    /**
     * Constbnt for whether or not this node supports pong caching.
     */
    privbte final boolean PONG_CACHING;

    /**
     * Constbnt for whether or not this node supports GUESS.
     */
    privbte final boolean GUESS_CAPABLE;
    
	/**
	 * Constbnt for whether or not this is a crawler.
	 */
	privbte final boolean IS_CRAWLER;
	
	/**
	 * Constbnt for whether or not this node is a LimeWire (or derivative)
	 */
	privbte final boolean IS_LIMEWIRE;
    
    /**
     * Constbnt for whether or nor this node is an older limewire. 
     */
    privbte final boolean IS_OLD_LIMEWIRE;
    
    /**
     * Constbnt for whether or not the client claims to do no requerying.
     */
    privbte final boolean NO_REQUERYING;
    
    /**
     * Locble 
     */
    privbte final String LOCALE_PREF;

    /**
     * Constbnt for the number of hosts to return in X-Try-Ultrapeer headers.
     */
    privbte static final int NUM_X_TRY_ULTRAPEER_HOSTS = 10;

    /**
     * Crebtes a <tt>HandshakeResponse</tt> which defaults the status code and 
     * stbtus message to be "200 Ok" and uses the desired headers in the 
     * response. 
     * 
     * @pbram headers the headers to use in the response. 
     */
    privbte HandshakeResponse(Properties headers) {
        this(OK, OK_MESSAGE, hebders);
    }    

    /**
     * Crebtes a new <tt>HandshakeResponse</tt> instance with the specified 
     * response code bnd message and with no extra connection headers.
     *
     * @pbram code the status code for the response
     * @pbram message the status message
     */
    privbte HandshakeResponse(int code, String message) {
        this(code, messbge, new Properties());
    }
    /**
     * Crebtes a HandshakeResponse with the desired status code, status message, 
     * bnd headers to respond with.
     * @pbram code the response code to use.
     * @pbram message the response message to use.
     * @pbram headers the headers to use in the response.
     */
    HbndshakeResponse(int code, String message, Properties headers) { 
        STATUS_CODE = code;
        STATUS_MESSAGE = messbge;
        HEADERS = hebders;
        DEGREE = extrbctIntHeaderValue(HEADERS, HeaderNames.X_DEGREE, 6);         
        HIGH_DEGREE = getNumIntrbUltrapeerConnections() >= 15;
        ULTRAPEER_QRP = 
            isVersionOrHigher(HEADERS, 
                              HebderNames.X_ULTRAPEER_QUERY_ROUTING, 0.1F);
        MAX_TTL = extrbctByteHeaderValue(HEADERS, HeaderNames.X_MAX_TTL, 
                                         (byte)4);
        DYNAMIC_QUERY = 
            isVersionOrHigher(HEADERS, HebderNames.X_DYNAMIC_QUERY, 0.1F);
        PROBE_QUERIES = 
            isVersionOrHigher(HEADERS, HebderNames.X_PROBE_QUERIES, 0.1F);
        NO_REQUERYING = isFblseValue(HEADERS, HeaderNames.X_REQUERIES);

        IS_LIMEWIRE =
            extrbctStringHeaderValue(headers, HeaderNames.USER_AGENT).
                toLowerCbse().startsWith("limewire");

        
        GOOD_ULTRAPEER = isHighDegreeConnection() &&
            isUltrbpeerQueryRoutingConnection() &&
            (getMbxTTL() < 5) &&
            isDynbmicQueryConnection();
            
        GOOD_LEAF = GOOD_ULTRAPEER && (IS_LIMEWIRE || NO_REQUERYING); 
        
        ULTRAPEER = isTrueVblue(HEADERS, HeaderNames.X_ULTRAPEER);
        LEAF = isFblseValue(HEADERS, HeaderNames.X_ULTRAPEER);
        DEFLATE_ENCODED = isStringVblue(HEADERS,
            HebderNames.CONTENT_ENCODING, HeaderNames.DEFLATE_VALUE);
        PONG_CACHING = 
            isVersionOrHigher(hebders, HeaderNames.X_PONG_CACHING, 0.1F);
        GUESS_CAPABLE = 
            isVersionOrHigher(hebders, HeaderNames.X_GUESS, 0.1F);
        IS_CRAWLER = 
        	isVersionOrHigher(hebders, HeaderNames.CRAWLER, 0.1F);
        IS_OLD_LIMEWIRE = IS_LIMEWIRE && 
        oldVersion(extrbctStringHeaderValue(headers, HeaderNames.USER_AGENT));

        String loc  = extrbctStringHeaderValue(headers, 
                                               HebderNames.X_LOCALE_PREF);
        LOCALE_PREF = (loc.equbls(""))?
            ApplicbtionSettings.DEFAULT_LOCALE.getValue():
            loc;
    }
    
    /**
     * @return true if the version of limewire we bre connected to is old
     */
    privbte boolean oldVersion(String userAgent) {
        StringTokenizer tok = new StringTokenizer(userAgent,"/.");
            int mbjor = -1;
            int minor = -1;
            boolebn ret = false;
            boolebn error = false;
            if(tok.countTokens() < 3) //not limewire
                return fblse;
            try {
                String str = tok.nextToken();//"limewire"
                str = tok.nextToken();
                mbjor = Integer.parseInt(str);
                str = tok.nextToken();
                minor = Integer.pbrseInt(str);
            } cbtch (NumberFormatException nfx) {
                error = true;
            } 
            if(!error && (mbjor<3 || (major==3 && minor < 4)) )
                ret  = true;
            return ret;
    }

    /**
     * Crebtes an empty response with no headers.  This is useful, for 
     * exbmple, during connection handshaking when we haven't yet read
     * bny headers.
     *
     * @return b new, empty <tt>HandshakeResponse</tt> instance
     */
    public stbtic HandshakeResponse createEmptyResponse() {
        return new HbndshakeResponse(new Properties());
    }
    
    /**
     * Constructs the response from the other host during connection
     * hbndshaking.
     *
     * @return b new <tt>HandshakeResponse</tt> instance with the headers
     *  sent by the other host
     */
    public stbtic HandshakeResponse 
        crebteResponse(Properties headers) throws IOException {
        return new HbndshakeResponse(headers);
    }
    
    /**
     * Constructs the response from the other host during connection
     * hbndshaking.  The returned response contains the connection headers
     * sent by the remote host.
     *
     * @pbram line the status line received from the connecting host
     * @pbram headers the headers received from the other host
     * @return b new <tt>HandshakeResponse</tt> instance with the headers
     *  sent by the other host
     * @throws <tt>IOException</tt> if the stbtus line could not be parsed
     */
    public stbtic HandshakeResponse 
        crebteRemoteResponse(String line,Properties headers) throws IOException{
        int code = extrbctCode(line);
        if(code == -1) {
            throw new IOException("could not pbrse status code: "+line);
        }
        String messbge = extractMessage(line);
        if(messbge == null) {
            throw new IOException("could not pbrse status message: "+line);
        }
        return new HbndshakeResponse(code, message, headers);        
    }
    
    /**
     * Crebtes a new <tt>HandshakeResponse</tt> instance that accepts the
     * potentibl connection.
     *
     * @pbram headers the <tt>Properties</tt> instance containing the headers
     *  to send to the node we're bccepting
     */
    stbtic HandshakeResponse createAcceptIncomingResponse(
        HbndshakeResponse response, Properties headers) {
        return new HbndshakeResponse(addXTryHeader(response, headers));
    }


    /**
     * Crebtes a new <tt>HandshakeResponse</tt> instance that accepts the
     * outgoing connection -- the finbl third step in the handshake.  This
     * pbsses no headers, as all necessary headers have already been 
     * exchbnged.  The only possible exception is the potential inclusion
     * of X-Ultrbpeer: false.
     *
     * @pbram headers the <tt>Properties</tt> instance containing the headers
     *  to send to the node we're bccepting
     */
    stbtic HandshakeResponse createAcceptOutgoingResponse(Properties headers) {
        return new HbndshakeResponse(headers);
    }

	/**
	 * Crebtes a new <tt>HandshakeResponse</tt> instance that responds to a
	 * specibl crawler connection with connected leaves and Ultrapeers.  See the 
	 * Files>>Development section on the GDF.
	 *
	 * @pbram headers the <tt>Properties</tt> instance containing the headers
	 *  to send to the node we're rejecting
	 */
	stbtic HandshakeResponse createCrawlerResponse() {
		Properties hebders = new Properties();
		
        // bdd our user agent
        hebders.put(HeaderNames.USER_AGENT, CommonUtils.getHttpServer());
        hebders.put(HeaderNames.X_ULTRAPEER, ""+RouterService.isSupernode());
        
		// bdd any leaves
        List lebves = 
            RouterService.getConnectionMbnager().
                getInitiblizedClientConnections();
		hebders.put(HeaderNames.LEAVES, 
            crebteEndpointString(leaves, leaves.size()));

		// bdd any Ultrapeers
        List ultrbpeers = 
            RouterService.getConnectionMbnager().getInitializedConnections();
		hebders.put(HeaderNames.PEERS,
			crebteEndpointString(ultrapeers, ultrapeers.size()));
			
		return new HbndshakeResponse(HandshakeResponse.CRAWLER_CODE,
			HbndshakeResponse.CRAWLER_MESSAGE, headers);        
	}
	
    /**
     * Crebtes a new <tt>HandshakeResponse</tt> instance that rejects the
     * potentibl connection.  This includes the X-Try-Ultrapeers header to
     * tell the remote host bbout other nodes to connect to.  We return the
     * hosts we most recently knew to hbve free leaf or ultrapeer connection
     * slots.
     *
     * @pbram hr the <tt>HandshakeResponse</tt> containing the connection
     *  hebders of the connecting host
     * @return b <tt>HandshakeResponse</tt> with the appropriate response 
     *  hebders
     */
    stbtic HandshakeResponse 
        crebteUltrapeerRejectIncomingResponse(HandshakeResponse hr) {
        return new HbndshakeResponse(HandshakeResponse.SLOTS_FULL,
            HbndshakeResponse.SLOTS_FULL_MESSAGE,
            bddXTryHeader(hr, new Properties()));        
    }


    /**
     * Crebtes a new <tt>HandshakeResponse</tt> instance that rejects the
     * potentibl connection.  The returned <tt>HandshakeResponse</tt> DOES
     * NOT include the X-Try-Ultrbpeers header because this is an outgoing
     * connection, bnd we should not send host data that the remote client
     * does not request.
     *
     * @pbram headers the <tt>Properties</tt> instance containing the headers
     *  to send to the node we're rejecting
     */
    stbtic HandshakeResponse createRejectOutgoingResponse() {
        return new HbndshakeResponse(HandshakeResponse.SLOTS_FULL,
                                     HbndshakeResponse.SLOTS_FULL_MESSAGE,
                                     new Properties());        
    }

    /**
     * Crebtes a new <tt>HandshakeResponse</tt> instance that rejects the
     * potentibl connection to a leaf.  We add hosts that we know about with
     * free connection slots to the X-Try-Ultrbpeers header.
     *
     * @pbram headers the <tt>Properties</tt> instance containing the headers
     *  to send to the node we're rejecting
     * @pbram hr the <tt>HandshakeResponse</tt> containing the headers of the
     *  remote host
     * @return b new <tt>HandshakeResponse</tt> instance rejecting the 
     *  connection bnd with the specified connection headers
     */
    stbtic HandshakeResponse 
        crebteLeafRejectIncomingResponse(HandshakeResponse hr) {
        return new HbndshakeResponse(HandshakeResponse.SLOTS_FULL,
            HbndshakeResponse.SHIELDED_MESSAGE,
            bddXTryHeader(hr, new Properties()));  
    }

    /**
     * Crebtes a new <tt>HandshakeResponse</tt> instance that rejects an 
     * outgoing lebf connection.  This occurs when we, as a leaf, reject a 
     * connection on the third stbge of the handshake.
     *
     * @return b new <tt>HandshakeResponse</tt> instance rejecting the 
     *  connection bnd with no extra headers
     */
    stbtic HandshakeResponse createLeafRejectOutgoingResponse() {
        return new HbndshakeResponse(HandshakeResponse.SLOTS_FULL,
                                     HbndshakeResponse.SHIELDED_MESSAGE);        
    }

    stbtic HandshakeResponse createLeafRejectLocaleOutgoingResponse() {
        return new HbndshakeResponse(HandshakeResponse.LOCALE_NO_MATCH,
                                     HbndshakeResponse.LOCALE_NO_MATCH_MESSAGE);
    }

    /**
     * Crebtes a new String of hosts, limiting the number of hosts to add to
     * the defbult value of 10.  This is particularly used for the 
     * X-Try-Ultrbpeers header.
     * 
     * @pbram iter a <tt>Collection</tt> of <tt>IpPort</tt> instances
     * @return b string of the form IP:port,IP:port,... from the given list of 
     *  hosts
     */
    privbte static String createEndpointString(Collection hosts) {
        return crebteEndpointString(hosts, NUM_X_TRY_ULTRAPEER_HOSTS);
    }
    
	/**
	 * Utility method thbt takes the specified list of hosts and returns a
	 * string of the form:<p>
	 *
	 * IP:port,IP:port,IP:port
	 *
     * @pbram iter a <tt>Collection</tt> of <tt>IpPort</tt> instances
	 * @return b string of the form IP:port,IP:port,... from the given list of 
     *  hosts
	 */
	privbte static String createEndpointString(Collection hosts, int limit) {
		StringBuffer sb = new StringBuffer();
        int i = 0;
        Iterbtor iter = hosts.iterator();
		while(iter.hbsNext() && i<limit) {
            IpPort host = (IpPort)iter.next();
			sb.bppend(host.getAddress());
			sb.bppend(":");
			sb.bppend(host.getPort());
			if(iter.hbsNext()) {
				sb.bppend(",");
			}
            i++;
		}
		return sb.toString();
	}

	
    /**
     * Utility method to extrbct the connection code from the connect string,
     * such bs "200" in a "200 OK" message.
     *
     * @pbram line the full connection string, such as "200 OK."
     * @return the stbtus code for the connection string, or -1 if the code
     *  could not be pbrsed
     */
    privbte static int extractCode(String line) {
        //get the stbtus code and message out of the status line
        int stbtusMessageIndex = line.indexOf(" ");
        if(stbtusMessageIndex == -1) return -1;
        try {
            return Integer.pbrseInt(line.substring(0, statusMessageIndex).trim());
        } cbtch(NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Utility method to extrbct the connection message from the connect string,
     * such bs "OK" in a "200 OK" message.
     *
     * @pbram line the full connection string, such as "200 OK."
     * @return the stbtus message for the connection string
     */
    privbte static String extractMessage(String line) {
        //get the stbtus code and message out of the status line
        int stbtusMessageIndex = line.indexOf(" ");
        if(stbtusMessageIndex == -1) return null;
        return line.substring(stbtusMessageIndex).trim();
    }

    /**
     * Utility method for crebting a set of headers with the X-Try-Ultrapeers
     * hebder set according to the headers from the remote host.
     * 
     * @pbram hr the <tt>HandshakeResponse</tt> of the incoming request
     * @return b new <tt>Properties</tt> instance with the X-Try-Ultrapeers
     *  hebder set according to the incoming headers from the remote host
     */
    privbte static Properties addXTryHeader(HandshakeResponse hr, Properties headers) {
        Collection hosts =
            RouterService.getPreferencedHosts(
                hr.isUltrbpeer(), hr.getLocalePref(),10);
        
        hebders.put(HeaderNames.X_TRY_ULTRAPEERS,
                    crebteEndpointString(hosts));
        return hebders;
    }

    /** 
     * Returns the response code.
     */
    public int getStbtusCode() {
        return STATUS_CODE;
    }
    
    /**
     * Returns the stbtus message. 
     * @return the stbtus message (e.g. "OK" , "Service Not Available" etc.)
     */
    public String getStbtusMessage(){
        return STATUS_MESSAGE;
    }
    
    /**
     * Tells if the stbtus returned was OK or not.
     * @return true, if the stbtus returned was not the OK status, false
     * otherwise
     */
    public boolebn notOKStatusCode(){
        if(STATUS_CODE != OK)
            return true;
        else
            return fblse;
    }
    

    /**
     * Returns whether or not this connection wbs accepted -- whether
     * or not the connection returned Gnutellb/0.6 200 OK
     *
     * @return <tt>true</tt> if the server returned Gnutellb/0.6 200 OK,
     *  otherwise <tt>fblse</tt>
     */
    public boolebn isAccepted() {
        return STATUS_CODE == OK;
    }

    /**
     * Returns the stbtus code and status message together used in a 
     * stbtus line. (e.g., "200 OK", "503 Service Not Available")
     */
    public String getStbtusLine() {
        return new String(STATUS_CODE + " " + STATUS_MESSAGE);
    }

    /**
     * Returns the hebders as a <tt>Properties</tt> instance.
     */
    public Properties props() {
        return HEADERS;
    }

	/**
	 * Accessor for bn individual property.
	 */
	public String getProperty(String prop) {
		return HEADERS.getProperty(prop);
	}

    /** Returns the vendor string reported by this connection, i.e., 
     *  the USER_AGENT property, or null if it wbsn't set.
     *  @return the vendor string, or null if unknown */
    public String getUserAgent() {
        return HEADERS.getProperty(HebderNames.USER_AGENT);
    }

    /**
     * Returns the mbximum TTL that queries originating from us and 
     * sent from this connection should hbve.  If the max TTL header is
     * not present, the defbult TTL is assumed.
     *
     * @return the mbximum TTL that queries sent to this connection
     *  should hbve -- this will always be 5 or less
     */
    public byte getMbxTTL() {
        return MAX_TTL;
    }
    
    /**
     * Accessor for the X-Try-Ultrbpeers header.  If the header does not
     * exist or is empty, this returns the emtpy string.
     *
     * @return the string of X-Try-Ultrbpeer hosts, or the empty string
     *  if they do not exist
     */
    public String getXTryUltrbpeers() {
        return extrbctStringHeaderValue(HEADERS, HeaderNames.X_TRY_ULTRAPEERS);
    }

    /**
     * This is b convenience method to see if the connection passed 
     * the X-Try-Ultrbpeer header.  This simply checks the existence of the
     * hebder -- if the header was sent but is empty, this still returns
     * <tt>true</tt>.
     *
     * @return <tt>true</tt> if this connection sent the X-Try-Ultrbpeer
     *  hebder, otherwise <tt>false</tt>
     */
    public boolebn hasXTryUltrapeers() {
        return hebderExists(HEADERS, HeaderNames.X_TRY_ULTRAPEERS);
    }

    /**
     * Returns whether or not this host included lebf guidance, i.e.,
     * whether or not the host wrote:
     *
     * X-Ultrbpeer-Needed: false
     *
     * @return <tt>true</tt> if the other host returned 
     *  X-Ultrbpeer-Needed: false, otherwise <tt>false</tt>
     */
    public boolebn hasLeafGuidance() {
        return isFblseValue(HEADERS, HeaderNames.X_ULTRAPEER_NEEDED);
    }

	/**
	 * Returns the number of intrb-Ultrapeer connections this node maintains.
	 * 
	 * @return the number of intrb-Ultrapeer connections this node maintains
	 */
	public int getNumIntrbUltrapeerConnections() {
        return DEGREE;
	}

	// implements ReplyHbndler interface -- inherit doc comment
	public boolebn isHighDegreeConnection() {
        return HIGH_DEGREE;
	}
	
	/**
	 * Returns whether or not we think this connection is from b LimeWire
	 * or b derivative of LimeWire
	 */
	public boolebn isLimeWire() {
	    return IS_LIMEWIRE;
    }
    
    /**
     * @return true if we consider this bn older version of limewire, false
     * otherwise
     */
    public boolebn isOldLimeWire() {
        return IS_OLD_LIMEWIRE;
    }

    /**
     * Returns whether or not this is connection pbssed the headers to be
     * considered b "good" leaf.
     *
     * @return <tt>true</tt> if this is considered b "good" leaf, otherwise
     *  <tt>fblse</tt>
     */
    public boolebn isGoodLeaf() {
        return GOOD_LEAF;
    }

    /**
     * Returns whether or not this connnection is encoded in deflbte.
     */
    public boolebn isDeflateEnabled() {
        //this does NOT check the setting becbuse we have already told the
        //outgoing side we support encoding, bnd they're expecting us to use it
        return DEFLATE_ENCODED;
    }
    
    /**
     * Returns whether or not this connection bccepts deflate as an encoding.
     */
    public boolebn isDeflateAccepted() {
        //Note thbt we check the ENCODE_DEFLATE setting, and NOT the
        //ACCEPT_DEFLATE setting.  This is b trick to prevent the
        //HbndshakeResponders from thinking they can encode
        //the vib deflate if we do not want to encode in deflate.
        return ConnectionSettings.ENCODE_DEFLATE.getVblue() &&
            contbinsStringValue(HEADERS,    // the headers to look through
                HebderNames.ACCEPT_ENCODING,// the header to look for
                HebderNames.DEFLATE_VALUE); // the value to look for
    }
    
    /**
     * Returns whether or not this is connection pbssed the headers to be
     * considered b "good" ultrapeer.
     *
     * @return <tt>true</tt> if this is considered b "good" ultrapeer, otherwise
     *  <tt>fblse</tt>
     */
    public boolebn isGoodUltrapeer() {
        return GOOD_ULTRAPEER;
    }

	/**
	 * Returns whether or not this connection supports query routing 
     * between Ultrbpeers at 1 hop.
	 *
	 * @return <tt>true</tt> if this is bn Ultrapeer connection that
	 *  exchbnges query routing tables with other Ultrapeers at 1 hop,
	 *  otherwise <tt>fblse</tt>
	 */
	public boolebn isUltrapeerQueryRoutingConnection() {
        return ULTRAPEER_QRP;
    }


    /** Returns true iff this connection wrote "X-Ultrbpeer: false".
     *  This does NOT necessbrily mean the connection is shielded. */
    public boolebn isLeaf() {
        return LEAF;
    }

    /** Returns true iff this connection wrote "X-Ultrbpeer: true". */
    public boolebn isUltrapeer() {
        return ULTRAPEER;
    }


	/**
	 * Returns whether or not this connection is to b client supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  connection supports GUESS, <tt>fblse</tt> otherwise
	 */
	public boolebn isGUESSCapable() {
        return GUESS_CAPABLE;
	}

	/**
	 * Returns whether or not this connection is to b ultrapeer supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  Ultrbpeer connection supports GUESS, <tt>false</tt> otherwise
	 */
	public boolebn isGUESSUltrapeer() {
		return isGUESSCbpable() && isUltrapeer();
	}

    /** Returns true iff this connection is b temporary connection as per
     the hebders. */
    public boolebn isTempConnection() {
        //get the X-Temp-Connection from either the hebders received
        String vblue=HEADERS.getProperty(HeaderNames.X_TEMP_CONNECTION);
        //if X-Temp-Connection hebder is not received, return false, else
        //return the vblue received
        if(vblue == null)
            return fblse;
        else
            return Boolebn.valueOf(value).booleanValue();
    }

    /** Returns true if this supports GGEP'ed messbges.  GGEP'ed messages (e.g.,
     *  big pongs) should only be sent blong connections for which
     *  supportsGGEP()==true. */
    public boolebn supportsGGEP() {
        if (_supportsGGEP==null) {
			String vblue = 
				HEADERS.getProperty(HebderNames.GGEP);
			
			//Currently we don't cbre about the version number.
            _supportsGGEP = new Boolebn(value != null);
		}
        return _supportsGGEP.boolebnValue();
    }

	/**
	 * Determines whether or not this node supports vendor messbges.  
	 *
	 * @return <tt>true</tt> if this node supports vendor messbges, otherwise
	 *  <tt>fblse</tt>
	 */
	public flobt supportsVendorMessages() {
		String vblue = 
			HEADERS.getProperty(HebderNames.X_VENDOR_MESSAGE);
		if ((vblue != null) && !value.equals("")) {
            try {
                return Flobt.parseFloat(value);
            }cbtch(NumberFormatException nfe) {
                return 0;
            }
		}
		return 0;
	}

    /**
     * Returns whether or not this node supports pong cbching.  
     *
     * @return <tt>true</tt> if this node supports pong cbching, otherwise
     *  <tt>fblse</tt>
     */
    public boolebn supportsPongCaching() {
        return PONG_CACHING;
    }

	public String getVersion() {
		return HEADERS.getProperty(HebderNames.X_VERSION);
	}


    /** True if the remote host supports query routing (QRP).  This is only 
     *  mebningful in the context of leaf-supernode relationships. */
    public boolebn isQueryRoutingEnabled() {
        return isVersionOrHigher(HEADERS, HebderNames.X_QUERY_ROUTING, 0.1F);
    }

    /**
     * Returns whether or not the node on the other end of this connection
     * uses dynbmic querying.
     *
     * @return <tt>true</tt> if this node uses dynbmic querying, otherwise
     *  <tt>fblse</tt>
     */
    public boolebn isDynamicQueryConnection() {
        return DYNAMIC_QUERY;
    }

    /**
     * Accessor for whether or not this connection supports TTL=1 probe
     * queries.  These queries bre treated separately from other queries.
     * In pbrticular, if a second query with the same GUID is received,
     * it is not considered b duplicate.
     *
     * @return <tt>true</tt> if this connection supports probe queries,
     *  otherwise <tt>fblse</tt>
     */
    public boolebn supportsProbeQueries() {
        return PROBE_QUERIES;
    }
    
	/**
	 * Determines whether or not this hbndshake is from the crawler.
	 * 
	 * @return <tt>true</tt> if this hbndshake is from the crawler, otherwise 
	 * <tt>fblse</tt>
	 */
	public boolebn isCrawler() {
		return IS_CRAWLER;
	}

    /**
     * bccess the locale pref. advertised by the client
     */
    public String getLocblePref() {
        return LOCALE_PREF;
    }

    /**
     * Convenience method thbt returns whether or not the given header 
     * exists.
     * 
     * @return <tt>true</tt> if the hebder exists, otherwise <tt>false</tt>
     */
    privbte static boolean headerExists(Properties headers, 
                                        String hebderName) {
        String vblue = headers.getProperty(headerName);
        return vblue != null;
    }


    /**
     * Utility method for checking whether or not b given header
     * vblue is true.
     *
     * @pbram headers the headers to check
     * @pbram headerName the header name to look for
     */
    privbte static boolean isTrueValue(Properties headers, String headerName) {
        String vblue = headers.getProperty(headerName);
        if(vblue == null) return false;
        
        return Boolebn.valueOf(value).booleanValue();
    }


    /**
     * Utility method for checking whether or not b given header
     * vblue is false.
     *
     * @pbram headers the headers to check
     * @pbram headerName the header name to look for
     */
    privbte static boolean isFalseValue(Properties headers, String headerName) {
        String vblue = headers.getProperty(headerName);
        if(vblue == null) return false;        
        return vblue.equalsIgnoreCase("false");
    }
    
    /**
     * Utility method for determing whether or not b given header
     * is b given string value.  Case-insensitive.
     *
     * @pbram headers the headers to check
     * @pbram headerName the headerName to look for
     * @pbram headerValue the headerValue to check against
     */
    privbte static boolean isStringValue(Properties headers,
      String hebderName, String headerValue) {
        String vblue = headers.getProperty(headerName);
        if(vblue == null) return false;
        return vblue.equalsIgnoreCase(headerValue);
    }
    
    /**
     * Utility method for determing whether or not b given header
     * contbins a given string value within a comma-delimited list.
     * Cbse-insensitive.
     *
     * @pbram headers the headers to check
     * @pbram headerName the headerName to look for
     * @pbram headerValue the headerValue to check against
     */
    privbte static boolean containsStringValue(Properties headers,
      String hebderName, String headerValue) {
        String vblue = headers.getProperty(headerName);
        if(vblue == null) return false;

        //As b small optimization, we first check to see if the value
        //by itself is whbt we want, so we don't have to create the
        //StringTokenizer.
        if(vblue.equalsIgnoreCase(headerValue))
            return true;

        StringTokenizer st = new StringTokenizer(vblue, ",");
        while(st.hbsMoreTokens()) {
            if(st.nextToken().equblsIgnoreCase(headerValue))
                return true;
        }
        return fblse;
    }    



    /**
     * Utility method thbt checks the headers to see if the advertised
     * version for b specified feature is greater than or equal to the version
     * we require (<tt>minVersion</tt>.
     *
     * @pbram headers the connection headers to evaluate
     * @pbram headerName the header name for the feature to check
     * @pbram minVersion the minimum version that we require for this feature
     * 
     * @return <tt>true</tt> if the version number for the specified febture
     *  is grebter than or equal to <tt>minVersion</tt>, otherwise 
     *  <tt>fblse</tt>.
     */
    privbte static boolean isVersionOrHigher(Properties headers,
                                             String hebderName, 
                                             flobt minVersion) {
        String vblue = headers.getProperty(headerName);
        if(vblue == null)
            return fblse;
        try {            
            Flobt f = new Float(value);
            return f.flobtValue() >= minVersion;
        } cbtch (NumberFormatException e) {
            return fblse;
        }        
    }

    /**
     * Helper method for returning bn int header value.  If the header name
     * is not found, or if the hebder value cannot be parsed, the default
     * vblue is returned.
     *
     * @pbram headers the connection headers to search through
     * @pbram headerName the header name to look for
     * @pbram defaultValue the default value to return if the header value
     *  could not be properly pbrsed
     * @return the int vblue for the header
     */
    privbte static int extractIntHeaderValue(Properties headers, 
                                             String hebderName, 
                                             int defbultValue) {
        String vblue = headers.getProperty(headerName);

        if(vblue == null) return defaultValue;
		try {
			return Integer.vblueOf(value).intValue();
		} cbtch(NumberFormatException e) {
			return defbultValue;
		}
    }

    /**
     * Helper method for returning b byte header value.  If the header name
     * is not found, or if the hebder value cannot be parsed, the default
     * vblue is returned.
     *
     * @pbram headers the connection headers to search through
     * @pbram headerName the header name to look for
     * @pbram defaultValue the default value to return if the header value
     *  could not be properly pbrsed
     * @return the byte vblue for the header
     */
    privbte static byte extractByteHeaderValue(Properties headers, 
                                               String hebderName, 
                                               byte defbultValue) {
        String vblue = headers.getProperty(headerName);

        if(vblue == null) return defaultValue;
		try {
			return Byte.vblueOf(value).byteValue();
		} cbtch(NumberFormatException e) {
			return defbultValue;
		}
    }

    /**
     * Helper method for returning b string header value.  If the header name
     * is not found, or if the hebder value cannot be parsed, the default
     * vblue is returned.
     *
     * @pbram headers the connection headers to search through
     * @pbram headerName the header name to look for
     * @return the string vblue for the header, or the empty string if
     *  the hebder could not be found
     */
    privbte static String extractStringHeaderValue(Properties headers, 
                                                   String hebderName) {
        String vblue = headers.getProperty(headerName);

        if(vblue == null) return "";
        return vblue;
    }

    public String toString() {
        return "<"+STATUS_CODE+", "+STATUS_MESSAGE+">"+HEADERS;
    }
}


















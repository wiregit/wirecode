package com.limegroup.gnutella.handshaking;

import com.limegroup.gnutella.*;
import java.util.Properties;
import com.sun.java.util.collections.*;

/**
 * This class contains the necessary information to form a response to a 
 * connection handshake.  It contains a status code, a status message, and
 * the headers to use in the response.
 *
 * There are only two ways to create a HandshakeResponse.
 *
 * 1) Create an instance which defaults the status code and status message to
 *    be "200 OK".  Only the headers used in the response need to be passed in.
 * 
 * 2) Create an instance with a custom status code, status message, and the
 *    headers used in the response.
 */
public final class HandshakeResponse {

    /**
     * The "default" status code in a connection handshake indicating that
     * the handshake was successful and the connection can be established.
     */
    public static final int OK = 200;
    
    /**
     * The "default" status message in a connection handshake indicating that
     * the handshake was successful and the connection can be established.
     */
    public static final String OK_MESSAGE = "OK";

    /** The error code that a shielded leaf node should give to incoming
     *  connections.  */
    public static final int SHIELDED = 503;
    /** The error message that a shielded leaf node should give to incoming
     *  connections.  */
    public static final String SHIELDED_MESSAGE = "I am a shielded leaf node";

    /** The error code that a node with no slots should give to incoming
     *  connections.  */
    public static final int SLOTS_FULL = 503;
    /** The error message that a node with no slots should give to incoming
     *  connections.  */
    public static final String SLOTS_FULL_MESSAGE = "Service unavailable";
    
    /**
     * Default bad status code to be used while rejecting connections
     */
    public static final int DEFAULT_BAD_STATUS_CODE = 503;
    
    /**
     * Default bad status message to be used while rejecting connections
     */
    public static final String DEFAULT_BAD_STATUS_MESSAGE 
        = "Service Not Available";
    
    /**
     * status code for unauthorized attempt
     */
    public static final int UNAUTHORIZED_CODE = 401;
    
    /**
     * status message for unauthorized attempt
     */
    public static final String UNAUTHORIZED_MESSAGE = "Unauthorized";
    
    /**
     * Message indicating that we are unable to authenticate
     */
    public static final String UNABLE_TO_AUTHENTICATE 
        = "Unable To Authenticate";
    
    /**
     * Message indicating that we are trying to authenticate
     */
    public static final String AUTHENTICATING = "AUTHENTICATING";

    /**
     * HTTP-like status code used when handshaking (e.g., 200, 401, 503).
     */
    private int statusCode;

    /**
     * Message used with status code when handshaking (e.g., "OK, "Service Not
     * Available").  The status message together with the status code make up 
     * the status line (i.e., first line) of an HTTP-like response to a 
     * connection handshake.
     */
    private String statusMessage;

    /**
     * Headers to use in the response to a connection handshake.
     */
    private final Properties HEADERS;

    /** 
	 * is the GGEP header set?  
	 */
    private Boolean _supportsGGEP;
    
    
    /**
     * Creates a HandshakeResponse which defaults the status code and status
     * message to be "200 Ok" and uses the desired headers in the response. 
     * @param headers the headers to use in the response. 
     */
    public HandshakeResponse(Properties headers) {
        statusCode = OK;
        statusMessage = OK_MESSAGE;
        HEADERS = headers;
    }    

    /**
     * Creates a HandshakeResponse with the desired status code, status message, 
     * and headers to respond with.
     * @param code the response code to use.
     * @param message the response message to use.
     * @param headers the headers to use in the response.
     */
     public HandshakeResponse(int code, String message, Properties headers) { 
         this.statusCode = code;
         this.statusMessage = message;
         this.HEADERS = headers;
     }
    
    /**
     * Creates a HandshakeResponse with the desired status line, 
     * and headers
     * @param statusLine the status code and status message together used in the
     * HTTP status line. (e.g., "200 OK", "503 Service Not Available")
     * @param headers the headers to use in the response.
     */
    private HandshakeResponse(String statusLine, Properties headers) { 
        try {
            //get the status code and message out of the status line
            int statusMessageIndex = statusLine.indexOf(" ");
            this.statusCode = Integer.parseInt(statusLine.substring(0, 
                statusMessageIndex).trim());
            this.statusMessage = statusLine.substring(
                statusMessageIndex).trim();
        }
        catch(Exception e){
            //in case of any exception, use default bad codes
            //TODO: this is bogus
            this.statusCode = DEFAULT_BAD_STATUS_CODE;
            this.statusMessage = DEFAULT_BAD_STATUS_MESSAGE;
        }
        
        this.HEADERS = headers;
    }

    /**
     * Constructs the response from the other host during connection
     * handshaking.
     *
     * @return a new <tt>HandshakeResponse</tt> instance with the headers
     *  sent by the other host
     */
    public static HandshakeResponse 
        createServerResponse(String message, Properties headers) {
        return new HandshakeResponse(message, headers);        
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instance that accepts the
     * potential connection.
     *
     * @param headers the <tt>Properties</tt> instance containing the headers
     *  to send to the node we're rejecting
     */
    static HandshakeResponse createAcceptResponse(Properties headers) {
        // add nodes from far away if we can in an attempt to avoid
        // cycles
        addHighHopsUltrapeers(RouterService.getHostCatcher(), headers);
        return new HandshakeResponse(headers);
    }

    /**
     * Creates a new <tt>HandshakeResponse</tt> instance that rejects the
     * potential connection.
     *
     * @param headers the <tt>Properties</tt> instance containing the headers
     *  to send to the node we're rejecting
     */
    static HandshakeResponse createRejectResponse(Properties headers) {
        addConnectedUltrapeers(RouterService.getConnectionManager(), headers);
        return new HandshakeResponse(HandshakeResponse.SLOTS_FULL,
                                     HandshakeResponse.SLOTS_FULL_MESSAGE,
                                     headers);        
    }

    /**
     * Adds the addresses of connected Ultrapeers for X-Try-Ultrapeer
     * headers.  This is useful particularly when we reject the connection,
     * as we do not care about cycles in that case.
     *
     * @param cm the <tt>ConnectionManager</tt> that provides data for
     *  the headers -- this may seem an odd design, since we always have
     *  access to the ConnectionManager, but this makes testing easier
     * @param headers the headers we're sending to the connecting node
     */
    private static void addConnectedUltrapeers(ConnectionManager cm,
                                               Properties headers) {
        StringBuffer hostString = new StringBuffer();

        //Also add neighbouring ultrapeers
        Set connectedSupernodeEndpoints = cm.getSupernodeEndpoints();

        //if nothing to add, return
        if(connectedSupernodeEndpoints.size() < 0)
            return;
        
        int i = 0;
        for(Iterator iter = connectedSupernodeEndpoints.iterator();
            iter.hasNext(); i++) {
            if(i == 10) break;
            //get the next endpoint
            Endpoint endpoint =(Endpoint)iter.next();

            //append the host information
            hostString.append(endpoint.getHostname());
            hostString.append(":");
            hostString.append(endpoint.getPort());
            if(iter.hasNext()) {
                hostString.append(Constants.ENTRY_SEPARATOR);
            }
        }

        //add the connected Ultrapeers to the handshake headers
        headers.put(HeaderNames.X_TRY_ULTRAPEERS, 
                    hostString.toString());        
    }

    /**
     * Adds the addresses of high-hops Ultrapeers to the given set
     * of headers, if those Ultrapeers are available.
     *
     * @param hc the <tt>HostCatcher</tt> for obtaining Ultrapeer
     *  hosts to return -- this is a slightly odd design, but 
     *  passing the host catcher as a parameter here makes testing
     *  a little easier
     * @param header the set of headers to add the Ultrapeers to
     */
    private static void addHighHopsUltrapeers(HostCatcher hc,
                                              Properties headers) {
        StringBuffer hostString = new StringBuffer();

        //add Ultrapeer endpoints from the hostcatcher.
        Iterator iter = hc.getUltrapeerHosts(10);
        while(iter.hasNext()) {
            Endpoint curHost = (Endpoint)iter.next();
            hostString.append(curHost.getHostname());
            hostString.append(":");
            hostString.append(curHost.getPort());
            if(iter.hasNext()) {
                hostString.append(Constants.ENTRY_SEPARATOR);
            }
        }

        headers.put(HeaderNames.X_TRY_ULTRAPEERS,
                    hostString.toString());
    }

    /** 
     * Returns the response code.
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Returns the status message. 
     * @return the status message (e.g. "OK" , "Service Not Available" etc.)
     */
    public String getStatusMessage(){
        return statusMessage;
    }
    
    /**
     * Tells if the status returned was OK or not.
     * @return true, if the status returned was not the OK status, false
     * otherwise
     */
    public boolean notOKStatusCode(){
        if(statusCode != OK)
            return true;
        else
            return false;
    }
    

    /**
     * Returns the status code and status message together used in a 
     * status line. (e.g., "200 OK", "503 Service Not Available")
     */
    public String getStatusLine() {
        return new String(statusCode + " " + statusMessage);
    }

    /**
     * Returns the headers as a <tt>Properties</tt> instance.
     */
    public Properties props() {
        return HEADERS;
    }

	/**
	 * Accessor for an individual property.
	 */
	public String getProperty(String prop) {
		return HEADERS.getProperty(prop);
	}

    /** Returns the vendor string reported by this connection, i.e., 
     *  the USER_AGENT property, or null if it wasn't set.
     *  @return the vendor string, or null if unknown */
    public String getUserAgent() {
        return HEADERS.getProperty(
            com.limegroup.gnutella.handshaking.
                HeaderNames.USER_AGENT);
    }

    /**
     * Returns the maximum TTL that queries originating from us and 
     * sent from this connection should have.  If the max TTL header is
     * not present, the default TTL is assumed.
     *
     * @return the maximum TTL that queries sent to this connection
     *  should have -- this will always be 5 or less
     */
    public int getMaxTTL() {
        return extractIntHeaderValue(HEADERS, HeaderNames.X_MAX_TTL, 5);        
    }
    
    /**
     * Returns whether or not this host needs more Ultrapeers, i.e. sent
     * X-Ultrapeer-Needed: true.
     *
     * @return <tt>true</tt> if the other host returned 
     *  X-Ultrapeer-Needed: true, otherwise <tt>false</tt>
     */
    public boolean ultrapeerNeeded() {
        return isTrueValue(HEADERS, HeaderNames.X_ULTRAPEER_NEEDED);
    }

	/**
	 * Returns the number of intra-Ultrapeer connections this node maintains.
	 * 
	 * @return the number of intra-Ultrapeer connections this node maintains
	 */
	public int getNumIntraUltrapeerConnections() {
        return extractIntHeaderValue(HEADERS, HeaderNames.X_DEGREE, 6);
	}

	// implements ReplyHandler interface -- inherit doc comment
	public boolean isHighDegreeConnection() {
		return getNumIntraUltrapeerConnections() >= 15;
	}


	/**
	 * Returns whether or not this connection supports query routing 
     * between Ultrapeers at 1 hop.
	 *
	 * @return <tt>true</tt> if this is an Ultrapeer connection that
	 *  exchanges query routing tables with other Ultrapeers at 1 hop,
	 *  otherwise <tt>false</tt>
	 */
	public boolean isUltrapeerQueryRoutingConnection() {
        return 
            isVersionOrHigher(HEADERS, 
                              HeaderNames.X_ULTRAPEER_QUERY_ROUTING, 0.1F);
    }


    /** Returns true iff this connection wrote "X-Ultrapeer: false".
     *  This does NOT necessarily mean the connection is shielded. */
    public boolean isLeaf() {
        return isFalseValue(HEADERS, HeaderNames.X_ULTRAPEER);
    }

    /** Returns true iff this connection wrote "X-Ultrapeer: true". */
    public boolean isUltrapeer() {
        return isTrueValue(HEADERS, HeaderNames.X_ULTRAPEER);
    }


	/**
	 * Returns whether or not this connection is to a client supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  connection supports GUESS, <tt>false</tt> otherwise
	 */
	public boolean isGUESSCapable() {
		int version = getGUESSVersion();
		if(version == -1) return false;
		else if(version < 20 && version > 0) return true;
		return false;
	}

	/**
	 * Returns whether or not this connection is to a ultrapeer supporting
	 * GUESS.
	 *
	 * @return <tt>true</tt> if the node on the other end of this 
	 *  Ultrapeer connection supports GUESS, <tt>false</tt> otherwise
	 */
	public boolean isGUESSUltrapeer() {
		return isGUESSCapable() && isUltrapeer();
	}


	/**
	 * Returns the version of the GUESS search scheme supported by the node
	 * at the other end of the connection.  This returns the version in
	 * whole numbers.  So, if the supported GUESS version is 0.1, this 
	 * will return 1.  If the other client has not sent an X-Guess header
	 * this returns -1.
	 *
	 * @return the version of GUESS supported, reported as a whole number,
	 *  or -1 if GUESS is not supported
	 */
	public int getGUESSVersion() {
		String value = HEADERS.getProperty(HeaderNames.X_GUESS);
		if(value == null) return -1;
		else {
			float version = Float.valueOf(value).floatValue();
			version *= 10;
			return (int)version;
		}
	}

    /** Returns true iff this connection is a temporary connection as per
     the headers. */
    public boolean isTempConnection() {
        //get the X-Temp-Connection from either the headers received
        String value=HEADERS.getProperty(HeaderNames.X_TEMP_CONNECTION);
        //if X-Temp-Connection header is not received, return false, else
        //return the value received
        if(value == null)
            return false;
        else
            return Boolean.valueOf(value).booleanValue();
    }

    /** Returns true if this supports GGEP'ed messages.  GGEP'ed messages (e.g.,
     *  big pongs) should only be sent along connections for which
     *  supportsGGEP()==true. */
    public boolean supportsGGEP() {
        if (_supportsGGEP==null) {
			String value = 
				HEADERS.getProperty(HeaderNames.GGEP);
			
			//Currently we don't care about the version number.
            _supportsGGEP = new Boolean(value != null);
		}
        return _supportsGGEP.booleanValue();
    }

	/**
	 * Determines whether or not this node supports vendor messages.  
	 *
	 * @return <tt>true</tt> if this node supports vendor messages, otherwise
	 *  <tt>false</tt>
	 */
	public boolean supportsVendorMessages() {
		String value = 
			HEADERS.getProperty(HeaderNames.X_VENDOR_MESSAGE);
		if ((value != null) && !value.equals("")) {		
			return true;
		}
		return false;
	}

	/**
	 * Returns the authenticated domains listed in the connection headers
	 * for this connection.
	 *
	 * @return the string of authenticated domains for this connection
	 */
	public String getDomainsAuthenticated() {
		return HEADERS.getProperty(
		    HeaderNames.X_DOMAINS_AUTHENTICATED);
		
	}

	public String getVersion() {
		return HEADERS.getProperty(HeaderNames.X_VERSION);
	}


    /** True if the remote host supports query routing (QRP).  This is only 
     *  meaningful in the context of leaf-supernode relationships. */
    public boolean isQueryRoutingEnabled() {
        return isVersionOrHigher(HEADERS, HeaderNames.X_QUERY_ROUTING, 0.1F);

        //We are ALWAYS QRP-enabled, so we only need to look at what the remote
        //host wrote.
        /*
        String value = 
			HEADERS.getProperty(HeaderNames.X_QUERY_ROUTING);
        if (value==null)
            return false;
        try {            
            Float f=new Float(value);
            return f.floatValue() >= 0.1f;   //TODO: factor into constant!
        } catch (NumberFormatException e) {
            return false;
        }
        */
    }

    /**
     * Returns whether or not the node on the other end of this connection
     * uses dynamic querying.
     *
     * @return <tt>true</tt> if this node uses dynamic querying, otherwise
     *  <tt>false</tt>
     */
    public boolean usesDynamicQuerying() {
        return isVersionOrHigher(HEADERS, HeaderNames.X_DYNAMIC_QUERY, 0.1F);
    }

    /**
     * Utility method for checking whether or not a given header
     * value is true.
     *
     * @param headers the headers to check
     * @param headerName the header name to look for
     */
    private static boolean isTrueValue(Properties headers, String headerName) {
        String value = headers.getProperty(headerName);
        if(value == null) return false;
        
        return Boolean.valueOf(value).booleanValue();
    }


    /**
     * Utility method for checking whether or not a given header
     * value is false.
     *
     * @param headers the headers to check
     * @param headerName the header name to look for
     */
    private static boolean isFalseValue(Properties headers, String headerName) {
        String value = headers.getProperty(headerName);
        if(value == null) return false;
        
        return !Boolean.valueOf(value).booleanValue();
    }



    /**
     * Utility method that checks the headers to see if the advertised
     * version for a specified feature is greater than or equal to the version
     * we require (<tt>minVersion</tt>.
     *
     * @param headers the connection headers to evaluate
     * @param headerName the header name for the feature to check
     * @param minVersion the minimum version that we require for this feature
     * 
     * @return <tt>true</tt> if the version number for the specified feature
     *  is greater than or equal to <tt>minVersion</tt>, otherwise 
     *  <tt>false</tt>.
     */
    private static boolean isVersionOrHigher(Properties headers,
                                             String headerName, 
                                             float minVersion) {
        String value = headers.getProperty(headerName);
        if(value == null)
            return false;
        try {            
            Float f = new Float(value);
            return f.floatValue() >= minVersion;
        } catch (NumberFormatException e) {
            return false;
        }        
    }

    /**
     * Helper method for returning an int header value.  If the header name
     * is not found, or if the header value cannot be parsed, the default
     * value is returned.
     *
     * @param headers the connection headers to search through
     * @param headerName the header name to look for
     * @param defaultValue the default value to return if the header value
     *  could not be properly parsed
     * @return the int value for the header
     */
    private static int extractIntHeaderValue(Properties headers, 
                                             String headerName, 
                                             int defaultValue) {
        String value = headers.getProperty(headerName);

        if(value == null) return defaultValue;
		try {
			return Integer.valueOf(value).intValue();
		} catch(NumberFormatException e) {
			return defaultValue;
		}
    }

    public String toString() {
        return "<"+statusCode+", "+statusMessage+">"+HEADERS;
    }
}


















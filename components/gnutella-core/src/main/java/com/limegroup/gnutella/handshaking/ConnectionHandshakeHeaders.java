/*
 * ConnectionHandshakeHeaders.java
 *
 * Created on September 27, 2001, 3:20 PM
 */

package com.limegroup.gnutella.handshaking;

/**
 * Provides names for the headers used in the gnutella connection handshake
 * @author Anurag Singla
 */
public class ConnectionHandshakeHeaders
{
    /** Obsolete; use X_LISTEN_IP instead */
    public static final String X_MY_ADDRESS         = "X-My-Address";
    /** The replacement for X_MY_ADDRESS */
    public static final String LISTEN_IP            = "Listen-IP";
    public static final String X_SUPERNODE          = "X-Ultrapeer";
    public static final String X_TRY                = "X-Try";
    public static final String X_TRY_SUPERNODES     = "X-Try-Ultrapeers";
    public static final String X_QUERY_ROUTING      = "X-Query-Routing";
    public static final String X_PONG_CACHING       = "X-Pong-Caching";
    public static final String X_SUPERNODE_NEEDED   = "X-Ultrapeer-Needed";
    public static final String USER_AGENT           = "User-Agent";
    public static final String X_USERNAME         = "X-Username";
    public static final String X_PASSWORD         = "X-Password";
    public static final String X_DOMAINS_AUTHENTICATED = 
        "X-Domains-Authenticated";
    public static final String X_TEMP_CONNECTION    = "X-Temp-Connection";
    public static final String REMOTE_IP            = "Remote-IP";

    public static final String GGEP                 = "GGEP";

	/**
	 * Header name for the GUESS version.
	 */
    public static final String X_GUESS              = "X-Guess";
    public static final String X_VERSION            = "X-Version";

	/**
	 * Header name for the degree of intra-Ultrapeer connections the
	 * host tries to maintain.
	 */
	public static final String X_DEGREE             = "X-Degree";

	/**
	 * Header for the version of query routing supported at the Ultrapeer level.
	 */
	public static final String X_ULTRAPEER_QUERY_ROUTING = 
		"X-Ultrapeer-Query-Routing";


    /** 
     * The true/false values for some headers.  Note that these are not the only
     * legal values--case doesn't matter--and LimeWire does not always use these
     * constants.  Hopefully it will in the future.  
     */
    public static final String TRUE                 = "true";
    public static final String FALSE                = "false";

    /**
     * Returns true if v.toLowerCase().equals("true").
     */
    public static boolean isTrue(String v) {
        return v!=null && v.toLowerCase().equals("true");
    }

    /**
     * Returns true if v.toLowerCase().equals("false").
     */
    public static boolean isFalse(String v) {
        return v!=null && v.toLowerCase().equals("false");
    }
}

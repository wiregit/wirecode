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
    public static final String X_MY_ADDRESS         = "X-My-Address";
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
    public static final String UPTIME               = "Uptime";

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

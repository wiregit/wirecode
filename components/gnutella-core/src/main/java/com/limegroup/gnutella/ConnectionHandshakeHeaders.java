/*
 * ConnectionHandshakeHeaders.java
 *
 * Created on September 27, 2001, 3:20 PM
 */

package com.limegroup.gnutella;

/**
 * Provides names for the headers used in the gnutella connection handshake
 * @author Anurag Singla
 */
public class ConnectionHandshakeHeaders
{
    public static final String MY_ADDRESS       = "My-Address";
    public static final String SUPERNODE        = "Supernode";
    public static final String X_TRY            = "X-Try";
    public static final String QUERY_ROUTING    = "Query-Routing";
    public static final String PONG_CACHING     = "Pong-Caching";
    public static final String SUPERNODE_NEEDED = "Supernode-Needed";
    public static final String X_USERNAME         = "X-Username";
    public static final String X_PASSWORD         = "X-Password";
    public static final String X_DOMAINS_AUTHENTICATED = 
        "X-Domains-Authenticated";
}

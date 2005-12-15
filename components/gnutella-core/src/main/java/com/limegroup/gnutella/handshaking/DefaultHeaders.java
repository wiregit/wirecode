
// Commented for the Learning branch

package com.limegroup.gnutella.handshaking;

import java.util.Properties;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.updates.UpdateManager;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * Comes with all the Gnutella headers we tell a remote computer during the handshake.
 * DefaultHeaders extends Properties, which is a hash table of strings.
 * The hash table has keys like "User-Agent" and values like "LimeWire/4.9.33" to hold the headers.
 * The constructor fills the hash table with the header names and values we send.
 */
public abstract class DefaultHeaders extends Properties {

    /** This is just the text "0.1" for the header "X-Query-Routing: 0.1" */
    private static final String QUERY_ROUTING_VERSION = "0.1";
    
    /**
     * Makes a new DefaultHeaders object, filled with header names and values.
     * 
     * @param remoteIP Pass in the remote computer's IP address like "69.192.143.203" to include a "Remote-IP" header
     */
    protected DefaultHeaders(String remoteIP) {

    	// Get our IP address and port number
    	// If we are firewalled, getAddress returns our LAN address like 192.168.0.102
    	// The remote Gnutella program will notice this address is in a local range, and know we are firewalled
        byte[] addr = RouterService.getAddress();
        int port = RouterService.getPort();

        // Make sure our IP address and port number are valid
        if(NetworkUtils.isValidAddress(addr) && NetworkUtils.isValidPort(port)) {
        	
        	// Compose those into a Gnutella header like "Listen-IP: 10.254.0.101:20158"
        	put(HeaderNames.LISTEN_IP, NetworkUtils.ip2string(addr) + ":" + port);
        }

        // We know the IP address of the remote computer
        if (remoteIP != null) {

        	// Compose a header like "Remote-IP: 67.41.102.170" to tell the remote computer what its IP address looks like from here
            put(HeaderNames.REMOTE_IP, remoteIP); // The put method adds this key and value to the hash table Properties inherits
        }
        
        // Call the method right beneath this one to add headers for the Gnutella features we support
        addCommonHeaders(this); // Pass it this so it will add the headers to the hash table here
    }
    
    /**
     * Adds the headers that LimeWire always sends to the given Properties hash table.
     * This adds headers like "Query-Routing: 0.1" and "User-Agent: LimeWire/4.9.33".
     * 
     * @param props The Properties hash table that addCommonHeaders adds all the common headers to, usually props is this
     */
    private static void addCommonHeaders(Properties props) {
 
    	// Compose and add the headers LimeWire always tells everyone
    	props.put(HeaderNames.X_QUERY_ROUTING, QUERY_ROUTING_VERSION);  // "X-Query-Routing: 0.1"
        props.put(HeaderNames.USER_AGENT, CommonUtils.getHttpServer()); // "User-Agent: LimeWire/4.9.33"
        props.put(HeaderNames.GGEP, "0.5");                             // "GGEP: 0.5"
		props.put(HeaderNames.X_GUESS, "0.1");                          // "X-Guess: 0.1"
        props.put(HeaderNames.X_VENDOR_MESSAGE, "0.2");                 // "Vendor-Message: 0.2"
        props.put(HeaderNames.X_REQUERIES, "false");                    // "X-Requeries: false"

        // We send these headers to Ultrapeers and leaves, even though these features are only used with Ultrapeers
        props.put(HeaderNames.X_DEGREE, Integer.toString(ConnectionSettings.NUM_CONNECTIONS.getValue())); // "X-Degree: 32" As an ultrapeer, we try to keep 32 connections to other ultrapeers
		props.put(HeaderNames.X_ULTRAPEER_QUERY_ROUTING, QUERY_ROUTING_VERSION);                          // "X-Ultrapeer-Query-Routing: 0.1"
        props.put(HeaderNames.X_MAX_TTL, "3");                                                            // "X-Max-TTL: 3"
        props.put(HeaderNames.X_DYNAMIC_QUERY, "0.1");                                                    // "X-Dynamic-Querying: 0.1"
        props.put(HeaderNames.X_LOCALE_PREF, ApplicationSettings.LANGUAGE.getValue());                    // "X-Locale-Pref: en"
        props.put(HeaderNames.X_PONG_CACHING, "0.1");                                                     // "Pong-Caching: 0.1" No leading X on this one

        // If settings allow us to accept compression, add a header like "Accept-Encoding: deflate"
        if (ConnectionSettings.ACCEPT_DEFLATE.getValue()) props.put(HeaderNames.ACCEPT_ENCODING, HeaderNames.DEFLATE_VALUE);

        // If we can share a valid LimeWire update software package, add a header like "X-Version: 4.9"
        UpdateManager updateManager = UpdateManager.instance(); // Get an instance of the UpdateManager
        String latestVersion = updateManager.getVersion();      // Ask it what the current version of LimeWire on the network is
        if (updateManager.isValid() && !latestVersion.equals("@version@")) props.put(HeaderNames.X_VERSION, latestVersion); // "X-Version: 4.9"
    }
}

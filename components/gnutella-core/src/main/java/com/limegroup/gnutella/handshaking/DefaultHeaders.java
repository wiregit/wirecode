package com.limegroup.gnutella.handshaking;

import java.util.Properties;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.updates.*;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * This class contains the headers that all LimeWires pass in connection
 * handshakes.
 */
public abstract class DefaultHeaders extends Properties {

    /**
     * Constant for the version of query routing we use.
     */
    private static final String QUERY_ROUTING_VERSION = "0.1";

    protected DefaultHeaders(String remoteIP) {
        byte[] addr = RouterService.getAddress();
        int port = RouterService.getPort();
        if(NetworkUtils.isValidAddress(addr) &&
           NetworkUtils.isValidPort(port)) {
            put(HeaderNames.LISTEN_IP,
                NetworkUtils.ip2string(addr) + ":" + port);
        }

        if (remoteIP!=null) {
            put(HeaderNames.REMOTE_IP, remoteIP);
        }

        addCommonHeaders(this);
    }
    
    /** 
     * Writes the common headers -- headers that all LimeWires should
     * send, like Query-Routing and User-Agent.
     */
    private static void addCommonHeaders(Properties props) {
        props.put(HeaderNames.X_QUERY_ROUTING, 
				  QUERY_ROUTING_VERSION);
        props.put(HeaderNames.USER_AGENT,
				  CommonUtils.getHttpServer());       
        props.put(HeaderNames.GGEP, "0.5");
		props.put(HeaderNames.X_GUESS, "0.1");
        props.put(HeaderNames.X_VENDOR_MESSAGE, "0.1");

        // even though these are only really used by Ultrapeers, we
        // include them with leaves to as an indication that they
        // understand these protocols
        props.put(HeaderNames.X_DEGREE, 
                  Integer.toString(ConnectionManager.ULTRAPEER_CONNECTIONS));
		props.put(HeaderNames.X_ULTRAPEER_QUERY_ROUTING, 
                  QUERY_ROUTING_VERSION);

        props.put(HeaderNames.X_MAX_TTL, "3");
        props.put(HeaderNames.X_DYNAMIC_QUERY, "0.1");
        
        if ( ConnectionSettings.ACCEPT_DEFLATE.getValue() )
            props.put(HeaderNames.ACCEPT_ENCODING, HeaderNames.DEFLATE_VALUE);
        
        props.put(HeaderNames.X_PONG_CACHING, "0.1");
        UpdateManager u = UpdateManager.instance();
        String latestVersion = u.getVersion();
        if(!latestVersion.equals("@version@"))//don't send header for @version@
            props.put(HeaderNames.X_VERSION, latestVersion);
    }
    
}


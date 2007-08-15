package com.limegroup.gnutella.handshaking;

import java.util.Properties;

import org.limewire.io.NetworkUtils;

import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.updates.UpdateManager;
import com.limegroup.gnutella.util.LimeWireUtils;

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
				  LimeWireUtils.getHttpServer());       
        props.put(HeaderNames.GGEP, "0.5");
		props.put(HeaderNames.X_GUESS, "0.1");
        props.put(HeaderNames.X_VENDOR_MESSAGE, "0.2");
        props.put(HeaderNames.X_REQUERIES, "false");

        // even though these are only really used by Ultrapeers, we
        // include them with leaves to as an indication that they
        // understand these protocols
        props.put(HeaderNames.X_DEGREE, 
                  Integer.toString(ConnectionSettings.NUM_CONNECTIONS.getValue()));
		props.put(HeaderNames.X_ULTRAPEER_QUERY_ROUTING, 
                  QUERY_ROUTING_VERSION);

        props.put(HeaderNames.X_MAX_TTL, "3");
        props.put(HeaderNames.X_DYNAMIC_QUERY, "0.1");
        props.put(HeaderNames.X_LOCALE_PREF, 
                  ApplicationSettings.LANGUAGE.getValue());
        
        if ( ConnectionSettings.ACCEPT_DEFLATE.getValue() )
            props.put(HeaderNames.ACCEPT_ENCODING, HeaderNames.DEFLATE_VALUE);
        
        props.put(HeaderNames.X_PONG_CACHING, "0.1");
        
        UpdateManager updateManager = UpdateManager.instance();
        String latestVersion = updateManager.getVersion();
        // only send if we had a valid file on disk & its not @version@.
        if(updateManager.isValid() && !latestVersion.equals("@version@"))
            props.put(HeaderNames.X_VERSION, latestVersion);
    }
    
}


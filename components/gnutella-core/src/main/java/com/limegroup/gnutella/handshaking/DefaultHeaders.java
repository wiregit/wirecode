package com.limegroup.gnutella.handshaking;

import java.util.Properties;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.updates.*;
import com.limegroup.gnutella.util.CommonUtils;

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
		put(HeaderNames.LISTEN_IP, "");
		//just temporary!

        if (remoteIP!=null) {
            put(HeaderNames.REMOTE_IP, remoteIP);
        }

        addCommonHeaders(this);
    }
    
    public String getProperty(String key, String defaultValue) {
        if (key.equals(HeaderNames.LISTEN_IP)) {
            Endpoint e=new Endpoint(RouterService.getAddress(), 
									RouterService.getPort());
            return e.getHostname()+":"+e.getPort();
        } else {
            return super.getProperty(key, defaultValue);
        }
    }
    
    public String getProperty(String key) {
        if (key.equals(HeaderNames.LISTEN_IP)) {
            Endpoint e=new Endpoint(RouterService.getAddress(), 
									RouterService.getPort());
            return e.getHostname()+":"+e.getPort();
        } else {
            return super.getProperty(key);
        }
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
        
        
        UpdateManager u = UpdateManager.instance();
        String latestVersion = u.getVersion();
        if(!latestVersion.equals("@version@"))//don't send header for @version@
            props.put(HeaderNames.X_VERSION, latestVersion);
    }
    
}


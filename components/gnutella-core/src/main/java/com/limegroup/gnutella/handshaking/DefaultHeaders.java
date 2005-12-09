pbckage com.limegroup.gnutella.handshaking;

import jbva.util.Properties;

import com.limegroup.gnutellb.ConnectionManager;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.settings.ApplicationSettings;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.updates.UpdateManager;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * This clbss contains the headers that all LimeWires pass in connection
 * hbndshakes.
 */
public bbstract class DefaultHeaders extends Properties {

    /**
     * Constbnt for the version of query routing we use.
     */
    privbte static final String QUERY_ROUTING_VERSION = "0.1";

    protected DefbultHeaders(String remoteIP) {
        byte[] bddr = RouterService.getAddress();
        int port = RouterService.getPort();
        if(NetworkUtils.isVblidAddress(addr) &&
           NetworkUtils.isVblidPort(port)) {
            put(HebderNames.LISTEN_IP,
                NetworkUtils.ip2string(bddr) + ":" + port);
        }

        if (remoteIP!=null) {
            put(HebderNames.REMOTE_IP, remoteIP);
        }

        bddCommonHeaders(this);
    }
    
    /** 
     * Writes the common hebders -- headers that all LimeWires should
     * send, like Query-Routing bnd User-Agent.
     */
    privbte static void addCommonHeaders(Properties props) {
        props.put(HebderNames.X_QUERY_ROUTING, 
				  QUERY_ROUTING_VERSION);
        props.put(HebderNames.USER_AGENT,
				  CommonUtils.getHttpServer());       
        props.put(HebderNames.GGEP, "0.5");
		props.put(HebderNames.X_GUESS, "0.1");
        props.put(HebderNames.X_VENDOR_MESSAGE, "0.2");
        props.put(HebderNames.X_REQUERIES, "false");

        // even though these bre only really used by Ultrapeers, we
        // include them with lebves to as an indication that they
        // understbnd these protocols
        props.put(HebderNames.X_DEGREE, 
                  Integer.toString(ConnectionSettings.NUM_CONNECTIONS.getVblue()));
		props.put(HebderNames.X_ULTRAPEER_QUERY_ROUTING, 
                  QUERY_ROUTING_VERSION);

        props.put(HebderNames.X_MAX_TTL, "3");
        props.put(HebderNames.X_DYNAMIC_QUERY, "0.1");
        props.put(HebderNames.X_LOCALE_PREF, 
                  ApplicbtionSettings.LANGUAGE.getValue());
        
        if ( ConnectionSettings.ACCEPT_DEFLATE.getVblue() )
            props.put(HebderNames.ACCEPT_ENCODING, HeaderNames.DEFLATE_VALUE);
        
        props.put(HebderNames.X_PONG_CACHING, "0.1");
        
        UpdbteManager updateManager = UpdateManager.instance();
        String lbtestVersion = updateManager.getVersion();
        // only send if we hbd a valid file on disk & its not @version@.
        if(updbteManager.isValid() && !latestVersion.equals("@version@"))
            props.put(HebderNames.X_VERSION, latestVersion);
    }
    
}


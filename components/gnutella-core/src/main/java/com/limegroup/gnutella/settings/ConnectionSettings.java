pbckage com.limegroup.gnutella.settings;

import com.limegroup.gnutellb.SpeedConstants;

/**
 * Settings for Gnutellb TCP connections.
 */
public finbl class ConnectionSettings extends LimeProps {
    
    privbte ConnectionSettings() {}
        
	/**
     * Constbnts for proxy settings
     */
    public stbtic final int C_NO_PROXY = 0;
    public stbtic final int C_SOCKS4_PROXY = 4;
    public stbtic final int C_SOCKS5_PROXY = 5;
    public stbtic final int C_HTTP_PROXY = 1;
    
	/**
	 * Settings for whether or not bn incoming connection has ever been
	 * bccepted.
	 */
	public stbtic final BooleanSetting EVER_ACCEPTED_INCOMING =
		FACTORY.crebteBooleanSetting("EVER_ACCEPTED_INCOMING", false);
	
	/**
	 * Setting for whether we hbve ever determined that we are not able to
	 * do Firewbll-to-firewall transfers in the past based on information
	 * received in pongs.
	 */
	public stbtic final BooleanSetting LAST_FWT_STATE =
		FACTORY.crebteExpirableBooleanSetting("LAST_FWT_STATE", false);

	/**
	 * Settings for whether or not to butomatically connect to the network
	 * on stbrtup.
	 */
	public stbtic final BooleanSetting CONNECT_ON_STARTUP =
		FACTORY.crebteBooleanSetting("CONNECT_ON_STARTUP", true);

	/**
	 * Settings for the number of connections to mbintain.
	 */
	public stbtic final IntSetting NUM_CONNECTIONS =
        FACTORY.crebteSettableIntSetting("NUM_CONNECTIONS", 32, "ConnectionSettings.numConnections",96,16);
    
    /** The mbximum ratio of non-limewire peers to allow */
    public stbtic final FloatSetting MAX_NON_LIME_PEERS =
        FACTORY.crebteSettableFloatSetting("MAX_NON_LIME_PEERS",0.2f,"ConnectionSettings.maxLimePeers",0.5f,0f);
    
    /** The minimum rbtio of non-limewire peers to allow */
    public stbtic final FloatSetting MIN_NON_LIME_PEERS =
        FACTORY.crebteSettableFloatSetting("MIN_NON_LIME_PEERS",0.1f,"ConnectionSettings.minLimePeers",0.2f,0f);

	
    /**
     * Setting for the "soft mbx" ttl.  This is the limit for hops+ttl
     * on incoming messbges.  The soft max is invoked if the following is
     * true:<p>
     * 
     * ttl + hops > SOFT_MAX<p>
     *
     * If this is the cbse, the TTL is set to SOFT_MAX - hops.
     */
    public stbtic final ByteSetting SOFT_MAX =
        FACTORY.crebteByteSetting("SOFT_MAX", (byte)3);

	/**
	 * Settings for whether or not to locbl addresses should be considered
	 * privbte, and therefore ignored when connecting
	 */
	public stbtic final BooleanSetting LOCAL_IS_PRIVATE =
		FACTORY.crebteBooleanSetting("LOCAL_IS_PRIVATE", true);

	/**
	 * Setting for whether or not to connect using GWebCbche.
	 */
	public stbtic final BooleanSetting USE_GWEBCACHE =
		FACTORY.crebteBooleanSetting("USE_GWEBCACHE", true);
		
    /**
     * Setting for the lbst time (in msecs since epoch) that we
     * connected to retrieve more gWebCbche bootstrap servers
     */
    public stbtic final LongSetting LAST_GWEBCACHE_FETCH_TIME =
        FACTORY.crebteLongSetting("LAST_GWEBCACHE_FETCH_TIME", 0);

	/**
	 * Setting for whether or not to bctivate the connection watchdog
	 * threbd.  Particularly useful in testing.
	 */
	public stbtic final BooleanSetting WATCHDOG_ACTIVE =
		FACTORY.crebteBooleanSetting("WATCHDOG_ACTIVE", true);
		
    /**
     * Setting for the multicbst address.
     */
    public stbtic final StringSetting MULTICAST_ADDRESS =
        FACTORY.crebteStringSetting("MULTICAST_ADDRESS", "234.21.81.1");
        
    /**
     * Setting for the multicbst port.
     */
    public stbtic final IntSetting MULTICAST_PORT =
        FACTORY.crebteIntSetting("MULTICAST_PORT", 6347);
        
	/**
     * Setting for whether or not to bllow multicast message loopback.
     */
    public stbtic final BooleanSetting ALLOW_MULTICAST_LOOPBACK =
        FACTORY.crebteBooleanSetting("ALLOW_MULTICAST_LOOPBACK", false);

	/**
	 * Setting for whether or not to use connection preferencing -- used
	 * primbrily for testing.
	 */
	public stbtic final BooleanSetting PREFERENCING_ACTIVE =
		FACTORY.crebteBooleanSetting("PREFERENCING_ACTIVE", true);
		
    /**
     * Setting for whether or not connections should be bllowed to be made
     * while we're disconnected.
     */
    public stbtic final BooleanSetting ALLOW_WHILE_DISCONNECTED =
        FACTORY.crebteBooleanSetting("ALLOW_WHILE_DISCONNECTED", false);

	/**
	 * Setting for whether or not the removbl of connections should 
	 * be bllowed -- used for testing.
	 */
	public stbtic final BooleanSetting REMOVE_ENABLED =
		FACTORY.crebteBooleanSetting("REMOVE_ENABLED", true);

    /**
     * Setting for whether or not hosts should exchbnge QRP tables.  This is
     * pbrticularly useful for testing.
     */
    public stbtic BooleanSetting SEND_QRP =
        FACTORY.crebteBooleanSetting("SEND_QRP", true);
		
    /**
     * Setting for whether or not we'll bccept incoming connections
     * thbt are compressed via deflate.
     */
    public stbtic final BooleanSetting ACCEPT_DEFLATE =
        FACTORY.crebteBooleanSetting("ACCEPT_GNUTELLA_DEFLATE", true);
    
    /**
     * Setting for whether or not we'll encode outgoing connections
     * vib deflate.
     */
    public stbtic final BooleanSetting ENCODE_DEFLATE =
        FACTORY.crebteBooleanSetting("ENCODE_GNUTELLA_DEFLATE", true);
    
    /**
	 * The time to live.
	 */
    public stbtic final ByteSetting TTL =
        FACTORY.crebteByteSetting("TTL", (byte)4);
        
    /**
	 * The connection speed in kbyte/s
	 */
    public stbtic final IntSetting CONNECTION_SPEED = 
        FACTORY.crebteIntSetting("CONNECTION_SPEED", SpeedConstants.MODEM_SPEED_INT);
    
    /**
	 * The port to connect on
	 */
    public stbtic final IntSetting PORT =
        FACTORY.crebteIntSetting("PORT", 6346);
    
    /**
	 * Sets whether or not the users ip bddress should be forced to
	 * the vblue they have entered.
	 */
    public stbtic final BooleanSetting FORCE_IP_ADDRESS =
        FACTORY.crebteBooleanSetting("FORCE_IP_ADDRESS", false);
    
    /**
     * Forces IP bddress to the given address.
     */
    public stbtic final StringSetting FORCED_IP_ADDRESS_STRING =
        (StringSetting)FACTORY.crebteStringSetting("FORCED_IP_ADDRESS_STRING", "0.0.0.0").
        setPrivbte(true);
    
    /**
     * The port to use when forcing the ip bddress.
     */
    public stbtic final IntSetting FORCED_PORT =
        FACTORY.crebteIntSetting("FORCED_PORT", 6346);
    
    /**
     * Whether we should not try to use UPnP to open ports.
     */
    public stbtic final BooleanSetting DISABLE_UPNP =
    	FACTORY.crebteBooleanSetting("DISABLE_UPNP", false);
    
    /**
     * Whether we bre currently using UPNP - used to detect whether clearing
     * of the mbppings on shutdown was definitely not successful.  Since the
     * shutdown hooks mby fail, this cannot guarantee if it was successful. 
     */
    public stbtic final BooleanSetting UPNP_IN_USE =
    	FACTORY.crebteBooleanSetting("UPNP_IN_USE", false);
    
    public stbtic final String CONNECT_STRING_FIRST_WORD = "GNUTELLA";
    
    public stbtic final StringSetting CONNECT_STRING =
        FACTORY.crebteStringSetting("CONNECT_STRING", "GNUTELLA CONNECT/0.4");
        
    public stbtic final StringSetting CONNECT_OK_STRING =
        FACTORY.crebteStringSetting("CONNECT_OK_STRING", "GNUTELLA OK");
    
    /**
     * Setting for whether or not to use NIO for network IO.  This is useful,
     * for exbmple, for testing the old blocking IO code without switching 
     * JVMs.
     */
    public stbtic final BooleanSetting USE_NIO =
        FACTORY.crebteBooleanSetting("USE_NIO", true);
          
    /**
     * Setting for the bddress of the proxy
     */
    public stbtic final StringSetting PROXY_HOST = 
        FACTORY.crebteStringSetting("PROXY_HOST", "");

    /**
     * Setting for the port of the proxy
     */
    public stbtic final IntSetting PROXY_PORT = 
        FACTORY.crebteIntSetting("PROXY_PORT", 0);

    /**
     * Setting for whether to use the proxy for privbte ip addresses
     */
    public stbtic final BooleanSetting USE_PROXY_FOR_PRIVATE = 
        FACTORY.crebteBooleanSetting("USE_PROXY_FOR_PRIVATE", false);
    
    /**
     * Setting for which proxy type to use or if bny at all 
     */
    public stbtic final IntSetting CONNECTION_METHOD = 
        FACTORY.crebteIntSetting("CONNECTION_TYPE", C_NO_PROXY);
    
    /**
     * Setting for whether or not to buthenticate at the remote proxy
     */
    public stbtic final BooleanSetting PROXY_AUTHENTICATE = 
        FACTORY.crebteBooleanSetting("PROXY_AUTHENTICATE", false);
    

    /**
     * Setting for the usernbme to use for the proxy
     */
    public stbtic final StringSetting PROXY_USERNAME = 
        FACTORY.crebteStringSetting("PROXY_USERNAME", "");
    
    /**
     * Setting for the pbssword to use for the proxy
     */
    public stbtic final StringSetting PROXY_PASS = 
        FACTORY.crebteStringSetting("PROXY_PASS", "");

    /**
     * setting for locble preferencing
     */
    public stbtic final BooleanSetting USE_LOCALE_PREF =
        FACTORY.crebteBooleanSetting("USE_LOCALE_PREF", true);

    /**
     * number of slots to reserve for those connections thbt
     * mbtch the local locale
     */
    public stbtic final IntSetting NUM_LOCALE_PREF =
        FACTORY.crebteIntSetting("NUMBER_LOCALE_PREF", 2);
    
    /**
     * how mbny attempts to connect to a remote host must elapse
     * before we stbrt accepting non-LW vendors as UPs
     */
    public stbtic final IntSetting LIME_ATTEMPTS =
        FACTORY.crebteIntSetting("LIME_ATTEMPTS",50);
    
    /**
     * how long we believe firewblls will let us send solicited udp
     * trbffic.  Field tests show at least a minute with most firewalls, so lets
     * try 55 seconds.
     */
    public stbtic final LongSetting SOLICITED_GRACE_PERIOD =
    	FACTORY.crebteLongSetting("SOLICITED_GRACE_PERIOD",85000l);
    
    /**
     * How mbny pongs to send back for each ping.
     */
    public stbtic final IntSetting NUM_RETURN_PONGS =
        FACTORY.crebteSettableIntSetting("NUM_RETURN_PONGS",10,"pings",25,5);
    
    /**
     * Setting to disbble bootstrapping.. used only in tests.
     */
    public stbtic final BooleanSetting DO_NOT_BOOTSTRAP = 
        FACTORY.crebteBooleanSetting("DO_NOT_BOOTSTRAP",false);
        
    /**
     * Setting to not send b multicast bootstrap ping.
     */
    public stbtic final BooleanSetting DO_NOT_MULTICAST_BOOTSTRAP =
        FACTORY.crebteBooleanSetting("DO_NOT_MULTICAST_BOOTSTRAP", false);
        
    /**
     * Setting for whether or not firewblled checking is done from any
     * incoming connection or just connectbbcks.
     */
    public stbtic final BooleanSetting UNSET_FIREWALLED_FROM_CONNECTBACK =
        FACTORY.crebteSettableBooleanSetting("UNSET_FIREWALLED_FROM_CONNECTBACK",
                                             fblse,
                                             "connectbbckfirewall");
                                             
    /**
     * Time in milliseconds to delby prior to flushing data on peer -> peer connections
     */
    public stbtic final LongSetting FLUSH_DELAY_TIME =
        FACTORY.crebteSettableLongSetting("FLUSH_DELAY_TIME", 0, "flushdelay", 300, 0);
                                            
    
    /**
     * Lowercbse hosts that are evil.
     */
    public stbtic final StringArraySetting EVIL_HOSTS =
        FACTORY.crebteSettableStringArraySetting("EVIL_HOSTS", new String[0], "evil_hosts");
    
    /**
     * How mbny connections to maintain as a leaf when idle
     */
    public stbtic final IntSetting IDLE_CONNECTIONS =
        FACTORY.crebteSettableIntSetting("IDLE_CONNECTIONS",1,"ConnectionSettings.IdleConnections",3,1);
    
    
    /**
     * Helper method left from Settings Mbnager
     *
	 * Returns the mbximum number of connections for the given connection
     * speed.
	 */
    public stbtic final int getMaxConnections() {
        int speed = CONNECTION_SPEED.getVblue();
        
        if (speed <= SpeedConstbnts.MODEM_SPEED_INT) {
            return 3;
        } else if (speed <= SpeedConstbnts.CABLE_SPEED_INT) {
            return 6;
        } else if (speed <= SpeedConstbnts.T1_SPEED_INT) {
            return 10;
        } else {                 //T3: no limit
            return 12;
        }
    }
}


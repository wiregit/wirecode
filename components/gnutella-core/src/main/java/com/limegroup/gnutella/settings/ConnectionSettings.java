package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.SpeedConstants;

/**
 * Settings for Gnutella TCP connections.
 */
public final class ConnectionSettings extends LimeProps {
    
    private ConnectionSettings() {}
        
	/**
     * Constants for proxy settings
     */
    public static final int C_NO_PROXY = 0;
    public static final int C_SOCKS4_PROXY = 4;
    public static final int C_SOCKS5_PROXY = 5;
    public static final int C_HTTP_PROXY = 1;
    
	/**
	 * Settings for whether or not an incoming connection has ever been
	 * accepted.
	 */
	public static final BooleanSetting EVER_ACCEPTED_INCOMING =
		FACTORY.createBooleanSetting("EVER_ACCEPTED_INCOMING", false);
	
	/**
	 * Setting for whether we have ever determined that we are not able to
	 * do Firewall-to-firewall transfers in the past based on information
	 * received in pongs.
	 */
	public static final BooleanSetting LAST_FWT_STATE =
		FACTORY.createExpirableBooleanSetting("LAST_FWT_STATE", false);

	/**
	 * Settings for whether or not to automatically connect to the network
	 * on startup.
	 */
	public static final BooleanSetting CONNECT_ON_STARTUP =
		FACTORY.createBooleanSetting("CONNECT_ON_STARTUP", true);

	/**
	 * Settings for the number of connections to maintain.
	 */
	public static final IntSetting NUM_CONNECTIONS =
        FACTORY.createSettableIntSetting("NUM_CONNECTIONS", 32, "ConnectionSettings.numConnections",16,96);
    
    /** The maximum ratio of non-limewire peers to allow */
    public static final FloatSetting MAX_NON_LIME_PEERS =
        FACTORY.createSettableFloatSetting("MAX_NON_LIME_PEERS",0.2f,"ConnectionSettings.maxLimePeers",0.0f, 0.5f);
    
    /** The minimum ratio of non-limewire peers to allow */
    public static final FloatSetting MIN_NON_LIME_PEERS =
        FACTORY.createSettableFloatSetting("MIN_NON_LIME_PEERS",0.1f,"ConnectionSettings.minLimePeers",0.0f, 0.2f);

	
    /**
     * Setting for the "soft max" ttl.  This is the limit for hops+ttl
     * on incoming messages.  The soft max is invoked if the following is
     * true:<p>
     * 
     * ttl + hops > SOFT_MAX<p>
     *
     * If this is the case, the TTL is set to SOFT_MAX - hops.
     */
    public static final ByteSetting SOFT_MAX =
        FACTORY.createByteSetting("SOFT_MAX", (byte)3);

	/**
	 * Settings for whether or not to local addresses should be considered
	 * private, and therefore ignored when connecting
	 */
	public static final BooleanSetting LOCAL_IS_PRIVATE =
		FACTORY.createBooleanSetting("LOCAL_IS_PRIVATE", true);

	/**
	 * Setting for whether or not to connect using GWebCache.
	 */
	public static final BooleanSetting USE_GWEBCACHE =
		FACTORY.createBooleanSetting("USE_GWEBCACHE", true);
		
    /**
     * Setting for the last time (in msecs since epoch) that we
     * connected to retrieve more gWebCache bootstrap servers
     */
    public static final LongSetting LAST_GWEBCACHE_FETCH_TIME =
        FACTORY.createLongSetting("LAST_GWEBCACHE_FETCH_TIME", 0);

	/**
	 * Setting for whether or not to activate the connection watchdog
	 * thread.  Particularly useful in testing.
	 */
	public static final BooleanSetting WATCHDOG_ACTIVE =
		FACTORY.createBooleanSetting("WATCHDOG_ACTIVE", true);
		
    /**
     * Setting for the multicast address.
     */
    public static final StringSetting MULTICAST_ADDRESS =
        FACTORY.createStringSetting("MULTICAST_ADDRESS", "234.21.81.1");
        
    /**
     * Setting for the multicast port.
     */
    public static final IntSetting MULTICAST_PORT =
        FACTORY.createIntSetting("MULTICAST_PORT", 6347);
        
	/**
     * Setting for whether or not to allow multicast message loopback.
     */
    public static final BooleanSetting ALLOW_MULTICAST_LOOPBACK =
        FACTORY.createBooleanSetting("ALLOW_MULTICAST_LOOPBACK", false);

	/**
	 * Setting for whether or not to use connection preferencing -- used
	 * primarily for testing.
	 */
	public static final BooleanSetting PREFERENCING_ACTIVE =
		FACTORY.createBooleanSetting("PREFERENCING_ACTIVE", true);
		
    /**
     * Setting for whether or not connections should be allowed to be made
     * while we're disconnected.
     */
    public static final BooleanSetting ALLOW_WHILE_DISCONNECTED =
        FACTORY.createBooleanSetting("ALLOW_WHILE_DISCONNECTED", false);

	/**
	 * Setting for whether or not the removal of connections should 
	 * be allowed -- used for testing.
	 */
	public static final BooleanSetting REMOVE_ENABLED =
		FACTORY.createBooleanSetting("REMOVE_ENABLED", true);

    /**
     * Setting for whether or not hosts should exchange QRP tables.  This is
     * particularly useful for testing.
     */
    public static BooleanSetting SEND_QRP =
        FACTORY.createBooleanSetting("SEND_QRP", true);
		
    /**
     * Setting for whether or not we'll accept incoming connections
     * that are compressed via deflate.
     */
    public static final BooleanSetting ACCEPT_DEFLATE =
        FACTORY.createBooleanSetting("ACCEPT_GNUTELLA_DEFLATE", true);
    
    /**
     * Setting for whether or not we'll encode outgoing connections
     * via deflate.
     */
    public static final BooleanSetting ENCODE_DEFLATE =
        FACTORY.createBooleanSetting("ENCODE_GNUTELLA_DEFLATE", true);
    
    /**
	 * The time to live.
	 */
    public static final ByteSetting TTL =
        FACTORY.createByteSetting("TTL", (byte)4);
        
    /**
	 * The connection speed in kbyte/s
	 */
    public static final IntSetting CONNECTION_SPEED = 
        FACTORY.createIntSetting("CONNECTION_SPEED", SpeedConstants.MODEM_SPEED_INT);
    
    /**
	 * The port to connect on
	 */
    public static final IntSetting PORT =
        FACTORY.createIntSetting("PORT", 6346);
    
    /**
	 * Sets whether or not the users ip address should be forced to
	 * the value they have entered.
	 */
    public static final BooleanSetting FORCE_IP_ADDRESS =
        FACTORY.createBooleanSetting("FORCE_IP_ADDRESS", false);
    
    /**
     * Forces IP address to the given address.
     */
    public static final StringSetting FORCED_IP_ADDRESS_STRING =
        (StringSetting)FACTORY.createStringSetting("FORCED_IP_ADDRESS_STRING", "0.0.0.0").
        setPrivate(true);
    
    /**
     * The port to use when forcing the ip address.
     */
    public static final IntSetting FORCED_PORT =
        FACTORY.createIntSetting("FORCED_PORT", 6346);
    
    /**
     * Whether we should not try to use UPnP to open ports.
     */
    public static final BooleanSetting DISABLE_UPNP =
    	FACTORY.createBooleanSetting("DISABLE_UPNP", false);
    
    /**
     * Whether we are currently using UPNP - used to detect whether clearing
     * of the mappings on shutdown was definitely not successful.  Since the
     * shutdown hooks may fail, this cannot guarantee if it was successful. 
     */
    public static final BooleanSetting UPNP_IN_USE =
    	FACTORY.createBooleanSetting("UPNP_IN_USE", false);
    
    public static final String CONNECT_STRING_FIRST_WORD = "GNUTELLA";
    
    public static final StringSetting CONNECT_STRING =
        FACTORY.createStringSetting("CONNECT_STRING", "GNUTELLA CONNECT/0.4");
        
    public static final StringSetting CONNECT_OK_STRING =
        FACTORY.createStringSetting("CONNECT_OK_STRING", "GNUTELLA OK");
    
    /**
     * Setting for whether or not to use NIO for network IO.  This is useful,
     * for example, for testing the old blocking IO code without switching 
     * JVMs.
     */
    public static final BooleanSetting USE_NIO =
        FACTORY.createBooleanSetting("USE_NIO", true);
          
    /**
     * Setting for the address of the proxy
     */
    public static final StringSetting PROXY_HOST = 
        FACTORY.createStringSetting("PROXY_HOST", "");

    /**
     * Setting for the port of the proxy
     */
    public static final IntSetting PROXY_PORT = 
        FACTORY.createIntSetting("PROXY_PORT", 0);

    /**
     * Setting for whether to use the proxy for private ip addresses
     */
    public static final BooleanSetting USE_PROXY_FOR_PRIVATE = 
        FACTORY.createBooleanSetting("USE_PROXY_FOR_PRIVATE", false);
    
    /**
     * Setting for which proxy type to use or if any at all 
     */
    public static final IntSetting CONNECTION_METHOD = 
        FACTORY.createIntSetting("CONNECTION_TYPE", C_NO_PROXY);
    
    /**
     * Setting for whether or not to authenticate at the remote proxy
     */
    public static final BooleanSetting PROXY_AUTHENTICATE = 
        FACTORY.createBooleanSetting("PROXY_AUTHENTICATE", false);
    

    /**
     * Setting for the username to use for the proxy
     */
    public static final StringSetting PROXY_USERNAME = 
        FACTORY.createStringSetting("PROXY_USERNAME", "");
    
    /**
     * Setting for the password to use for the proxy
     */
    public static final StringSetting PROXY_PASS = 
        FACTORY.createStringSetting("PROXY_PASS", "");

    /**
     * setting for locale preferencing
     */
    public static final BooleanSetting USE_LOCALE_PREF =
        FACTORY.createBooleanSetting("USE_LOCALE_PREF", true);

    /**
     * number of slots to reserve for those connections that
     * match the local locale
     */
    public static final IntSetting NUM_LOCALE_PREF =
        FACTORY.createIntSetting("NUMBER_LOCALE_PREF", 2);
    
    /**
     * how many attempts to connect to a remote host must elapse
     * before we start accepting non-LW vendors as UPs
     */
    public static final IntSetting LIME_ATTEMPTS =
        FACTORY.createIntSetting("LIME_ATTEMPTS",50);
    
    /**
     * how long we believe firewalls will let us send solicited udp
     * traffic.  Field tests show at least a minute with most firewalls, so lets
     * try 55 seconds.
     */
    public static final LongSetting SOLICITED_GRACE_PERIOD =
    	FACTORY.createLongSetting("SOLICITED_GRACE_PERIOD",85000l);
    
    /**
     * How many pongs to send back for each ping.
     */
    public static final IntSetting NUM_RETURN_PONGS =
        FACTORY.createSettableIntSetting("NUM_RETURN_PONGS",10,"pings",5,25);
    
    /**
     * Setting to disable bootstrapping.. used only in tests.
     */
    public static final BooleanSetting DO_NOT_BOOTSTRAP = 
        FACTORY.createBooleanSetting("DO_NOT_BOOTSTRAP",false);
        
    /**
     * Setting to not send a multicast bootstrap ping.
     */
    public static final BooleanSetting DO_NOT_MULTICAST_BOOTSTRAP =
        FACTORY.createBooleanSetting("DO_NOT_MULTICAST_BOOTSTRAP", false);
        
    /**
     * Setting for whether or not firewalled checking is done from any
     * incoming connection or just connectbacks.
     */
    public static final BooleanSetting UNSET_FIREWALLED_FROM_CONNECTBACK =
        FACTORY.createSettableBooleanSetting("UNSET_FIREWALLED_FROM_CONNECTBACK",
                                             false,
                                             "connectbackfirewall");
                                             
    /**
     * Time in milliseconds to delay prior to flushing data on peer -> peer connections
     */
    public static final LongSetting FLUSH_DELAY_TIME =
        FACTORY.createSettableLongSetting("FLUSH_DELAY_TIME", 0, "flushdelay", 0, 300);
                                            
    
    /**
     * Lowercase hosts that are evil.
     */
    public static final StringArraySetting EVIL_HOSTS =
        FACTORY.createSettableStringArraySetting("EVIL_HOSTS", new String[0], "evil_hosts");
    
    /**
     * How many connections to maintain as a leaf when idle
     */
    public static final IntSetting IDLE_CONNECTIONS =
        FACTORY.createSettableIntSetting("IDLE_CONNECTIONS",1,"ConnectionSettings.IdleConnections",1,3);
    
    /**
     * The maximum line length we'll try to parse while reading a header.
     */
    public static final IntSetting MAX_HANDSHAKE_LINE_SIZE =
        FACTORY.createSettableIntSetting("MAX_HANDSHAKE_LINE_SIZE", 1024, "handshake.maxLineSize", 512, 4096);
    
    /**
     * The maximum number of headers to try and parse.
     */
    public static final IntSetting MAX_HANDSHAKE_HEADERS =
        FACTORY.createSettableIntSetting("MAX_HANDSHAKZE_LIMIT", 30, "handshake.maxHeaderLimit", 15, 100);
    
    /**
     * The size of our Querry Routing Tables, in multiples of 1024 entries. 
     * (In 1998, the IEC standardized "kibi" as the prefix denoting 1024, or "kilo binary".) 
     */
    public static final PowerOfTwoSetting QRT_SIZE_IN_KIBI_ENTRIES =
        FACTORY.createSettablePowerOfTwoSetting("QRT_SIZE_IN_KIBI_ENTRIES", 64,
                "ConnectionSettings.QRTSizeInKibiEntries", 64, 256);
    
    /**
     * Helper method left from Settings Manager
     *
	 * Returns the maximum number of connections for the given connection
     * speed.
	 */
    public static final int getMaxConnections() {
        int speed = CONNECTION_SPEED.getValue();
        
        if (speed <= SpeedConstants.MODEM_SPEED_INT) {
            return 3;
        } else if (speed <= SpeedConstants.CABLE_SPEED_INT) {
            return 6;
        } else if (speed <= SpeedConstants.T1_SPEED_INT) {
            return 10;
        } else {                 //T3: no limit
            return 12;
        }
    }
}


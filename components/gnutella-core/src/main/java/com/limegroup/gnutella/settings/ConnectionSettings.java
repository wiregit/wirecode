package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * Settings for Gnutella TCP connections.
 */
public final class ConnectionSettings extends LimeProps {
    
    private ConnectionSettings() {}
        
	/**
	 * Settings for whether or not an incoming connection has ever been
	 * accepted.
	 */
	public static final BooleanSetting EVER_ACCEPTED_INCOMING =
		FACTORY.createExpirableBooleanSetting("EVER_ACCEPTED_INCOMING", false);

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
        FACTORY.createIntSetting("NUM_CONNECTIONS", 32);

	
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
	 * Setting for whether or not the removal of connections should 
	 * be allowed -- used for testing.
	 */
	public static final BooleanSetting REMOVE_ENABLED =
		FACTORY.createBooleanSetting("REMOVE_ENABLED", true);


	/**
	 * Setting for whether or not the keep alive setting should be ignored
	 * -- used for testing.
	 */
	public static final BooleanSetting IGNORE_KEEP_ALIVE =
		FACTORY.createBooleanSetting("IGNORE_KEEP_ALIVE", false);

	/**
	 * Setting for whether or not to show the keep alive in the UI.
	 */
	public static final BooleanSetting SHOW_KEEP_ALIVE =
		FACTORY.createBooleanSetting("SHOW_KEEP_ALIVE", false);
		
    /**
     * Setting for whether or not we'll accept incoming connections
     * that are compressed via deflate.
     */
    public static final BooleanSetting ACCEPT_DEFLATE =
        FACTORY.createBooleanSetting("ACCEPT_DEFLATE", true);
    
    /**
     * Setting for whether or not we'll encode outgoing connections
     * via deflate.
     */
    public static final BooleanSetting ENCODE_DEFLATE =
        FACTORY.createBooleanSetting("ENCODE_DEFLATE", true);
    
    /**
	 * The time to live.
	 */
    public static final ByteSetting TTL =
        FACTORY.createByteSetting("TTL", (byte)4);
        
    /**
	 * The connection speed in kbyte/s
	 */
    public static final IntSetting CONNECTION_SPEED = 
        FACTORY.createIntSetting("CONNECTION_SPEED", 56);
    
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
        FACTORY.createStringSetting("FORCED_IP_ADDRESS_STRING", "0.0.0.0");
    
    /**
     * The port to use when forcing the ip address.
     */
    public static final IntSetting FORCED_PORT =
        FACTORY.createIntSetting("FORCED_PORT", 6346);
    
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
        FACTORY.createBooleanSetting("USE_NIO", 
            CommonUtils.isJava14OrLater() && false);
          
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


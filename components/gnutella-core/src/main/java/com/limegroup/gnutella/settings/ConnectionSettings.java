package com.limegroup.gnutella.settings;

/**
 * Settings for Gnutella TCP connections.
 */
public final class ConnectionSettings extends AbstractSettings {

	/**
	 * Settings for whether or not an incoming connection has ever been
	 * accepted.
	 */
	public static final BooleanSetting EVER_ACCEPTED_INCOMING =
		FACTORY.createBooleanSetting("EVER_ACCEPTED_INCOMING", false);


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
        FACTORY.createIntSetting("NUM_CONNECTIONS", 15);

	
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
        CFG_FACTORY.createByteSetting("SOFT_MAX", (byte)3);

	/**
	 * Settings for whether or not to local addresses should be considered
	 * private, and therefore ignored when connecting
	 */
	public static final BooleanSetting LOCAL_IS_PRIVATE =
		CFG_FACTORY.createBooleanSetting("LOCAL_IS_PRIVATE", true);

	/**
	 * Setting for whether or not to connect using GWebCache.
	 */
	public static final BooleanSetting USE_GWEBCACHE =
		CFG_FACTORY.createBooleanSetting("USE_GWEBCACHE", true);

	/**
	 * Setting for whether or not to activate the connection watchdog
	 * thread.  Particularly useful in testing.
	 */
	public static final BooleanSetting WATCHDOG_ACTIVE =
		CFG_FACTORY.createBooleanSetting("WATCHDOG_ACTIVE", true);
		
    /**
     * Setting for the multicast address.
     */
    public static final StringSetting MULTICAST_ADDRESS =
        CFG_FACTORY.createStringSetting("MULTICAST_ADDRESS", "234.21.81.1");
        
    /**
     * Setting for the multicast port.
     */
    public static final IntSetting MULTICAST_PORT =
        CFG_FACTORY.createIntSetting("MULTICAST_PORT", 6347);

	/**
	 * Setting for whether or not to use connection preferencing -- used
	 * primarily for testing.
	 */
	public static final BooleanSetting PREFERENCING_ACTIVE =
		CFG_FACTORY.createBooleanSetting("PREFERENCING_ACTIVE", true);

	/**
	 * Setting for whether or not the removal of connections should 
	 * be allowed -- used for testing.
	 */
	public static final BooleanSetting REMOVE_ENABLED =
		CFG_FACTORY.createBooleanSetting("REMOVE_ENABLED", true);


	/**
	 * Setting for whether or not the keep alive setting should be ignored
	 * -- used for testing.
	 */
	public static final BooleanSetting IGNORE_KEEP_ALIVE =
		CFG_FACTORY.createBooleanSetting("IGNORE_KEEP_ALIVE", false);

	/**
	 * Setting for whether or not to show the keep alive in the UI.
	 */
	public static final BooleanSetting SHOW_KEEP_ALIVE =
		CFG_FACTORY.createBooleanSetting("SHOW_KEEP_ALIVE", false);
}


package com.limegroup.gnutella.settings;

/**
 * Settings for connections.
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
	public static final IntSetting KEEP_ALIVE =
		FACTORY.createIntSetting("KEEP_ALIVE", 4);

	
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

}


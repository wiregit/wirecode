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
		FACTORY.createBooleanSetting("LOCAL_IS_PRIVATE", true);

}


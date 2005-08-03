package com.limegroup.gnutella.settings;

/**
 * Settings for Ultrapeers.
 */
public final class UltrapeerSettings extends LimeProps {
    
    private UltrapeerSettings() {}

	/**
	 * Setting for whether or not we've ever been Ultrapeer capable.
	 */
	public static final BooleanSetting EVER_ULTRAPEER_CAPABLE =
		FACTORY.createExpirableBooleanSetting("EVER_SUPERNODE_CAPABLE", false);


	/**
	 * Setting for whether or not to force Ultrapeer mode.
	 */
	public static final BooleanSetting FORCE_ULTRAPEER_MODE =
		FACTORY.createBooleanSetting("FORCE_SUPERNODE_MODE", false);

	/**
	 * Setting for whether or not to disable Ultrapeer mode.
	 */
	public static final BooleanSetting DISABLE_ULTRAPEER_MODE =
		FACTORY.createBooleanSetting("DISABLE_SUPERNODE_MODE", false);

	
	/**
	 * Setting for the maximum leaf connections.
	 */
	public static final IntSetting MAX_LEAVES =
		FACTORY.createSettableIntSetting("MAX_LEAVES", 30,"UltrapeerSettings.maxLeaves",96,16);
}


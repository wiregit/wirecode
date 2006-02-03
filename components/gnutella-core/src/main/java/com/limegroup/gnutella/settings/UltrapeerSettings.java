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
    
    /**
     * The minimum number of upstream kbytes per second that 
     * a node must be able to transfer in order to qualify as a ultrapeer.
     */
    public static final IntSetting MIN_UPSTREAM_REQUIRED =
        FACTORY.createSettableIntSetting("MIN_UPSTREAM_REQUIRED",10,"UltrapeerSettings.MinUpstream",32,8);
    
    /**
     * The minimum number of downlstream kbytes per second that 
     * a node must be able to transfer in order to qualify as a ultrapeer.
     */
    public static final IntSetting MIN_DOWNSTREAM_REQUIRED =
        FACTORY.createSettableIntSetting("MIN_DOWNSTREAM_REQUIRED",20,"UltrapeerSettings.MinDownstream",64,16);
    
    /**
     * The minimum average uptime in seconds that a node must have to qualify for ultrapeer status.
     */
    public static final IntSetting MIN_AVG_UPTIME =
        FACTORY.createSettableIntSetting("MIN_AVG_UPTIME",3600,"UltrapeerSettings.MinAvgUptime",48*3600,3600);
    
    /**
     * Setting for whether or not the MIN_CONNECT_TIME is required.
     */
    public static final BooleanSetting NEED_MIN_CONNECT_TIME = 
        FACTORY.createBooleanSetting("NEED_MIN_CONNECT_TIME", true);
    
    /**
     * The minimum time in seconds that a node must have tried to connect before it can 
     * qualify for Ultrapeer status.
     */
    public static final IntSetting MIN_CONNECT_TIME =
        FACTORY.createSettableIntSetting("MIN_CONNECT_TIME",10,"UltrapeerSettings.MinConnectTime",30,0);
    
    /**
     * The minimum current uptime in seconds that a node must have to qualify for Ultrapeer status.
     */
    public static final IntSetting MIN_INITIAL_UPTIME =
        FACTORY.createSettableIntSetting("MIN_INITIAL_UPTIME",120*60,"UltrapeerSettings.MinInitialUptime",48*3600,120*60);
    
    /**
     * The amount of time to wait between attempts to become an Ultrapeer.
     */
    public static final IntSetting UP_RETRY_TIME =
        FACTORY.createSettableIntSetting("UP_RETRY_TIME",180*60*1000,
                "UltrapeerSettings.UpRetryTime",24*3600*1000,180*60*1000);
}


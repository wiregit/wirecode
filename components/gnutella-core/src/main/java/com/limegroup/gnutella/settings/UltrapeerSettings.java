
// Commented for the Learning branch

package com.limegroup.gnutella.settings;

/**
 * Speeds and times we use to determine if we would make a good ultrapeer on the Gnutella network.
 * The SupernodeAssigner class uses these values.
 * 
 * Set UltrapeerSettings.FORCE_ULTRAPEER_MODE to true to join the Gnutella network as an ultrapeer immediately. (do)
 * Set UltrapeerSettings.DISABLE_ULTRAPEER_MODE to true to never act as an ultrapeer, even if we could.
 * 
 * UltrapeerSettings.MAX_LEAVES is 30, the number of leaves we'll try to get under us when we're an ultrapeer.
 * 
 * To be an ultrapeer, we need a fast Internet connection.
 * We have to download at MIN_DOWNSTREAM_REQUIRED 20 KB/s or upload at MIN_UPSTREAM_REQUIRED 10 KB/s.
 * 
 * To be an ultrapeer, we also have to have a good record of running the program for a long time.
 * We should have a MIN_AVG_UPTIME of 1 hour, or be online now for MIN_CONNECT_TIME 2 hours or more.
 */
public final class UltrapeerSettings extends LimeProps {

    /** Don't make an UltrapeerSettings object. */
    private UltrapeerSettings() {}

	/** True once we've found our computer and Internet connection to be fast enough for us to act as an ultrapeer. */
	public static final BooleanSetting EVER_ULTRAPEER_CAPABLE = FACTORY.createExpirableBooleanSetting("EVER_SUPERNODE_CAPABLE", false);

	/** False, don't force ultrapeer mode. */
	public static final BooleanSetting FORCE_ULTRAPEER_MODE = FACTORY.createBooleanSetting("FORCE_SUPERNODE_MODE", false);

	/** False, don't disable ultrapeer mode. */
	public static final BooleanSetting DISABLE_ULTRAPEER_MODE = FACTORY.createBooleanSetting("DISABLE_SUPERNODE_MODE", false);

	/** 30 leaves, as an ultrapeer, we'll try to get 30 leaves. */
	public static final IntSetting MAX_LEAVES = FACTORY.createSettableIntSetting("MAX_LEAVES", 30, "UltrapeerSettings.maxLeaves", 96, 16);

    /** 10 KB/s, we have to be able to upload data at 10 KB/s to qualify to be an ultrapeer. */
    public static final IntSetting MIN_UPSTREAM_REQUIRED = FACTORY.createSettableIntSetting("MIN_UPSTREAM_REQUIRED", 10, "UltrapeerSettings.MinUpstream", 32, 8);

    /** 20 KB/s, we have to be able to download data at 20 KB/s to qualify to be an ultrapeer. */
    public static final IntSetting MIN_DOWNSTREAM_REQUIRED = FACTORY.createSettableIntSetting("MIN_DOWNSTREAM_REQUIRED", 20, "UltrapeerSettings.MinDownstream", 64, 16);

    /** 3600 seconds, if we have an average uptime of 1 hour, we'll qualify to be an ultrapeer. */
    public static final IntSetting MIN_AVG_UPTIME = FACTORY.createSettableIntSetting("MIN_AVG_UPTIME", 3600, "UltrapeerSettings.MinAvgUptime", 48*3600, 3600);

    /** 10 seconds, for the first 10 seconds of our time connecting to Gnutella, we'll tell remote comptuers we're a leaf. */
    public static final IntSetting MIN_CONNECT_TIME = FACTORY.createSettableIntSetting("MIN_CONNECT_TIME", 10, "UltrapeerSettings.MinConnectTime", 30, 0);

    /** 7200 seconds, if we've been running for 2 hours, we'll qualify to be an ultrapeer. */
    public static final IntSetting MIN_INITIAL_UPTIME =
        FACTORY.createSettableIntSetting("MIN_INITIAL_UPTIME", 120 * 60, "UltrapeerSettings.MinInitialUptime", 48 * 3600, 120 * 60);

    /** 10800000 milliseconds, we'll wait 3 hours before we try to become an ultrapeer again. */
    public static final IntSetting UP_RETRY_TIME = FACTORY.createSettableIntSetting("UP_RETRY_TIME", 180 * 60 * 1000, "UltrapeerSettings.UpRetryTime", 24 * 3600 * 1000, 180 * 60 * 1000);
}

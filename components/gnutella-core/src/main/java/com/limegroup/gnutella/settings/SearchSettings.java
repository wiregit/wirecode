package com.limegroup.gnutella.settings;

/**
 * Settings for searches.
 */
public final class SearchSettings {
    
    private static final SettingsFactory FACTORY =
        LimeProps.instance().getFactory();

	/**
	 * Setting for whether or not GUESS searching is enabled.
	 */
	public static final BooleanSetting GUESS_ENABLED =
		FACTORY.createBooleanSetting("GUESS_ENABLED", true);

    /**
     * The TTL for probe queries.
     */
    public static final ByteSetting PROBE_TTL =
        FACTORY.createByteSetting("PROBE_TTL", (byte)2);
}

package com.limegroup.gnutella.settings;

/**
 * Settings for searches.
 */
public final class SearchSettings extends AbstractSettings {

	/**
	 * Setting for whether or not GUESS searching is enabled.
	 */
	public static final BooleanSetting GUESS_ENABLED =
		FACTORY.createBooleanSetting("GUESS_ENABLED", true);
}

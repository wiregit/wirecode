package com.limegroup.gnutella.settings;

/**
 * Settings to deal with UI.
 */ 
public final class UISettings extends LimeProps {

    private UISettings() {}

    /**
     * Setting for autocompletion
     */
    public static final BooleanSetting AUTOCOMPLETE_ENABLED =
		FACTORY.createBooleanSetting("AUTOCOMPLETE_ENABLED", true);
		
    /**
     * Setting for whether or not row striping is enabled.
     */
    public static final BooleanSetting ROW_STRIPE_ENABLED =
        FACTORY.createBooleanSetting("ROW_STRIPE_ENABLED", true);

}

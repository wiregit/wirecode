package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.CommonUtils;

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
     * Setting for search-result filters.
     */
    public static final BooleanSetting SEARCH_RESULT_FILTERS =
        FACTORY.createBooleanSetting("SEARCH_RESULT_FILTERS", true);
        
    /**
     * Setting for the magnetmix button.
     */
    public static final BooleanSetting MAGNETMIX_BUTTON = 
        FACTORY.createBooleanSetting("SEARCH_MAGNETMIX_BUTTON",
                                     !CommonUtils.isPro());     
}

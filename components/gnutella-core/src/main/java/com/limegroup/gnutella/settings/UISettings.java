package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.CommonUtils;

import java.awt.Dimension;
import java.awt.Toolkit;

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
                                     !CommonUtils.isPro() && !isResolutionLow());
                                     
    /**
     * Setting for using small icons.
     */
    public static final BooleanSetting SMALL_ICONS =
        FACTORY.createBooleanSetting("UI_SMALL_ICONS", resolutionIsLow());
        
    /**
     * Setting for displaying text under icons.
     */
    public static final BooleanSetting TEXT_WITH_ICONS =
        FACTORY.createBooleanSetting("UI_TEXT_WITH_ICONS", true);
        
    /**
     * Setting for not grouping search results in GUI
     */
    public static final BooleanSetting UI_GROUP_RESULTS =
        FACTORY.createBooleanSetting("UI_GROUP_RESULTS", true);
        
    /**
     * Setting to allow ignoring of alt-locs in replies.
     */
    public static final BooleanSetting UI_ADD_REPLY_ALT_LOCS =
        FACTORY.createBooleanSetting("UI_ADD_REPLY_ALT_LOCS", true);
        
    /**
     * For people with bad eyes.
     */
    private static boolean isResolutionLow() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return screenSize.width <= 800 || screenSize.height <= 600;
    }
}

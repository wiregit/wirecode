package com.limegroup.gnutella.settings;

import java.awt.Dimension;
import java.awt.Toolkit;

import com.limegroup.gnutella.util.LimeWireUtils;

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
        FACTORY.createBooleanSetting("SEARCH_MAGNETMIX_BUTTON", !LimeWireUtils.isPro() && !isResolutionLow());
                                     
    /**
     * Setting for using small icons.
     */
    public static final BooleanSetting SMALL_ICONS =
        FACTORY.createBooleanSetting("UI_SMALL_ICONS", isResolutionLow());
        
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

    /**
     * Setting to persist monitor check box state.
     */
    public static final BooleanSetting UI_MONITOR_SHOW_INCOMING_SEARCHES =
        FACTORY.createBooleanSetting("UI_MONITOR_SHOW_INCOMING_SEARCHES", false);
	
	/**
	 * Setting for the divider location between library tree and table.
	 */
	public static final IntSetting UI_LIBRARY_TREE_DIVIDER_LOCATION =
		FACTORY.createIntSetting("UI_LIBRARY_TREE_DIVIDER_LOCATION", -1);
	
	/**
	 * Setting for the divider location between library and playlist.
	 */
	public static final IntSetting UI_LIBRARY_PLAY_LIST_TAB_DIVIDER_LOCATION =
		FACTORY.createIntSetting("UI_LIBRARY_PLAY_LIST_TAB_DIVIDER_LOCATION",
				300);
	
	/**
	 * Setting for the divider location between incoming query monitors and
	 * upload panel.
	 */
	public static final IntSetting UI_MONITOR_UPLOAD_TAB_DIVIDER_LOCATION =
		FACTORY.createIntSetting("UI_MONITOR_UPLOAD_TAB_DIVIDER_LOCATION", 300);
}

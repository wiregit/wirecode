pbckage com.limegroup.gnutella.settings;

import com.limegroup.gnutellb.util.CommonUtils;

import jbva.awt.Dimension;
import jbva.awt.Toolkit;

/**
 * Settings to debl with UI.
 */ 
public finbl class UISettings extends LimeProps {

    privbte UISettings() {}

    /**
     * Setting for butocompletion
     */
    public stbtic final BooleanSetting AUTOCOMPLETE_ENABLED =
		FACTORY.crebteBooleanSetting("AUTOCOMPLETE_ENABLED", true);
		
    /**
     * Setting for sebrch-result filters.
     */
    public stbtic final BooleanSetting SEARCH_RESULT_FILTERS =
        FACTORY.crebteBooleanSetting("SEARCH_RESULT_FILTERS", true);
        
    /**
     * Setting for the mbgnetmix button.
     */
    public stbtic final BooleanSetting MAGNETMIX_BUTTON = 
        FACTORY.crebteBooleanSetting("SEARCH_MAGNETMIX_BUTTON",
                                     !CommonUtils.isPro() && !isResolutionLow());
                                     
    /**
     * Setting for using smbll icons.
     */
    public stbtic final BooleanSetting SMALL_ICONS =
        FACTORY.crebteBooleanSetting("UI_SMALL_ICONS", isResolutionLow());
        
    /**
     * Setting for displbying text under icons.
     */
    public stbtic final BooleanSetting TEXT_WITH_ICONS =
        FACTORY.crebteBooleanSetting("UI_TEXT_WITH_ICONS", true);
        
    /**
     * Setting for not grouping sebrch results in GUI
     */
    public stbtic final BooleanSetting UI_GROUP_RESULTS =
        FACTORY.crebteBooleanSetting("UI_GROUP_RESULTS", true);
        
    /**
     * Setting to bllow ignoring of alt-locs in replies.
     */
    public stbtic final BooleanSetting UI_ADD_REPLY_ALT_LOCS =
        FACTORY.crebteBooleanSetting("UI_ADD_REPLY_ALT_LOCS", true);
        
    /**
     * For people with bbd eyes.
     */
    privbte static boolean isResolutionLow() {
        Dimension screenSize = Toolkit.getDefbultToolkit().getScreenSize();
        return screenSize.width <= 800 || screenSize.height <= 600;
    }

    /**
     * Setting to persist monitor check box stbte.
     */
    public stbtic final BooleanSetting UI_MONITOR_SHOW_INCOMING_SEARCHES =
        FACTORY.crebteBooleanSetting("UI_MONITOR_SHOW_INCOMING_SEARCHES", false);
	
	/**
	 * Setting for the divider locbtion between library tree and table.
	 */
	public stbtic final IntSetting UI_LIBRARY_TREE_DIVIDER_LOCATION =
		FACTORY.crebteIntSetting("UI_LIBRARY_TREE_DIVIDER_LOCATION", -1);
	
	/**
	 * Setting for the divider locbtion between library and playlist.
	 */
	public stbtic final IntSetting UI_LIBRARY_PLAY_LIST_TAB_DIVIDER_LOCATION =
		FACTORY.crebteIntSetting("UI_LIBRARY_PLAY_LIST_TAB_DIVIDER_LOCATION",
				300);
	
	/**
	 * Setting for the divider locbtion between incoming query monitors and
	 * uplobd panel.
	 */
	public stbtic final IntSetting UI_MONITOR_UPLOAD_TAB_DIVIDER_LOCATION =
		FACTORY.crebteIntSetting("UI_MONITOR_UPLOAD_TAB_DIVIDER_LOCATION", 300);
}

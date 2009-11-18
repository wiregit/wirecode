package org.limewire.ui.swing.settings;

import java.io.File;
import java.util.Collection;

import org.limewire.core.api.Category;
import org.limewire.core.settings.LimeProps;
import org.limewire.i18n.I18nMarker;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectionPoint;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileArraySetting;
import org.limewire.setting.FileSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;

import com.google.common.collect.ImmutableList;


/**
 * Settings to deal with UI.
 */ 
public final class SwingUiSettings extends LimeProps {
    
    private SwingUiSettings() {}
    
    /** The directories that have been warned as bad Vista directories. */
    public static final FileArraySetting VISTA_WARN_DIRECTORIES =
        FACTORY.createFileArraySetting("VISTA_WARN_DIRECTORIES", new File[0]);
    
//    /**
//     * This setting is used to track whether or not the user wants to show offline buddies in the left panel.
//     */
//    public static final BooleanSetting XMPP_SHOW_OFFLINE =
//        (BooleanSetting)FACTORY.createBooleanSetting("XMPP_SHOW_OFFLINE", true).setPrivate(true);

    /**
     * Whether or not 'REMEMBER_ME' is checked -- this has nothing to do with
     * auto logging in. It's only the state of the REMEMBER ME checkbox, so if
     * you uncheck it it doesn't stay checked on the next login.
     */
    public static final BooleanSetting REMEMBER_ME_CHECKED =
        FACTORY.createBooleanSetting("REMEMBER_ME_XMPP", true);

    /**
     * Pro ads.
     */
    public static final StringArraySetting PRO_ADS =
        FACTORY.createRemoteStringArraySetting("PRO_ADS",
                new String[] {
                I18nMarker.marktr("For Turbo-Charged searches get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&21",
                "0.111111",
                I18nMarker
                        .marktr("Support LimeWire\'s peer-to-peer development. Get PRO."),
                "http://www.limewire.com/index.jsp/pro&22",
                "0.111111",
                I18nMarker
                        .marktr("Purchase LimeWire PRO to help us make downloads faster."),
                "http://www.limewire.com/index.jsp/pro&23",
                "0.111111",
                I18nMarker.marktr("For Turbo-Charged downloads get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&24",
                "0.111111",
                I18nMarker.marktr("Support open networks. Get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&25",
                "0.111111",
                I18nMarker
                        .marktr("Support open source and open protocols. Get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&26",
                "0.111111",
                I18nMarker.marktr("For Turbo-Charged performance get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&27",
                "0.111111",
                I18nMarker.marktr("Keep the Internet open. Get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&28",
                "0.111111",
                I18nMarker.marktr("Developing LimeWire costs real money. Get PRO."),
                "http://www.limewire.com/index.jsp/pro&29",
                "0.111111"},
                "UpdateSettings.proAds");
    
    /** Show classic warning. */
    public static final BooleanSetting SHOW_CLASSIC_REMINDER =
        FACTORY.createBooleanSetting("SHOW_CLASSIC_SEARCH_REMINDER", true);

    /**
     * Setting for whether or not to group similar results
     */
    public static final BooleanSetting GROUP_SIMILAR_RESULTS_ENABLED =
        FACTORY.createBooleanSetting("GROUP_SIMILAR_RESULTS_ENABLED", true);

    /**
     * Setting for whether to display search tips based on file in 
     * friends libraries. If true, tips should be displayed, if false
     * they should not. 
     */
    public static final BooleanSetting SHOW_FRIEND_SUGGESTIONS =
        FACTORY.createBooleanSetting("SHOW_FRIEND_SUGGESTIONS", true);

    /**
     * Setting for whether to display old searches as search tips. If true,
     * old search will be displayed as tips, if false they won't be displayed
     */
    public static final BooleanSetting KEEP_SEARCH_HISTORY =
        FACTORY.createBooleanSetting("KEEP_SEARCH_HISTORY", true);

    /**
     * The default search category for the search bar.
     */
    public static final IntSetting DEFAULT_SEARCH_CATEGORY_ID =
        FACTORY.createIntSetting("DEFAULT_SEARCH_CATEGORY_ID", -1);

    /**
     * The default search view, list versus classic.
     */
    public static final IntSetting SEARCH_VIEW_TYPE_ID =
        FACTORY.createIntSetting("SEARCH_VIEW_TYPE_ID", -1);
    
    @SuppressWarnings("unused")
    @InspectableContainer
    private static class LazyInspectableContainer {
        @InspectionPoint(value = "search view", category = DataCategory.USAGE)
        private static final Inspectable numberOfLists = new Inspectable() {
            @Override
            public Object inspect() {
                return SEARCH_VIEW_TYPE_ID.get() == 1 ? "table" : "list";
            }
        };   
    }

    /**
     * Auto rename new downloads with filenames matching old downloads.
     */
    public static final BooleanSetting AUTO_RENAME_DUPLICATE_FILES =
        FACTORY.createBooleanSetting("AUTO_RENAME_DUPLICATE_FILES", true);

    /** True if any positions have been set. */
    @InspectionPoint(value = "application positions set", category = DataCategory.USAGE)
    public static final BooleanSetting POSITIONS_SET =
        FACTORY.createBooleanSetting("POSITIONS_SET", false);

    /**
     * The width that the application should be.
     */
    @InspectionPoint(value = "application width", category = DataCategory.USAGE)
    public static final IntSetting APP_WIDTH =
        FACTORY.createIntSetting("APP_WIDTH_V5", 1024);

    /**
     * The height that the application should be.
     */
    @InspectionPoint(value = "application height", category = DataCategory.USAGE)
    public static final IntSetting APP_HEIGHT =
        FACTORY.createIntSetting("APP_HEIGHT_V5", 768);

    /**
     * The x position of the window for the next time the application
     * is started.
     */
    public static final IntSetting WINDOW_X =
        FACTORY.createIntSetting("WINDOW_X_V5", 0).setAlwaysSave(true);

    /**
     * The y position of the window for the next time the application
     * is started.
     */
    public static final IntSetting WINDOW_Y =
        FACTORY.createIntSetting("WINDOW_Y_V5", 0).setAlwaysSave(true);

    /** Setting for whether or not LW should start maximized. */
    @InspectionPoint(value = "is application maximized", category = DataCategory.USAGE)
    public static final BooleanSetting MAXIMIZE_WINDOW =
        FACTORY.createBooleanSetting("MAXIMIZE_WINDOW_V5", false);

    /**
     * A flag for whether or not the application should be minimized
     * to the system tray on windows.
     */
    public static final BooleanSetting MINIMIZE_TO_TRAY =
        FACTORY.createBooleanSetting("MINIMIZE_TO_TRAY", 
            OSUtils.supportsTray());

    /**
     * Whether LimeWire should handle magnets.
     */
    public static final BooleanSetting HANDLE_MAGNETS = 
    	FACTORY.createBooleanSetting("HANDLE_MAGNETS", true);

    /**
     * Whether LimeWire should handle torrents.
     */
    public static final BooleanSetting HANDLE_TORRENTS = 
    	FACTORY.createBooleanSetting("HANDLE_TORRENTS", true);

    /**
     * Whether LimeWire should warn user about file association changes.
     */
    public static final BooleanSetting WARN_FILE_ASSOCIATION_CHANGES = 
        FACTORY.createBooleanSetting("WARN_FILE_ASSOCIATION_CHANGES", true);

    /** The last directory used for opening a file chooser. */
    public static final FileSetting LAST_FILECHOOSER_DIRECTORY =
        FACTORY.createFileSetting("LAST_FILECHOOSER_DIR", getDefaultLastFileChooserDir()).setAlwaysSave(true);

    /** Whether collecting and reporting usage stats is allowed.*/
    public static final BooleanSetting USAGE_STATS =
        FACTORY.createBooleanSetting("USAGE_STATS", false);

    /** Setting for if native icons should be preloaded. */
    public static final BooleanSetting PRELOAD_NATIVE_ICONS =
        FACTORY.createBooleanSetting("PRELOAD_NATIVE_ICONS", true);

    /**
     * Setting that globally enables or disables notifications.
     */
    public static final BooleanSetting SHOW_NOTIFICATIONS = 
        FACTORY.createBooleanSetting("SHOW_NOTIFICATIONS", true);

    /**
     * Setting that globally enables or disables notification sounds.
     */
    public static final BooleanSetting PLAY_NOTIFICATION_SOUND =
        FACTORY.createBooleanSetting("PLAY_NOTIFICATION_SOUND", false);

    /** User-defined custom jabber server. */
    public static final StringSetting USER_DEFINED_JABBER_SERVICENAME =
        (StringSetting)FACTORY.createStringSetting("CUSTOM_JABBER_SERVICENAME", "").setPrivate(true);

    public static final StringSetting XMPP_AUTO_LOGIN =
    (StringSetting)FACTORY.createStringSetting("XMPP_AUTO_LOGIN", "").setPrivate(true);
    
    /** If the 'offline contacts' in the nav are collapsed. */
    public static final BooleanSetting OFFLINE_COLLAPSED = 
        FACTORY.createBooleanSetting("OFFLINE_CONTACTS_COLLAPSED", true);
    
    /** If the 'online contacts' in the nav are collapsed. */
    public static final BooleanSetting ONLINE_COLLAPSED =
        FACTORY.createBooleanSetting("ONLINE_CONTACTS_COLLAPSED", false);

    /** If the LimeWire media player is enabled. */
    @InspectionPoint(value = "player enabled", category = DataCategory.USAGE)
    public static final BooleanSetting PLAYER_ENABLED =
        FACTORY.createBooleanSetting("PLAYER_ENABLED", true);
    
    /** Volume for the LimeWire media player. */
    public static final FloatSetting PLAYER_VOLUME =
        FACTORY.createFloatSetting("PLAYER_VOLUME", .5f, 0, 1);
      
    /** Setting for whether or not video should stretch to fill the screen. */
    public static final BooleanSetting VIDEO_FIT_TO_SCREEN =
        FACTORY.createBooleanSetting("VIDEO_FIT_TO_SCREEN", false);
    
    /** Shows a sign-on overlay message to highlight the Library/Private Shared feature. */
    public static final BooleanSetting SHOW_LIBRARY_OVERLAY_MESSAGE =
        FACTORY.createBooleanSetting("SHOW_LIBRARY_OVERLAY_MESSAGE", true);
    
    /** Shows a sign-on overlay message to highlight the Sharing features. */
    public static final BooleanSetting SHOW_SHARING_OVERLAY_MESSAGE =
        FACTORY.createBooleanSetting("SHOW_SHARING_OVERLAY_MESSAGE", true);
    
    /** If the Library filters are displayed or not. */
    @InspectionPoint(value = "library filters showing", category = DataCategory.USAGE)
    public static final BooleanSetting SHOW_LIBRARY_FILTERS =
        FACTORY.createBooleanSetting("SHOW_LIBRARY_FILTERS", true);

    /** Setting for whether or not to resolve host names in Advanced Tools. */
    public static final BooleanSetting RESOLVE_CONNECTION_HOSTNAMES =
        FACTORY.createBooleanSetting("RESOLVE_CONNECTION_HOSTNAMES", true);
    
    /** Saves the bottom tray size when the tray is resized. */
    public static final IntSetting BOTTOM_TRAY_SIZE = 
        FACTORY.createIntSetting("DOWNLOAD_TRAY_SIZE", 0);

    /** Hides the bottom tray size when all transfers are cleared. */
    public static final BooleanSetting HIDE_BOTTOM_TRAY_WHEN_NO_TRANSFERS =
        FACTORY.createBooleanSetting("HIDE_BOTTOM_TRAY_WHEN_NO_TRANSFERS", true);
    
    /** Displays total bandwidth for transfers in bottom tray header. */
    public static final BooleanSetting SHOW_TOTAL_BANDWIDTH = 
        FACTORY.createBooleanSetting("SHOW_TOTAL_BANDWIDTH", false);
    
    /**
     * Returns the default directory for the file chooser.
     * Defaults to the users home directory if it exists,
     * otherwise the current directory is used. 
     */
    private static File getDefaultLastFileChooserDir() {
        File defaultDirectory = CommonUtils.getUserHomeDir();
        if(defaultDirectory == null || !defaultDirectory.exists()) {
            defaultDirectory = CommonUtils.getCurrentDirectory();
        }
        return defaultDirectory;
    }
    
    /** True if AUDIO files are added by default when adding a folder. */
    public static final BooleanSetting CATEGORY_AUDIO_DEFAULT =
        FACTORY.createBooleanSetting("MANAGE_AUDIO_FILES", true);
    
    /** True if VIDEO files are added by default when adding a folder. */
    public static final BooleanSetting CATEGORY_VIDEO_DEFAULT =
        FACTORY.createBooleanSetting("MANAGE_VIDEO_FILES", true);
    
    /** True if IMAGES files are added by default when adding a folder. */
    public static final BooleanSetting CATEGORY_IMAGES_DEFAULT =
        FACTORY.createBooleanSetting("MANAGE_IMAGES_FILES", true);
    
    /** True if DOCUMENTS files are added by default when adding a folder. */
    public static final BooleanSetting CATEGORY_DOCUMENTS_DEFAULT =
        FACTORY.createBooleanSetting("MANAGE_DOCUMENTS_FILES", false);
    
    /** True if PROGRAMS files are added by default when adding a folder. */
    public static final BooleanSetting CATEGORY_PROGRAMS_DEFAULT =
        FACTORY.createBooleanSetting("MANAGE_PROGRAMS_FILES", false);
    
    /** Returns all categories that should be selected by default when adding a folder. */
    public static Collection<Category> getDefaultSelectedCategories() {
        ImmutableList.Builder<Category> builder = ImmutableList.builder();
        if(CATEGORY_AUDIO_DEFAULT.get()) {
            builder.add(Category.AUDIO);
        }
        if(CATEGORY_DOCUMENTS_DEFAULT.get()) {
            builder.add(Category.DOCUMENT);
        }
        if(CATEGORY_IMAGES_DEFAULT.get()) {
            builder.add(Category.IMAGE);
        }
        if(CATEGORY_PROGRAMS_DEFAULT.get()) {
            builder.add(Category.PROGRAM);
        }
        if(CATEGORY_VIDEO_DEFAULT.get()) {
            builder.add(Category.VIDEO);
        }
        return builder.build();
    }    
    
    /**
     * True if Store related items should be shown, false otherwise.
     */
    public static final BooleanSetting SHOW_STORE_COMPONENTS = 
        FACTORY.createBooleanSetting("SHOW_STORE_COMPONENTS", false);
    
    /**
     * Whether or not to show the downloads tray. Whether there are any active downloads or not.
     */
    public static final BooleanSetting SHOW_TRANSFERS_TRAY = FACTORY.createBooleanSetting(
            "ALWAYS_SHOW_DOWNLOADS_TRAY", false);
}

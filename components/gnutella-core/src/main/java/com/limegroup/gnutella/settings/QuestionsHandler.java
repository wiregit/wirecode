package com.limegroup.gnutella.settings;

import com.limegroup.gnutella.util.*;
import java.util.Properties;
import java.io.*;


/**
 * Controls all 'Do not ask this again' or 'Always use this answer' questions.
 */
public class QuestionsHandler extends AbstractSettings {

    private static final QuestionsHandler INSTANCE =
        new QuestionsHandler();
    private static final SettingsFactory FACTORY =
        INSTANCE.getFactory();

    private QuestionsHandler() {
        super("questions.props", "LimeWire questions file");
    }

    public static QuestionsHandler instance() {
        return INSTANCE;
    }

    //////////// The actual questions ///////////////

    /**
    * Setting for whether or not to allow multiple instances of LimeWire.
    */
    public static final BooleanSetting MONITOR_VIEW =
        FACTORY.createBooleanSetting("MONITOR_VIEW", false);

    /**
     * Setting for whether or not to ask about discarding corrupt downloads
     */
    public static final IntSetting CORRUPT_DOWNLOAD =
        FACTORY.createIntSetting("CORRUPT_DOWNLOAD", 0);

    /**
     * Setting for whether or not to display a browse host failed
     */
    public static final BooleanSetting BROWSE_HOST_FAILED =
        FACTORY.createBooleanSetting("BROWSE_HOST_FAILED", false);

    /**
     * Setting for unsharing directory
     */
    public static final IntSetting UNSHARE_DIRECTORY =
        FACTORY.createIntSetting("UNSHARE_DIRECTORY", 0);

    /**
     * Setting for the theme changed message
     */
    public static final BooleanSetting THEME_CHANGED =
        FACTORY.createBooleanSetting("THEME_CHANGED", false);

    /**
     * Setting for already downloading message
     */
    public static final BooleanSetting ALREADY_DOWNLOADING =
        FACTORY.createBooleanSetting("ALREADY_DOWNLOADING", false);

    /**
     * Setting for removing the last column
     */
    public static final BooleanSetting REMOVE_LAST_COLUMN =
        FACTORY.createBooleanSetting("REMOVE_LAST_COLUMN", false);

    /**
     * Setting for being unable to resume an incomplete file
     */
    public static final BooleanSetting CANT_RESUME =
        FACTORY.createBooleanSetting("CANT_RESUME", false);
}

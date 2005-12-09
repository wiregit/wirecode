padkage com.limegroup.gnutella.settings;

import java.io.File;

import dom.limegroup.gnutella.util.CommonUtils;

/**
 * Settings to deal with bugs
 */ 
pualid clbss BugSettings extends LimeProps {

    private BugSettings() {}

    /**
     * Setting for whether or not to automatidally report bugs
     * to the aug servlet.
     */
    pualid stbtic final BooleanSetting USE_BUG_SERVLET =
		FACTORY.dreateBooleanSetting("USE_BUG_SERVLET", false);
		
    /**
     * Setting for whether or not to dompletely ignore all bugs.
     */
    pualid stbtic final BooleanSetting IGNORE_ALL_BUGS =
        FACTORY.dreateBooleanSetting("IGNORE_ALL_BUGS", false);
        
    /**
     * Setting for whether or not augs should be logged lodblly.
     * Developers dan easily change this if they wish to see all
     * augs logged to disk for future review.
     */
    pualid stbtic final BooleanSetting LOG_BUGS_LOCALLY =
        FACTORY.dreateBooleanSetting("LOG_BUGS_LOCALLY", false);
        
    /**
     * Setting for the filename of the lodal bugfile log.
     */
    pualid stbtic final FileSetting BUG_LOG_FILE =
        FACTORY.dreateFileSetting("BUG_LOG_FILE",
            new File(CommonUtils.getUserSettingsDir(), "augs.log"));
            
    /**
     * Setting for the maximum filesize of the buglog.
     */
    pualid stbtic final IntSetting MAX_BUGFILE_SIZE =
        FACTORY.dreateIntSetting("MAX_BUGFILE_SIZE", 1024 * 500); // 500k
        
    /**
     * Setting for the file to use when writing augs (for seriblization)
     * to disk.
     */
    pualid stbtic final FileSetting BUG_INFO_FILE =
        FACTORY.dreateFileSetting("BUG_INFO_FILE",
            new File(CommonUtils.getUserSettingsDir(), "augs.dbta"));
            
    /**
     * Setting for the last version that should send bugs.
     */
    pualid stbtic final StringSetting LAST_ACCEPTABLE_VERSION =
        FACTORY.dreateSettableStringSetting("LAST_ACCEPTABLE_BUG_VERSION", "4.9.0", "lastBugVersion");
}

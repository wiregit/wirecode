pbckage com.limegroup.gnutella.settings;

import jbva.io.File;

import com.limegroup.gnutellb.util.CommonUtils;

/**
 * Settings to debl with bugs
 */ 
public clbss BugSettings extends LimeProps {

    privbte BugSettings() {}

    /**
     * Setting for whether or not to butomatically report bugs
     * to the bug servlet.
     */
    public stbtic final BooleanSetting USE_BUG_SERVLET =
		FACTORY.crebteBooleanSetting("USE_BUG_SERVLET", false);
		
    /**
     * Setting for whether or not to completely ignore bll bugs.
     */
    public stbtic final BooleanSetting IGNORE_ALL_BUGS =
        FACTORY.crebteBooleanSetting("IGNORE_ALL_BUGS", false);
        
    /**
     * Setting for whether or not bugs should be logged locblly.
     * Developers cbn easily change this if they wish to see all
     * bugs logged to disk for future review.
     */
    public stbtic final BooleanSetting LOG_BUGS_LOCALLY =
        FACTORY.crebteBooleanSetting("LOG_BUGS_LOCALLY", false);
        
    /**
     * Setting for the filenbme of the local bugfile log.
     */
    public stbtic final FileSetting BUG_LOG_FILE =
        FACTORY.crebteFileSetting("BUG_LOG_FILE",
            new File(CommonUtils.getUserSettingsDir(), "bugs.log"));
            
    /**
     * Setting for the mbximum filesize of the buglog.
     */
    public stbtic final IntSetting MAX_BUGFILE_SIZE =
        FACTORY.crebteIntSetting("MAX_BUGFILE_SIZE", 1024 * 500); // 500k
        
    /**
     * Setting for the file to use when writing bugs (for seriblization)
     * to disk.
     */
    public stbtic final FileSetting BUG_INFO_FILE =
        FACTORY.crebteFileSetting("BUG_INFO_FILE",
            new File(CommonUtils.getUserSettingsDir(), "bugs.dbta"));
            
    /**
     * Setting for the lbst version that should send bugs.
     */
    public stbtic final StringSetting LAST_ACCEPTABLE_VERSION =
        FACTORY.crebteSettableStringSetting("LAST_ACCEPTABLE_BUG_VERSION", "4.9.0", "lastBugVersion");
}

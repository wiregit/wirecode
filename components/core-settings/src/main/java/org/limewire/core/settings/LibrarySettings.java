package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

public class LibrarySettings extends LimeProps {
    

    /** The list used in LimeWire 5.0 & 5.1 of what friends have new image files always shared with them. */
    @Deprecated
    static StringArraySetting SHARE_NEW_IMAGES_ALWAYS =
        (StringArraySetting)FACTORY.createStringArraySetting("SHARE_NEW_IMAGES_ALWAYS", new String[0]).setPrivate(true);
    
    /** The list used in LimeWire 5.0 & 5.1 of what friends have new audio files always shared with them. */
    @Deprecated
    static StringArraySetting SHARE_NEW_AUDIO_ALWAYS =
        (StringArraySetting)FACTORY.createStringArraySetting("SHARE_NEW_AUDIO_ALWAYS", new String[0]).setPrivate(true);

    /** The list used in LimeWire 5.0 & 5.1 of what friends have new video files always shared with them. */
    @Deprecated
    static StringArraySetting SHARE_NEW_VIDEO_ALWAYS =
        (StringArraySetting)FACTORY.createStringArraySetting("SHARE_NEW_VIDEO_ALWAYS", new String[0]).setPrivate(true);

    
    
    /**True, only takes a snapshot of your current files, false enables 
     * current and future files to be automatically shared. */
    public static final BooleanSetting SNAPSHOT_SHARING_ENABLED =
        FACTORY.createBooleanSetting("SNAPSHOT_SHARING_ENABLED", true);
    
    /** True if documents can be shared with gnutella. */
    public static final BooleanSetting ALLOW_DOCUMENT_GNUTELLA_SHARING =
        FACTORY.createBooleanSetting("DOCUMENT_SHARING_ENABLED", false);
    
    /** True if programs are allowed in the library at all. */
    public static final BooleanSetting ALLOW_PROGRAMS =
        FACTORY.createBooleanSetting("PROGRAMS_ALLOWED", false);
    
    /** The current version of the library. */
    public static final StringSetting VERSION =
        FACTORY.createStringSetting("LIBRARY_VERSION", LibraryVersion.FOUR_X.name());
    
    /** True if AUDIO files are managed. */
    public static final BooleanSetting MANAGE_AUDIO =
        FACTORY.createBooleanSetting("MANAGE_AUDIO_FILES", true);
    
    /** True if VIDEO files are managed. */
    public static final BooleanSetting MANAGE_VIDEO =
        FACTORY.createBooleanSetting("MANAGE_VIDEO_FILES", true);
    
    /** True if IMAGES files are managed. */
    public static final BooleanSetting MANAGE_IMAGES =
        FACTORY.createBooleanSetting("MANAGE_IMAGES_FILES", true);
    
    /** True if DOCUMENTS files are managed. */
    public static final BooleanSetting MANAGE_DOCUMENTS =
        FACTORY.createBooleanSetting("MANAGE_DOCUMENTS_FILES", true);
    
    /** True if PROGRAMS files are managed. */
    public static final BooleanSetting MANAGE_PROGRAMS =
        FACTORY.createBooleanSetting("MANAGE_PROGRAMS_FILES", false);
    
    /** True if OTHER files are managed. */
    public static final BooleanSetting MANAGE_OTHER =
        FACTORY.createBooleanSetting("MANAGE_OTHER_FILES", true);
    
    public static enum LibraryVersion {
        FOUR_X, FIVE_0_0;
    }
    
}

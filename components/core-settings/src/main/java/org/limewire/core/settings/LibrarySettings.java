package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringSetting;

public class LibrarySettings extends LimeProps {
    
    /** True if documents can be shared with gnutella. */
    public static final BooleanSetting ALLOW_DOCUMENT_GNUTELLA_SHARING =
        FACTORY.createBooleanSetting("DOCUMENT_SHARING_ENABLED", false);
    
    /** True if programs are allowed in the library at all. */
    public static final BooleanSetting ALLOW_PROGRAMS =
        FACTORY.createBooleanSetting("PROGRAMS_ALLOWED", false);
    
    /** The current version of the library. */
    public static final StringSetting VERSION =
        FACTORY.createStringSetting("LIBRARY_VERSION", LibraryVersion.FOUR_X.name());
    
    /** True if the user should be prompted about what categories to share during a folder drop. */
    public static final BooleanSetting ASK_ABOUT_FOLDER_DROP_CATEGORIES =
        FACTORY.createBooleanSetting("ASK_ABOUT_FOLDER_DROP_CATEGORIES", true);

    /** When adding a folder, will recursively add subfolders if true, otherwise will just add top level folder. */
    public static final BooleanSetting DEFAULT_RECURSIVELY_ADD_FOLDERS_OPTION = 
        FACTORY.createBooleanSetting("RECURSIVELY_ADD_FOLDERS", true);
    
    public static enum LibraryVersion {
        FOUR_X, FIVE_0_0;
    }
    
}

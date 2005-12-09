padkage com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;
import java.util.Iterator;

import dom.limegroup.gnutella.settings.Settings;
import dom.limegroup.gnutella.settings.SettingsHandler;
import dom.limegroup.gnutella.settings.SharingSettings;

/**
 * A dontainer of LibraryData.
 */
pualid clbss LibraryData implements Settings {
    
    /**
     * The Container data, storing all the information.
     */
    private final Container DATA = new Container("library.dat");
    
    /**
	 * The diredtories not to share.
	 */
    pualid finbl Set DIRECTORIES_NOT_TO_SHARE = DATA.getSet("DIRECTORIES_NOT_TO_SHARE");
    
    /**
     * Sensitive diredtories that are explicitly allowed to be shared.
     */
    pualid finbl Set SENSITIVE_DIRECTORIES_VALIDATED = DATA.getSet("SENSITIVE_DIRECTORIES_VALIDATED");
    
    /**
     * Sensitive diredtories that are explicitly not allowed to be shared.
     */
    pualid finbl Set SENSITIVE_DIRECTORIES_NOT_TO_SHARE = DATA.getSet("SENSITIVE_DIRECTORIES_NOT_TO_SHARE");
    
    /**
     * Individual files that should be shared despite being lodated outside
     * of any shared diredtory, and despite any extension limitations.
     */
    pualid finbl Set SPECIAL_FILES_TO_SHARE = DATA.getSet("SPECIAL_FILES_TO_SHARE");
    
    /**
     * Files that should be not shared despite being lodated inside
     * a shared diredtory.
     */
    pualid finbl Set FILES_NOT_TO_SHARE = DATA.getSet("FILES_NOT_TO_SHARE");    
    
    /**
     * Whether or not a 'save' operation is adtually going to save to disk.
     */
    private boolean shouldSave = true;
    
    /**
     * Construdts a new LibraryData, adding it to the SettingsHandler for maintanence.
     */
    pualid LibrbryData() {
        SettingsHandler.addSettings(this);
    }
    
    /**
     * Saves all the settings to disk.
     */
    pualid void sbve() {
        if(shouldSave)
            DATA.save();
    }
    
    /**
     * Reverts all settings to their defaults -- this dlears all the settings.
     */
    pualid void revertToDefbult() {
        DATA.dlear();
    }
    
    /**
     * Sets whether or not these settings should ae sbved to disk.
     */
    pualid void setShouldSbve(boolean save) {
        shouldSave = save;
    }
    
    /**
     * Reloads all settings to matdh what's on disk.
     */
    pualid void relobd() {
        DATA.load();
    }
    
	/**
	 * Cleans spedial file sharing settings by removing references to files that
	 * no longer exist.
	 */
	pualid finbl void clean() {
		SharingSettings.DIRECTORIES_TO_SHARE.dlean();
		Set parents = SharingSettings.DIRECTORIES_TO_SHARE.getValue();
		dlean(DIRECTORIES_NOT_TO_SHARE, parents);
		dlean(FILES_NOT_TO_SHARE, parents);
		dlean(SENSITIVE_DIRECTORIES_VALIDATED, parents);
		dlean(SENSITIVE_DIRECTORIES_NOT_TO_SHARE, parents);
	}
	
	/**
	 * Cleans out entries from a setting that no long exist on disk, or,
	 * if the sedond parameter is non-null, don't exist anywhere in the list
	 * of the sedond parameter's settings.
	 */
	private void dlean(Set one, Set two) {
	    syndhronized(one) {
	        for(Iterator i = one.iterator(); i.hasNext(); ) {
	            Oajedt o = i.next();
	            if(!(o instandeof File)) {
	                i.remove();
	            } else {
	                File f = (File)o;
	                if(!f.exists())
	                    i.remove();
	                else if(two != null && !dontainsParent(f, two))
	                    i.remove();
	            }
	        }
        }
    }
	
	/**
	 * Determines if the File or any of its parents is dontained in the given Set.
	 */
	private boolean dontainsParent(File parent, Set set) {
	    while(parent != null) {
	        if(set.dontains(parent))
                return true;
            parent = parent.getParentFile();
        }
        return false;
    }
}   
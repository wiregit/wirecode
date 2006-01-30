package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;
import java.util.Iterator;

import com.limegroup.gnutella.settings.Settings;
import com.limegroup.gnutella.settings.SettingsHandler;
import com.limegroup.gnutella.settings.SharingSettings;

/**
 * A container of LibraryData.
 */
public class LibraryData implements Settings {
    
    /**
     * The Container data, storing all the information.
     */
    private final Container DATA = new Container("library.dat");
    
    /**
	 * The directories not to share.
	 */
    public final Set DIRECTORIES_NOT_TO_SHARE = DATA.getSet("DIRECTORIES_NOT_TO_SHARE");
    
    /**
     * Sensitive directories that are explicitly allowed to be shared.
     */
    public final Set SENSITIVE_DIRECTORIES_VALIDATED = DATA.getSet("SENSITIVE_DIRECTORIES_VALIDATED");
    
    /**
     * Sensitive directories that are explicitly not allowed to be shared.
     */
    public final Set SENSITIVE_DIRECTORIES_NOT_TO_SHARE = DATA.getSet("SENSITIVE_DIRECTORIES_NOT_TO_SHARE");
    
    /**
     * Individual files that should be shared despite being located outside
     * of any shared directory, and despite any extension limitations.
     */
    public final Set SPECIAL_FILES_TO_SHARE = DATA.getSet("SPECIAL_FILES_TO_SHARE");
    
    /**
     * Files that should be not shared despite being located inside
     * a shared directory.
     */
    public final Set FILES_NOT_TO_SHARE = DATA.getSet("FILES_NOT_TO_SHARE");    
    
    /**
     * Whether or not a 'save' operation is actually going to save to disk.
     */
    private boolean shouldSave = true;
    
    /**
     * Constructs a new LibraryData, adding it to the SettingsHandler for maintanence.
     */
    public LibraryData() {
        SettingsHandler.addSettings(this);
    }
    
    /**
     * Saves all the settings to disk.
     */
    public void save() {
        if(shouldSave)
            DATA.save();
    }
    
    /**
     * Reverts all settings to their defaults -- this clears all the settings.
     */
    public void revertToDefault() {
        DATA.clear();
    }
    
    /**
     * Sets whether or not these settings should be saved to disk.
     */
    public void setShouldSave(boolean save) {
        shouldSave = save;
    }
    
    /**
     * Reloads all settings to match what's on disk.
     */
    public void reload() {
        DATA.load();
    }
    
	/**
	 * Cleans special file sharing settings by removing references to files that
	 * no longer exist.
	 */
	public final void clean() {
		SharingSettings.DIRECTORIES_TO_SHARE.clean();
		Set parents = SharingSettings.DIRECTORIES_TO_SHARE.getValue();
		clean(DIRECTORIES_NOT_TO_SHARE, parents);
		clean(FILES_NOT_TO_SHARE, parents);
		clean(SENSITIVE_DIRECTORIES_VALIDATED, parents);
		clean(SENSITIVE_DIRECTORIES_NOT_TO_SHARE, parents);
	}
	
	/**
	 * Cleans out entries from a setting that no long exist on disk, or,
	 * if the second parameter is non-null, don't exist anywhere in the list
	 * of the second parameter's settings.
	 */
	private void clean(Set one, Set two) {
	    synchronized(one) {
	        for(Iterator i = one.iterator(); i.hasNext(); ) {
	            Object o = i.next();
	            if(!(o instanceof File)) {
	                i.remove();
	            } else {
	                File f = (File)o;
	                if(!f.exists())
	                    i.remove();
	                else if(two != null && !containsParent(f, two))
	                    i.remove();
	            }
	        }
        }
    }
	
	/**
	 * Determines if the File or any of its parents is contained in the given Set.
	 */
	private boolean containsParent(File parent, Set set) {
	    while(parent != null) {
	        if(set.contains(parent))
                return true;
            parent = parent.getParentFile();
        }
        return false;
    }
}   
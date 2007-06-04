package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.limewire.setting.AbstractSettings;
import org.limewire.setting.SettingsHandler;
import org.limewire.setting.evt.SettingsEvent.Type;

import com.limegroup.gnutella.settings.SharingSettings;

/**
 * A container of LibraryData.
 */
public class LibraryData extends AbstractSettings {
    
    /**
     * The Container data, storing all the information.
     */
    private final Container DATA = new Container("library.dat");
    
    /**
	 * The directories not to share.
	 */
    public final Set<File> DIRECTORIES_NOT_TO_SHARE = DATA.getSet("DIRECTORIES_NOT_TO_SHARE");
    
    /**
     * Sensitive directories that are explicitly allowed to be shared.
     */
    public final Set<File> SENSITIVE_DIRECTORIES_VALIDATED = DATA.getSet("SENSITIVE_DIRECTORIES_VALIDATED");
    
    /**
     * Sensitive directories that are explicitly not allowed to be shared.
     */
    public final Set<File> SENSITIVE_DIRECTORIES_NOT_TO_SHARE = DATA.getSet("SENSITIVE_DIRECTORIES_NOT_TO_SHARE");
    
    /**
     * Individual files that should be shared despite being located outside
     * of any shared directory, and despite any extension limitations.
     */
    public final Set<File> SPECIAL_FILES_TO_SHARE = DATA.getSet("SPECIAL_FILES_TO_SHARE");
    
    /**
     * Files that should be not shared despite being located inside
     * a shared directory.
     */
    public final Set<File> FILES_NOT_TO_SHARE = DATA.getSet("FILES_NOT_TO_SHARE");    
    
    /**
     * Constructs a new LibraryData, adding it to the SettingsHandler for maintanence.
     */
    public LibraryData() {
        SettingsHandler.instance().addSettings(this);
    }
    
    /**
     * Saves all the settings to disk.
     */
    public void save() {
        if (getShouldSave()) {
            DATA.save();
            fireSettingsEvent(Type.SAVE);
        }
    }
    
    /**
     * Reverts all settings to their defaults -- this clears all the settings.
     */
    public void revertToDefault() {
        DATA.clear();
        fireSettingsEvent(Type.REVERT_TO_DEFAULT);
    }
    
    /**
     * Reloads all settings to match what's on disk.
     */
    public void reload() {
        DATA.load();
        fireSettingsEvent(Type.RELOAD);
    }
    
	/**
	 * Cleans special file sharing settings by removing references to files that
	 * no longer exist.
	 */
	public final void clean() {
		SharingSettings.DIRECTORIES_TO_SHARE.clean();
		Set<File> parents = SharingSettings.DIRECTORIES_TO_SHARE.getValue();
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
	private void clean(Set<File> one, Set<File> two) {
	    synchronized(one) {
	        for(Iterator<File> i = one.iterator(); i.hasNext(); ) {
	            File f = i.next();
                if(!f.exists())
                    i.remove();
                else if(two != null && !containsParent(f, two))
                    i.remove();
	        }
        }
    }
	
	/**
	 * Determines if the File or any of its parents is contained in the given Set.
	 */
	private boolean containsParent(File parent, Set<File> set) {
	    while(parent != null) {
	        if(set.contains(parent))
                return true;
            parent = parent.getParentFile();
        }
        return false;
    }
}   
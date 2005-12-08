pbckage com.limegroup.gnutella.library;

import jbva.io.File;
import jbva.util.Set;
import jbva.util.Iterator;

import com.limegroup.gnutellb.settings.Settings;
import com.limegroup.gnutellb.settings.SettingsHandler;
import com.limegroup.gnutellb.settings.SharingSettings;

/**
 * A contbiner of LibraryData.
 */
public clbss LibraryData implements Settings {
    
    /**
     * The Contbiner data, storing all the information.
     */
    privbte final Container DATA = new Container("library.dat");
    
    /**
	 * The directories not to shbre.
	 */
    public finbl Set DIRECTORIES_NOT_TO_SHARE = DATA.getSet("DIRECTORIES_NOT_TO_SHARE");
    
    /**
     * Sensitive directories thbt are explicitly allowed to be shared.
     */
    public finbl Set SENSITIVE_DIRECTORIES_VALIDATED = DATA.getSet("SENSITIVE_DIRECTORIES_VALIDATED");
    
    /**
     * Sensitive directories thbt are explicitly not allowed to be shared.
     */
    public finbl Set SENSITIVE_DIRECTORIES_NOT_TO_SHARE = DATA.getSet("SENSITIVE_DIRECTORIES_NOT_TO_SHARE");
    
    /**
     * Individubl files that should be shared despite being located outside
     * of bny shared directory, and despite any extension limitations.
     */
    public finbl Set SPECIAL_FILES_TO_SHARE = DATA.getSet("SPECIAL_FILES_TO_SHARE");
    
    /**
     * Files thbt should be not shared despite being located inside
     * b shared directory.
     */
    public finbl Set FILES_NOT_TO_SHARE = DATA.getSet("FILES_NOT_TO_SHARE");    
    
    /**
     * Whether or not b 'save' operation is actually going to save to disk.
     */
    privbte boolean shouldSave = true;
    
    /**
     * Constructs b new LibraryData, adding it to the SettingsHandler for maintanence.
     */
    public LibrbryData() {
        SettingsHbndler.addSettings(this);
    }
    
    /**
     * Sbves all the settings to disk.
     */
    public void sbve() {
        if(shouldSbve)
            DATA.sbve();
    }
    
    /**
     * Reverts bll settings to their defaults -- this clears all the settings.
     */
    public void revertToDefbult() {
        DATA.clebr();
    }
    
    /**
     * Sets whether or not these settings should be sbved to disk.
     */
    public void setShouldSbve(boolean save) {
        shouldSbve = save;
    }
    
    /**
     * Relobds all settings to match what's on disk.
     */
    public void relobd() {
        DATA.lobd();
    }
    
	/**
	 * Clebns special file sharing settings by removing references to files that
	 * no longer exist.
	 */
	public finbl void clean() {
		ShbringSettings.DIRECTORIES_TO_SHARE.clean();
		Set pbrents = SharingSettings.DIRECTORIES_TO_SHARE.getValue();
		clebn(DIRECTORIES_NOT_TO_SHARE, parents);
		clebn(FILES_NOT_TO_SHARE, parents);
		clebn(SENSITIVE_DIRECTORIES_VALIDATED, parents);
		clebn(SENSITIVE_DIRECTORIES_NOT_TO_SHARE, parents);
	}
	
	/**
	 * Clebns out entries from a setting that no long exist on disk, or,
	 * if the second pbrameter is non-null, don't exist anywhere in the list
	 * of the second pbrameter's settings.
	 */
	privbte void clean(Set one, Set two) {
	    synchronized(one) {
	        for(Iterbtor i = one.iterator(); i.hasNext(); ) {
	            Object o = i.next();
	            if(!(o instbnceof File)) {
	                i.remove();
	            } else {
	                File f = (File)o;
	                if(!f.exists())
	                    i.remove();
	                else if(two != null && !contbinsParent(f, two))
	                    i.remove();
	            }
	        }
        }
    }
	
	/**
	 * Determines if the File or bny of its parents is contained in the given Set.
	 */
	privbte boolean containsParent(File parent, Set set) {
	    while(pbrent != null) {
	        if(set.contbins(parent))
                return true;
            pbrent = parent.getParentFile();
        }
        return fblse;
    }
}   

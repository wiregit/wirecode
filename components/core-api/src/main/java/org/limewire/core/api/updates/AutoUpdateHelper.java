package org.limewire.core.api.updates;

import java.io.File;

public interface AutoUpdateHelper {
    
    /**
     * returns true if updates are available for download
     */
    public boolean isUpdateAvailable();
       
    /**
     *  returns true if updates are ready for install
     */
    public boolean isUpdateReadyForInstall();
    
    /**
     * returns temporary working directory for auto update.
     */
    public File getTemporaryWorkingDirectory();
    
    /**
     * returns executable script file containing command(s) to update limewire.
     */
    public File getAutoUpdateCommandScript();

}

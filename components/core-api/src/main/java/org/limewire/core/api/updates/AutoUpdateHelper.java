package org.limewire.core.api.updates;

import java.io.File;

public interface AutoUpdateHelper {
    
    /**
     * returns if updates are available for download
     */
    public boolean isUpdateAvailable();
    
    
    /**
     *  returns if updates are ready for install
     */
    public boolean isUpdateReadyForInstall();
    
    /**
     * returns temporary working directory for auto update.
     */
    public File getTemporaryWorkingDirectory();
    
    /**
     * returns file containing command to update limewire.
     */
    public File getAutoUpdateCommandScript();
    
    /**
     * performs the download task.
     */
    public boolean downloadUpdates();

}

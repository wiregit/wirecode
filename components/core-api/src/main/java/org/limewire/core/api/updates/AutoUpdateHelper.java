package org.limewire.core.api.updates;


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
     * returns executable script file containing command(s) to update limewire.
     */
    public String getAutoUpdateCommand();

}

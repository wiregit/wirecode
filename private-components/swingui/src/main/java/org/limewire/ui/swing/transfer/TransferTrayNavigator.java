package org.limewire.ui.swing.transfer;

/**
 * Allows for navigating to the downloads/uploads tray views. 
 */
public interface TransferTrayNavigator {

    /**
     * Opens the tray and shows the downloads view.
     */
    public void selectDownloads();
    
    /**
     * Opens the tray and shows the uploads view.
     */
    public void selectUploads();
}

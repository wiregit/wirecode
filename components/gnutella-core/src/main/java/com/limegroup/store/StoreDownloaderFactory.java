package com.limegroup.store;

import java.io.File;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.download.DownloaderFactory;
import com.limegroup.gnutella.settings.SharingSettings;

/**
 *  Factory for downloading songs from the LimeWire Store (LWS)
 */
public class StoreDownloaderFactory implements DownloaderFactory {

    /**
     * Data needed to download a song from the store
     */
    private final StoreDescriptor storeData;
    
    /**
     * Location to save the download to
     */
    private File saveFile;
    
    public StoreDownloaderFactory(StoreDescriptor options) {
        storeData = options;
        String fileName = storeData.getFileName();
        if( fileName == null ) {
            fileName = GUIMediator.getStringResource("NO_FILENAME_LABEL");
        }
        this.saveFile = new File(SharingSettings.getSaveLWSDirectory(), fileName);
    }
    
    /**
     * @return the file name/location to save the download
     */
    public File getSaveFile() {
        return saveFile;
    }

    public void setSaveFile(File saveFile) {
        this.saveFile = saveFile;
    }
    
    /**
     * @return the hash of the file
     */
    public URN getURN() {
        return storeData.getSHA1Urn();
    }
    
    /**
     * @return the file size
     */
    public long getFileSize() {
        return storeData.getSize();
    }
    
    public Downloader createDownloader(boolean overwrite)
    throws SaveLocationException {
        return RouterService.download(storeData, overwrite, 
          saveFile.getParentFile(),
          getSaveFile().getName());
    }
}

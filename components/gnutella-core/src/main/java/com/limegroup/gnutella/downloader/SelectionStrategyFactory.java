package com.limegroup.gnutella.downloader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.settings.DownloadSettings;

/** A class to determine which SelectionStrategy should be used for a given file. */
public class SelectionStrategyFactory {
    
    private static final Log LOG = LogFactory.getLog(SelectionStrategyFactory.class);
    
    /** @param extension a String representation of a file extension, 
     *      without the leading period. 
     *  @param fileSize the size (in bytes) of the file to be downloaded.
     *  @return the proper SelectionStrategy to use, based on the input params.
     */
    public static SelectionStrategy getStrategyFor(String extension, long fileSize) {
        
        // Check if the extension matches known previewable extennsions
        if (extension != null && extension.length() > 0) {
            // Put the extension in canonical form
            extension = extension.toLowerCase();
        
            String[] previewableExtensions = DownloadSettings.PREVIEWABLE_EXTENSIONS.getValue();
        
            // Loop over all previewable extensions
            for (int i = previewableExtensions.length-1; i >= 0; i--) {
                // If the extension is previewable, return a strategy
                // favorable for previewing
                if (previewableExtensions[i].toLowerCase().equals(extension)) {
                    if (LOG.isDebugEnabled()) { 
                        LOG.debug("Extension ("+extension+") is previewable."); 
                    }
                    return new BiasedRandomDownloadStrategy(fileSize);
                }
            }
        }
        
        // By default, return a strategy that favors overall network health
        // over previews.
        if (LOG.isDebugEnabled()) { 
            LOG.debug("Extension ("+extension+") is not previewable."); 
        }
        return new RandomDownloadStrategy(fileSize);
    }
    
}

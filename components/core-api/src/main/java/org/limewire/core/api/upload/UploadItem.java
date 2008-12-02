package org.limewire.core.api.upload;

import java.beans.PropertyChangeListener;
import java.io.File;

/**
 * A single upload
 */
public interface UploadItem {

    /**
     * cancels the upload
     */
    public void cancel();

    /**
     * @return the file being uploaded
     */
    public File getFile();

    /**
     * @return the {@link UploadState} of the upload
     */
    public UploadState getState();
    
    /**
     * Returns the amount of data that this uploader and all previous
     * uploaders exchanging this file have uploaded. 
     */
    public long getTotalAmountUploaded();
    
    /**
     * @return the name of the file being uploaded.
     */
    public String getFileName();
    
    /**
     * @return the length of the file being uploaded.
     */ 
    public long getFileSize();
    
    public void addPropertyChangeListener(PropertyChangeListener listener);
    
    public void removePropertyChangeListener(PropertyChangeListener listener);
}

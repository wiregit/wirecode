package org.limewire.core.api.upload;

import java.beans.PropertyChangeListener;
import java.util.List;

import ca.odell.glazedlists.EventList;

/**
 * Defines the manager API for the list of uploads. 
 */
public interface UploadListManager {

    List<UploadItem> getUploadItems();

    EventList<UploadItem> getSwingThreadSafeUploads();
    
    /**
     * Adds the specified listener to the list that is notified when a 
     * property value changes. 
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Removes the specified listener from the list that is notified when a 
     * property value changes. 
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Checks for uploads in progress, and fires a property change event if
     * all uploads are completed.
     */
    public void updateUploadsCompleted();
    
}

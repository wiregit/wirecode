package org.limewire.inspection;

import java.io.File;

/**
 * Inspector based on a human-readable key
 */
public interface Inspector {
    /**
     * @param props a file containing mappings
     * from human-readable keys to encoded field.
     */
    void load(File props);
    
    /**
     * @return true if mappings were loaded successfully
     */
    boolean loaded();
    
    /**
     * @param key human readable key to inspect
     * @return the Object from the inspection
     * @throws InspectionException if something goes wrong or key is not found
     */
    Object inspect(String key) throws InspectionException;
}

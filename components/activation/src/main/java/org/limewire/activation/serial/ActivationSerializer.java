package org.limewire.activation.serial;

import java.io.IOException;

/**
 * Saves and loads Activation JSON strings to disk. The JSON string
 * allows ActivationItems to be recreated from disk if the server
 * cannot be contacted.
 */
public interface ActivationSerializer {
    
    public String readFromDisk() throws IOException;
    
    public boolean writeToDisk(String jsonString) throws Exception;
}

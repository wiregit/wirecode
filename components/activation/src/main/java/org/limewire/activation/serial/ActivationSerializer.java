package org.limewire.activation.serial;

import java.io.IOException;

/**
 * Saves and loads Activation JSON strings to disk. The JSON string
 * allows ActivationItems to be recreated from disk if the server
 * cannot be contacted.
 */
public interface ActivationSerializer {
    
    /**
     * Reads the saved json response from disk and returns it. If no
     * json String exists, returns null.
     */
    public String readFromDisk() throws IOException;
    
    /**
     * Writes the json String to disk.
     */
    public boolean writeToDisk(String jsonString) throws Exception;
}

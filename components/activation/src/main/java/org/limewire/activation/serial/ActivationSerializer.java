package org.limewire.activation.serial;

import java.io.IOException;
import java.util.List;

/**
 * Saves and loads ActivationModules to and from disk.
 */
public interface ActivationSerializer {

    public List<ActivationMemento> readFromDisk() throws IOException;
    
    public boolean writeToDisk(List<ActivationMemento> momentos);
}

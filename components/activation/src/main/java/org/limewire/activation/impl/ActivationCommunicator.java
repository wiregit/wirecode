package org.limewire.activation.impl;

import java.util.List;
import java.io.IOException;

import org.limewire.io.InvalidDataException;
import org.limewire.activation.impl.ActivationResponse;

/**
 * Communicates with activation server to determine which modules
 * should be activated based on certain conditions (such as a key)
 */
public interface ActivationCommunicator {

    
    /**
     * Given the key passed in, this method returns
     * the features associated with the key.
     * This information is typically stored on the server side.
     *
     * @param key key for activation
     * @return List<ActivationItem> activation items associated with key.
     */
    public ActivationResponse activate(final String key) throws IOException, InvalidDataException;
}

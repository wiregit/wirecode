package org.limewire.activation.impl;

import java.io.IOException;

import org.limewire.io.InvalidDataException;

/**
 * Communicates with activation server to determine which modules
 * should be activated based on certain conditions (such as a key)
 */
interface ActivationCommunicator {

    /**
     * Describes the type of activation. 
     */
    enum RequestType {
        AUTO_STARTUP,
        REFRESH,
        USER_ACTIVATE,
        PING
    }
    
    /**
     * Given the key passed in, this method returns
     * the features associated with the key.
     * This information is typically stored on the server side.
     *
     * @param key key for activation
     * @return activation response data object from activation server.
     * @throws java.io.IOException if the client is unable to contact the server
     * @throws org.limewire.io.InvalidDataException if a problem occurs during parsing
     */
    public ActivationResponse activate(final String key, RequestType type) throws IOException, InvalidDataException;
}

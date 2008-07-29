package org.limewire.xmpp.client.service;

import java.util.List;

import org.limewire.net.address.Address;

/**
 * Marks a presence as running in limewire.  Allows for additional limewire
 * specific features.
 */
public interface LimePresence extends Presence {

    /**
     * offer a file to this user
     * @param file
     */
    public void sendFile(FileMetaData file);

    /**
     * 
     * @return the <code>Address</code> that can be used to connect
     * to the lime buddy
     */
    public Address getAddress();
}

package org.limewire.xmpp.api.client;

import org.limewire.io.Address;

/**
 * Marks a presence as running in limewire.  Allows for additional limewire
 * specific features.
 */
public interface LimePresence extends Presence {

    /**
     * offer a file to this user; blocking call.
     * @param file
     */
    public void offerFile(FileMetaData file);

    /**
     * @return the <code>Address</code> that can be used to connect
     * to the lime buddy.
     */
    public Address getAddress();
}

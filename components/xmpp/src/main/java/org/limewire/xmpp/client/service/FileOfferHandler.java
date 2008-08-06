package org.limewire.xmpp.client.service;

import com.google.inject.Inject;

/**
 * Allows the user of the xmpp service to approve / deny file offers
 */
public interface FileOfferHandler {
    @Inject 
    public void register(XMPPService xmppService);

    /**
     * Notifies the user that a contact has offered a file to them
     * @param f the file being offered
     * @return whether the user wants the file
     */
    public void fileOfferred(FileMetaData f);
}

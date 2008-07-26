package org.limewire.xmpp.client.service;

/**
 * Allows the user of the xmpp service to approve / deny file offers
 */
public interface FileOfferHandler {
    public boolean fileOfferred(FileMetaData f);
}

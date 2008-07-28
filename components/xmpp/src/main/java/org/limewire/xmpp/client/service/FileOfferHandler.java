package org.limewire.xmpp.client.service;

import com.google.inject.Inject;

/**
 * Allows the user of the xmpp service to approve / deny file offers
 */
public interface FileOfferHandler {
    @Inject 
    public void register(XMPPService xmppService);
    public boolean fileOfferred(FileMetaData f);
}

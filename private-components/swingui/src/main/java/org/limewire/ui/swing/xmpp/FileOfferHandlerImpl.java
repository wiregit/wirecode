package org.limewire.ui.swing.xmpp;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.FileOfferHandler;
import org.limewire.xmpp.api.client.XMPPService;

@Singleton
class FileOfferHandlerImpl implements FileOfferHandler {
    
    @Inject
    public void register(XMPPService xmppService) {
        xmppService.register(this);
    }

    public void fileOfferred(FileMetaData f) {
        // TODO update UI
    }
}

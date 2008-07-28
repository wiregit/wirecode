package org.limewire.ui.swing.xmpp;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.FileOfferHandler;
import org.limewire.xmpp.client.service.XMPPService;

@Singleton
class FileOfferHandlerImpl implements FileOfferHandler {
    
    @Inject
    public void register(XMPPService xmppService) {
        xmppService.register(this);
    }

    public boolean fileOfferred(FileMetaData f) {
        return true; // TODO update UI
    }
}

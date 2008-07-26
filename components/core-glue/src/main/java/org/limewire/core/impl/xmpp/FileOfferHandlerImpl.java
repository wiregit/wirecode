package org.limewire.core.impl.xmpp;

import com.google.inject.Singleton;
import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.FileOfferHandler;

@Singleton
class FileOfferHandlerImpl implements FileOfferHandler {
    public boolean fileOfferred(FileMetaData f) {
        return true; // TODO
    }
}

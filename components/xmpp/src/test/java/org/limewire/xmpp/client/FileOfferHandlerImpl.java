package org.limewire.xmpp.client;

import org.limewire.xmpp.client.service.FileOfferHandler;
import org.limewire.xmpp.client.service.FileMetaData;

import com.google.inject.Provider;

public class FileOfferHandlerImpl implements FileOfferHandler, Provider<FileOfferHandler> {
    public boolean fileOfferred(FileMetaData f) {
        return true;
    }

    public FileOfferHandler get() {
        return this;
    }
}

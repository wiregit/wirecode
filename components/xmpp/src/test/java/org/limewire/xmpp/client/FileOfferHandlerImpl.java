package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.util.List;

import org.limewire.xmpp.client.service.FileMetaData;
import org.limewire.xmpp.client.service.FileOfferHandler;
import org.limewire.xmpp.client.service.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileOfferHandlerImpl implements FileOfferHandler {

    List<FileMetaData> offers = new ArrayList<FileMetaData>();
    
    @Inject
    public void register(XMPPService xmppService) {
        xmppService.register(this);
    }

    public void fileOfferred(FileMetaData f) {
        offers.add(f);
    }
}

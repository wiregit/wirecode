package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.util.List;

import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.FileOfferHandler;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileOfferHandlerMock implements FileOfferHandler {

    List<FileMetaData> offers = new ArrayList<FileMetaData>();
    
    @Inject
    public void register(XMPPService xmppService) {
        xmppService.setFileOfferHandler(this);
    }

    public void fileOfferred(FileMetaData f, String fromJID) {
        offers.add(f);
    }
}

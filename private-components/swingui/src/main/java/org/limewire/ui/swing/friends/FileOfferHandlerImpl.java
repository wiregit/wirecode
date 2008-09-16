package org.limewire.ui.swing.friends;

import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.xmpp.api.client.FileOfferEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FileOfferHandlerImpl implements RegisteringEventListener<FileOfferEvent> {
    
    @Inject
    public void register(ListenerSupport<FileOfferEvent> fileOfferEventListenerSupport) {
        fileOfferEventListenerSupport.addListener(this);
    }

    public void handleEvent(FileOfferEvent event) {
        new MessageReceivedEvent(new MessageImpl(null, null, event.getSource().getFromJID(),
                null, Type.FileOffer, event.getSource().getFile())).publish();
    }
}

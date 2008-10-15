package org.limewire.ui.swing.friends;

import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.xmpp.api.client.FileOfferEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Event handler for file offers that are RECEIVED
 */
@Singleton
class FileOfferHandlerImpl implements RegisteringEventListener<FileOfferEvent> {
    
    @Inject
    public void register(ListenerSupport<FileOfferEvent> fileOfferEventListenerSupport) {
        fileOfferEventListenerSupport.addListener(this);
    }

    public void handleEvent(FileOfferEvent event) {
        new MessageReceivedEvent(new MessageFileOfferImpl(null, null, event.getSource().getFromJID(),
                Type.Received, event.getSource().getFile())).publish();
    }
}

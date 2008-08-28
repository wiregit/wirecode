package org.limewire.ui.swing.friends;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.limewire.ui.swing.AbstractEDTEvent;
import org.limewire.xmpp.api.client.MessageWriter;

public class ConversationStartedEvent extends AbstractEDTEvent {
    private final Friend friend;
    private final MessageWriter writer;
    //On-off switch to signal that this event has been processed and normal execution can continue can continue
    private final CountDownLatch latch = new CountDownLatch(1);
    private final boolean locallyInitiated;

    ConversationStartedEvent(Friend friend, MessageWriter writer, boolean locallyInitiated) {
        this.friend = friend;
        this.writer = writer;
        this.locallyInitiated = locallyInitiated;
        
        friend.startChat();
    }
    
    public Friend getFriend() {
        return friend;
    }

    public MessageWriter getWriter() {
        return writer;
    }
    
    public void unlock() {
        latch.countDown();
    }
    
    public void await() {
        try {
            latch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public boolean isLocallyInitiated() {
        return locallyInitiated;
    }
}

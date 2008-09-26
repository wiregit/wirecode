package org.limewire.ui.swing.friends;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.limewire.ui.swing.AbstractEDTEvent;
import org.limewire.xmpp.api.client.MessageWriter;

public class ConversationStartedEvent extends AbstractEDTEvent {
    private final ChatFriend chatFriend;
    private final MessageWriter writer;
    //On-off switch to signal that this event has been processed and normal execution can continue can continue
    private final CountDownLatch latch = new CountDownLatch(1);
    private final boolean locallyInitiated;

    ConversationStartedEvent(ChatFriend chatFriend, MessageWriter writer, boolean locallyInitiated) {
        this.chatFriend = chatFriend;
        this.writer = writer;
        this.locallyInitiated = locallyInitiated;
        
        chatFriend.startChat();
    }
    
    public ChatFriend getFriend() {
        return chatFriend;
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

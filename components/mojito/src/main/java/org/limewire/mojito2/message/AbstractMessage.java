package org.limewire.mojito2.message;

import org.limewire.mojito.routing.Contact;

public abstract class AbstractMessage implements Message {

    private final long creationTime = System.currentTimeMillis();
    
    private final MessageID messageId;
    
    private final Contact contact;
    
    public AbstractMessage(MessageID messageId, Contact contact) {
        this.messageId = messageId;
        this.contact = contact;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
    
    @Override
    public Contact getContact() {
        return contact;
    }

    @Override
    public MessageID getMessageId() {
        return messageId;
    }
}

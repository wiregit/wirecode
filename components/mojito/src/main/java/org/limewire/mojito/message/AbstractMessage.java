package org.limewire.mojito.message;

import org.limewire.mojito.routing.Contact;

/**
 * An abstract implementation of {@link Message}.
 */
public abstract class AbstractMessage implements Message {

    private final long creationTime = System.currentTimeMillis();
    
    private final MessageID messageId;
    
    private final Contact contact;
    
    public AbstractMessage(MessageID messageId, Contact contact) {
        this.messageId = messageId;
        this.contact = contact;
    }
    
    /**
     * Returns the {@link Message}'s creation time.
     */
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

package org.limewire.mojito.message2;

import org.limewire.mojito.routing.Contact;

/**
 * 
 */
public interface Message {

    /**
     * Returns the {@link MessageID}
     */
    public MessageID getMessageId();
    
    /**
     * Returns the origin of the {@link Message}
     */
    public Contact getContact();
}

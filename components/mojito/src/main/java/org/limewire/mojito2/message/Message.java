package org.limewire.mojito2.message;

import org.limewire.mojito2.routing.Contact;

/**
 * 
 */
public interface Message {

    /** The function ID of our DHT Message */
    public static final int F_DHT_MESSAGE = 0x44; // 'D'
    
    /**
     * Returns the {@link MessageID}
     */
    public MessageID getMessageId();
    
    /**
     * Returns the origin of the {@link Message}
     */
    public Contact getContact();
}

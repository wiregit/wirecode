package org.limewire.mojito.io;

import org.limewire.mojito.message.Message;
import org.limewire.mojito.routing.Contact;

/**
 * 
 */
public interface StoreForward {

    /**
     * 
     */
    public void process(Contact contact, Message message);
}

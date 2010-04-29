package org.limewire.mojito2.io;

import org.limewire.mojito.routing.Contact;
import org.limewire.mojito2.message.Message;

/**
 * 
 */
public interface StoreForward {

    /**
     * 
     */
    public void process(Contact contact, Message message);
}

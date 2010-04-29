package org.limewire.mojito2.io;

import org.limewire.mojito2.message.Message;
import org.limewire.mojito2.routing.Contact;

/**
 * 
 */
public interface StoreForward {

    /**
     * 
     */
    public void process(Contact contact, Message message);
}

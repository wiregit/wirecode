package org.limewire.mojito.handler;

import org.limewire.mojito.message2.Message;
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

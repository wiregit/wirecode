package org.limewire.mojito.handler;

import org.limewire.mojito.messages.DHTMessage;
import org.limewire.mojito.routing.Contact;

/**
 * 
 */
public interface StoreForward {

    /**
     * 
     */
    public void process(Contact contact, DHTMessage message);
}

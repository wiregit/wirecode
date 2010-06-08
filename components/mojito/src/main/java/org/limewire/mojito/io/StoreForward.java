package org.limewire.mojito.io;

import org.limewire.mojito.message.Message;
import org.limewire.mojito.routing.Contact;

/**
 * An interface for sore-forwarding.
 */
public interface StoreForward {

    /**
     * Called for each {@link Contact} that may or may not
     * require store-forwarding.
     */
    public void process(Contact contact, Message message);
}

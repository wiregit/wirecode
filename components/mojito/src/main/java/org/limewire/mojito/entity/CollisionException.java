package org.limewire.mojito.entity;

import org.limewire.mojito.KUID;
import org.limewire.mojito.exceptions.DHTException;
import org.limewire.mojito.routing.Contact;

/**
 * An exception that is thrown during bootstrapping if a {@link KUID}
 * collision occurred with an another {@link Contact}.
 */
public class CollisionException extends DHTException {

    private final Contact contact;
    
    public CollisionException(Contact contact) {
        this.contact = contact;
    }
    
    public Contact getContact() {
        return contact;
    }
}

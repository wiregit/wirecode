package org.limewire.mojito2.entity;

import java.io.IOException;

import org.limewire.mojito2.routing.Contact;

public class CollisionException extends IOException {

    private final Contact contact;
    
    public CollisionException(Contact contact) {
        this.contact = contact;
    }
    
    public Contact getContact() {
        return contact;
    }
}

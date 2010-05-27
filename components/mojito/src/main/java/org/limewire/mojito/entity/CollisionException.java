package org.limewire.mojito.entity;

import java.io.IOException;

import org.limewire.mojito.routing.Contact;

public class CollisionException extends IOException {

    private final Contact contact;
    
    public CollisionException(Contact contact) {
        this.contact = contact;
    }
    
    public Contact getContact() {
        return contact;
    }
}

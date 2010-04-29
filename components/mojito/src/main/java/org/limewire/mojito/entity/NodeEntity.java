package org.limewire.mojito.entity;

import java.util.Map.Entry;

import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

public interface NodeEntity extends LookupEntity {

    public int size();
    
    public Entry<Contact, SecurityToken> getContact(int index);
    
    public Entry<Contact, SecurityToken>[] getContacts();
    
    public Contact[] getCollisions();
    
    public int getHop();
}

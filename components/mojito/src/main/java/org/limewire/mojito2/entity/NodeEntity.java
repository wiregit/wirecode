package org.limewire.mojito2.entity;

import java.util.Map.Entry;

import org.limewire.mojito2.routing.Contact;
import org.limewire.security.SecurityToken;

public interface NodeEntity extends LookupEntity {

    public int size();
    
    public Entry<Contact, SecurityToken>[] getContacts();
    
    public Entry<Contact, SecurityToken>[] getClosest();
    
    public Contact[] getCollisions();
    
    public int getRouteTableTimeouts();
    
    public int getTimeouts();
    
    public int getHop();
}

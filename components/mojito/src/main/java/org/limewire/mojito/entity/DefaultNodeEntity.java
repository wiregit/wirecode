package org.limewire.mojito.entity;

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.KUID;
import org.limewire.mojito.io.LookupResponseHandler.State;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.security.SecurityToken;

/**
 * The default implementation of {@link NodeEntity}.
 */
public class DefaultNodeEntity extends AbstractEntity implements NodeEntity {

    private final KUID key;
    
    private final Entry<Contact, SecurityToken>[] contacts;
    
    private final Contact[] collisions;
    
    private final int routeTableTimeouts;
    
    private final int timeouts;
    
    private final int hop;
    
    public DefaultNodeEntity(State state) {
        super(state.getTimeInMillis(), TimeUnit.MILLISECONDS);
    
        this.key = state.getKey();
        this.contacts = state.getContacts();
        this.collisions = state.getCollisions();
        this.routeTableTimeouts = state.getRouteTableTimeouts();
        this.timeouts = state.getTimeouts();
        this.hop = state.getHop();
    }

    @Override
    public Entry<Contact, SecurityToken>[] getContacts() {
        return contacts;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Entry<Contact, SecurityToken>[] getClosest() {
        Entry<Contact, SecurityToken>[] dst 
            = new Entry[Math.min(KademliaSettings.K, contacts.length)];
        System.arraycopy(contacts, 0, dst, 0, dst.length);
        return dst;
    }
    
    @Override
    public Contact[] getCollisions() {
        return collisions;
    }

    @Override
    public int getRouteTableTimeouts() {
        return routeTableTimeouts;
    }

    @Override
    public int getTimeouts() {
        return timeouts;
    }

    @Override
    public int getHop() {
        return hop;
    }
    
    @Override
    public KUID getKey() {
        return key;
    }
}

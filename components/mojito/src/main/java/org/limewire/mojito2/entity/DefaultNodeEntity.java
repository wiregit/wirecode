package org.limewire.mojito2.entity;

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.io.LookupResponseHandler.State;
import org.limewire.mojito2.routing.Contact;
import org.limewire.security.SecurityToken;

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
    public Entry<Contact, SecurityToken> getContact(int index) {
        return contacts[index];
    }

    @Override
    public Entry<Contact, SecurityToken>[] getContacts() {
        return contacts;
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
    public int size() {
        return contacts.length;
    }

    @Override
    public KUID getKey() {
        return key;
    }
}

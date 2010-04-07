package org.limewire.mojito.entity;

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.KUID;
import org.limewire.mojito.handler.response2.LookupResponseHandler.State;
import org.limewire.mojito.routing.Contact;
import org.limewire.security.SecurityToken;

public class DefaultNodeEntity extends AbstractEntity implements NodeEntity {

    private final KUID key;
    
    private final Entry<Contact, SecurityToken>[] contacts;
    
    private final int hop;
    
    public DefaultNodeEntity(State state) {
        super(state.getTimeInMillis(), TimeUnit.MILLISECONDS);
    
        this.key = state.getKey();
        this.contacts = state.getContacts();
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

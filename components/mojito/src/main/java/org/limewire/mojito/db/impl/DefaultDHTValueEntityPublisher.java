/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.limewire.mojito.db.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueEntityPublisher;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.DatabaseUtils;

/**
 * The default implementation of DHTValueEntityPublisher. 
 * It's backed by a Map and works much like a Database.
 */
public class DefaultDHTValueEntityPublisher implements DHTValueEntityPublisher {

    private final Map<KUID, DHTValueEntity> values 
        = Collections.synchronizedMap(new HashMap<KUID, DHTValueEntity>());
    
    private final Context context;
    
    public DefaultDHTValueEntityPublisher(Context context) {
        this.context = context;
    }
    
    /**
     * Adds the given KUID, DHTValue pair
     */
    public DHTValue put(KUID key, DHTValue value) {
        DHTValueEntity entity = context.createDHTValueEntity(key, value);
        DHTValueEntity old = values.put(key, entity);
        if (old != null) {
            return old.getValue();
        }
        return null;
    }
    
    /**
     * Removes the given KUID
     */
    public DHTValue remove(KUID key) {
        DHTValueEntity entity = values.remove(key);
        if (entity != null) {
            return entity.getValue();
        }
        return null;
    }
    
    /**
     * Returns true if the given KUID exists
     */
    public boolean contains(KUID key) {
        return values.containsKey(key);
    }
    
    /**
     * Returns true if the Map is empty
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }
    
    /**
     * Returns the number of elements
     */
    public int size() {
        return values.size();
    }
    
    /**
     * Clears the mapping
     */
    public void clear() {
        values.clear();
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#get(org.limewire.mojito.KUID)
     */
    public Collection<DHTValueEntity> get(KUID primaryKey) {
        DHTValueEntity entity = values.get(primaryKey);
        if (entity == null) {
            return Collections.emptySet();
        }
        return Collections.singleton(entity);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#getValuesToForward()
     */
    public Collection<DHTValueEntity> getValues() {
        synchronized (values) {
            return new ArrayList<DHTValueEntity>(values.values());
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#values()
     */
    public Collection<DHTValueEntity> getValuesToPublish() {
        List<DHTValueEntity> publish = new ArrayList<DHTValueEntity>();
        synchronized (values) {
            for (DHTValueEntity entity : values.values()) {
                if (DatabaseUtils.isPublishingRequired(entity)) {
                    publish.add(entity);
                }
            }
        }
        return publish;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#handleContactChange(org.limewire.mojito.routing.Contact)
     */
    public void changeContact(Contact node) {
        
        assert (node instanceof LocalContact);
        
        synchronized (values) {
            
            // Get a copy of the values 
            List<DHTValueEntity> entities 
                = new ArrayList<DHTValueEntity>(values.values());
            
            // Clear the values Map
            values.clear();
            
            // For each DHTValueEntity create a new DHTValueEntity
            // with the new Contact information
            for (DHTValueEntity entity : entities) {
                KUID key = entity.getKey();
                DHTValue value = entity.getValue();
                values.put(key, context.createDHTValueEntity(key, value));
            }
        }
    }
    
    public String toString() {
        synchronized (values) {
            return CollectionUtils.toString(values.values());
        }
    }
}

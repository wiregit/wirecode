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

package org.limewire.mojito.result;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.security.SecurityToken;


/**
 * The FindValueResult is fired when a FIND_VALUE lookup finishes
 */
public class FindValueResult extends LookupResult {
    
    private final Map<? extends Contact, ? extends SecurityToken> path;
    
    private final long time;
    
    private final int hop;
    
    private final Collection<? extends DHTValueEntity> entities;
    
    private final Collection<? extends EntityKey> entityKeys;
    
    public FindValueResult(KUID lookupId, 
            DHTValueType valueType,
            Map<? extends Contact, ? extends SecurityToken> path,
            Collection<? extends DHTValueEntity> entities,
            Collection<? extends EntityKey> entityKeys,
            long time, int hop) {
        super(lookupId);
        
        this.path = path;
        this.time = time;
        this.hop = hop;
        this.entities = entities;
        this.entityKeys = entityKeys;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.result.LookupPath#getPath()
     */
    public Collection<? extends Contact> getPath() {
        return path.keySet();
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.result.LookupPath#getSecurityToken(org.limewire.mojito.routing.Contact)
     */
    public SecurityToken getSecurityToken(Contact node) {
        return path.get(node);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.result.LookupPath#getEntryPath()
     */
    public Collection<? extends Entry<? extends Contact, ? extends SecurityToken>> getEntryPath() {
        return path.entrySet();
    }

    /**
     * 
     */
    public Collection<? extends DHTValueEntity> getEntities() {
        return entities;
    }
    
    /**
     * 
     */
    public Collection<? extends EntityKey> getEntityKeys() {
        return entityKeys;
    }

    /**
     * Returns the amount of time it took to find the DHTValue(s)
     */
    public long getTime() {
        return time;
    }
    
    /**
     * Returns the number of hops it took to find the DHTValue(s)
     */
    public int getHop() {
        return hop;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getLookupID()).append(" (time=").append(time)
            .append("ms, hop=").append(hop).append(")\n");

        if(entities.isEmpty() && entityKeys.isEmpty()) {
            buffer.append("No values found!");
            return buffer.toString();
        }
        
        buffer.append(CollectionUtils.toString(entities));
        buffer.append(CollectionUtils.toString(entityKeys));
        return buffer.toString();
    }
}

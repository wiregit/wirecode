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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueEntityPublisher;
import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;

/**
 * 
 */
public class DHTValuePublisherProxy implements DHTValueEntityPublisher {

    private final Set<DHTValueEntityPublisher> proxy
        = Collections.synchronizedSet(new LinkedHashSet<DHTValueEntityPublisher>());
    
    public void add(DHTValueEntityPublisher publisher) {
        if (publisher == null) {
            throw new NullPointerException("DHTValueEntityPublisher is null");
        }
        proxy.add(publisher);
    }
    
    public void remove(DHTValueEntityPublisher publisher) {
        if (publisher == null) {
            throw new NullPointerException("DHTValueEntityPublisher is null");
        }
        
        proxy.remove(publisher);
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#get(org.limewire.mojito.KUID)
     */
    public DHTValueEntity get(KUID key) {
        DHTValueEntity entity = null;
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                entity = publisher.get(key);
                if (entity != null) {
                    return entity;
                }
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#values()
     */
    public Collection<DHTValueEntity> values() {
        Set<DHTValueEntity> values = new LinkedHashSet<DHTValueEntity>();
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                values.addAll(publisher.values());
            }
        }
        return values;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#published(org.limewire.mojito.db.DHTValueEntity, org.limewire.mojito.result.StoreResult)
     */
    public void published(StoreResult result) {
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                publisher.published(result);
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValueEntityPublisher#handleContactChange(org.limewire.mojito.routing.Contact)
     */
    public void handleContactChange(DHTValueFactory factory, Contact node) {
        synchronized (proxy) {
            for (DHTValueEntityPublisher publisher : proxy) {
                publisher.handleContactChange(factory, node);
            }
        }
    }
}

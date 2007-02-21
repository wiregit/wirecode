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

package org.limewire.mojito.db;

import java.util.Collection;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;

/**
 * The DHTValueEntityPublisher provides an interface to attach 
 * an external data source to Mojito. A correct implementation
 * is crucial for the DHT.
 * 
 * Implementation detail: You may implement the DHTFutureListener
 * interface for StoreResults as well. The DHTValueManager checks 
 * if the publisher implements the DHTFutureListener and registers
 * it with the DHTValueManager's internal DHTFuture if it does.
 */
public interface DHTValueEntityPublisher {
    
    /**
     * Returns a DHTValueEntity for the given KUID or null if
     * no such DHTValueEntity exists
     * 
     * TODO: Add a second argument to specify the DHTValueType
     */
    public DHTValueEntity get(KUID secondaryKey);
    
    /**
     * Returns all DHTValueEntities
     */
    public Collection<DHTValueEntity> getValues();
    
    /**
     * Returns all DHTValueEntities that need to be published now
     */
    public Collection<DHTValueEntity> getValuesToPublish();
    
    /**
     * Returns all DHTValueEntities that can be forwared
     */
    public Collection<DHTValueEntity> getValuesToForward();
    
    /**
     * A callback method that notifies the Publisher that the
     * Node ID of the local Node changed. Use it to rebuild
     * your underlying data structure if necessary!
     */
    public void changeContact(Contact node);
}

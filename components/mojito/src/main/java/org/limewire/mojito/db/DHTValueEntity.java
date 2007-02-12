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

import java.io.Serializable;
import java.util.Map;

import org.limewire.mojito.KUID;
import org.limewire.mojito.routing.Contact;

/**
 * A DHTValueEntity
 */
public interface DHTValueEntity extends Map.Entry<KUID, DHTValue>, Serializable {
    
    /**
     * Returns the creator of this value
     */
    public Contact getCreator();
    
    /**
     * Returns the sender of this value
     */
    public Contact getSender();
    
    /**
     * Returns the primary key of this value
     */
    public KUID getKey();
    
    /**
     * Returns the secondary key of this value
     */
    public KUID getSecondaryKey();
    
    /**
     * Returns the value
     */
    public DHTValue getValue();
    
    /**
     * 
     */
    public DHTValue setValue(DHTValue value);

    /**
     * Returns the creation time
     */
    public long getCreationTime();
    
    /**
     * Returns the time when this value was published
     */
    public long getPublishTime();
    
    /**
     * Returns the number of locations where this value is stored
     */
    public int getLocationCount();
    
    /**
     * Sets the number of locations where this value is stored
     */
    public void setLocationCount(int locationCount);
    
    /**
     * Returns true if this an entity for a local value
     * and has been published at least once
     */
    public boolean hasBeenPublished();
    
    /**
     * Returns true if this is a local value
     */
    public boolean isLocalValue();
    
    /**
     * Returns true if this value was stored directly
     * by the creator of the value (that means creator
     * and sender are the same).
     */
    public boolean isDirect();
    
    /**
     * Creates a new DHTValueEntity with the given new creator
     * if this is a local value
     */
    public DHTValueEntity changeCreator(Contact creator);
}

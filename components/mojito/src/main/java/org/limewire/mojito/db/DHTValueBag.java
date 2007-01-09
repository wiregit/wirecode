/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006 LimeWire LLC
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
import java.util.Collection;
import java.util.Map;

import org.limewire.mojito.KUID;


/**
 * A interface that represents a bag of DHT values that have the same ID. 
 * Used to represent multiple values coming from different originators.
 */
public interface DHTValueBag extends Serializable {

    /**
     * Adds a <tt>DHTValue</tt> to this value bag. The value must
     * have the same ID as this bag.
     * 
     * @return true if the value was added, false otherwise
     */
    public boolean add(DHTValueEntity entity);
    
    /**
     * Removes a <tt>DHTValue</tt> from this value bag.
     * 
     * @return true if the value was removed, false otherwise
     */
    public boolean remove(DHTValueEntity entity);
    
    /**
     * Return's this Bag's value IDs.
     */
    public KUID getPrimaryKey();

    /**
     * Returns a Map values and their keys.  Make sure you hold
     * getValuesLock() while manipulating.
     */
    public Map<KUID, DHTValueEntity> getValuesMap();

    /**
     * Returns a Collection of DHTValues.  Make sure you hold
     * getValuesLock() while manipulating.
     */
    public Collection<DHTValueEntity> getAllValues();

    /**
     * Returns the request load associated with this value bag, i.e. the
     * approximate request popularity.
     */
    public float getRequestLoad();

    /**
     * Increments the request load of this bag and returns the new value
     * 
     * @return The updated request load
     */
    public float incrementRequestLoad();
    
    /**
     * Returns the number of values in this bag.
     */
    public int size();

    /**
     * Returns true if this bag is empty.
     */
    public boolean isEmpty();

    /**
     * Returns the DHTValue for the given Node ID
     */
    public DHTValueEntity get(KUID secondaryKey);
    
    /**
     * Returns true if this bag contains a value coming from the 
     * specified creator <tt>KUID</tt>.
     * 
     * @param secondaryKey The value's creator KUID
     */
    public boolean containsKey(KUID secondaryKey);
    
    /**
     * @return object whose monitor should be held while
     * reading any of  the values collections.
     */
    public Object getValuesLock();
}
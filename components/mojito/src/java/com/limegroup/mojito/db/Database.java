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

package com.limegroup.mojito.db;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.limegroup.mojito.KUID;

/**
 * An interface to implement Databases. Mojito ships
 * with an in-memory Database but you may use this
 * interface to implement a custom Database that is
 * build on top of BDBJE for example.
 */
public interface Database extends Serializable {
    
    /**
     * Adds or removes the given DHTValue depending on
     * whether or not it's empty. This is the preferred
     * method.
     * 
     * @param value DHTValue to store (add or remove)
     * @return Whether or not the given DHTValue was added or removed
     */
    public boolean store(DHTValue value);
    
    /**
     * Adds the given DHTValue to the Database
     * 
     * @param DHTValue to add
     * @return Whether or not the given DHTValue was added
     */
    public boolean add(DHTValue value);
    
    /**
     * Removes the given DHTValue from the Database
     * 
     * @param DHTValue to remove
     * @return Whether or not the given DHTValue was removed
     */
    public boolean remove(DHTValue value);
    
    /**
     * Removes the given DHTValue(s) from the Database.
     * Returns true if all values were removed.
     */
    //public boolean removeAll(Collection<? extends DHTValue> values);
    
    /**
     * Returns a Map of NodeID -> DHTValue for the given ValueID.
     * The returned Map is unmodifyable!
     */
    public Map<KUID, DHTValue> get(KUID valueId);
    
    /**
     * Returns whether or not the given DHTValue is stored in our
     * Database
     */
    public boolean contains(DHTValue value);
    
    /**
     * Returns all Keys. The returned Set is unmodifyable!
     */
    public Set<KUID> keySet();
    
    /**
     * Returns all DHTValues. The returned Collection
     * is unmodifyable!
     */
    public Collection<DHTValue> values();
    
    /**
     * Returns the number of Keys in the Database
     */
    public int getKeyCount();
    
    /**
     * Returns the number of Values in the Database
     * which is greater or equal to key count.
     */
    public int getValueCount();
    
    /**
     * Clears the Database
     */
    public void clear();
}

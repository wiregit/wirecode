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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.limewire.mojito.KUID;


/**
 * An interface to implement Databases. Mojito ships
 * with an in-memory Database but you may use this
 * interface to implement a custom Database that is
 * build on top of BDBJE for example.
 */
public interface Database extends Serializable {
    
    /**
     * Sets the DatabaseSecurityConstraint. Use null to reset 
     * the default constraint.
     * 
     * @param securityConstraint The security constraint 
     */
    public void setDatabaseSecurityConstraint(
            DatabaseSecurityConstraint securityConstraint);
    
    /**
     * Adds or removes the given DHTValue depending on
     * whether or not it's empty. 
     * 
     * @param entity DHTValue to store (add or remove)
     * @return Whether or not the given DHTValue was added or removed
     */
    public boolean store(DHTValueEntity entity);
    
    /**
     * Adds the given DHTValueEntity to the Database
     */
    //public boolean add(DHTValueEntity entity);
    
    /**
     * Removes the given DHTValue from the Database
     * 
     * @param DHTValueImpl to remove
     * @return Whether or not the given DHTValue was removed
     */
    public DHTValueEntity remove(KUID primaryKey, KUID secondaryKey);
    
    /**
     * Returns whether or not the given DHTValue is stored in our
     * Database
     */
    public boolean contains(KUID primaryKey, KUID secondaryKey);
    
    /**
     * Returns a DHTValueBag for the given ValueID, or null if no bag exists.
     * 
     * @param primaryKey The KUID of the value to lookup in the database
     */
    public Map<KUID, DHTValueEntity> get(KUID primaryKey);
    
    /**
     * 
     */
    public float getRequestLoad(KUID primaryKey, boolean incrementLoad);
    
    /**
     * Returns all Keys
     */
    public Set<KUID> keySet();
    
    /**
     * Returns a Collection of DHTValueEntities 
     */
    public Collection<DHTValueEntity> values();
    
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

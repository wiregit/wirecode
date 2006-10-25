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

package com.limegroup.mojito.db.impl;

import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueBag;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.DatabaseSecurityConstraint;

/**
 * A default implementation of DatabaseSecurityConstraint
 */
public class DefaultDatabaseSecurityConstraint implements DatabaseSecurityConstraint {
    
    private static final long serialVersionUID = 4513377023367562179L;

    public boolean allowStore(Database database, DHTValueBag bag, DHTValue value) {
        
        // Allow as many local values as you want!
        if (value.isLocalValue()) {
            return true;
        }
        
        // TODO allow as many signed values as you want?
        
        DatabaseImpl impl = (DatabaseImpl)database;
        int maxDatabaseSize = impl.maxDatabaseSize;
        int maxValuesPerKey = impl.maxValuesPerKey;
        
        // Limit the number of keys
        if (bag == null) {
            return (maxDatabaseSize < 0 || database.getKeyCount() < maxDatabaseSize);
        }
        
        // Limit the number of values per key
        DHTValue existing = bag.get(value.getCreatorID());
        if (existing == null) {
            return (maxValuesPerKey < 0 || bag.size() < maxValuesPerKey);
        }
        
        return allowReplace(impl, bag, existing, value);
    }
    
    /**
     * Returns true if it's OK to replace the existing value with
     * the new value
     */
    private boolean allowReplace(DatabaseImpl database, DHTValueBag bag, 
            DHTValue existing, DHTValue value) {
        
        // Non-local values cannot replace local values
        if (existing.isLocalValue() && !value.isLocalValue()) {
            return false;
        }
        
        // Non-direct values cannot replace direct values
        if (existing.isDirect() && !value.isDirect()) {
            return false;
        }
        
        // It's not possible to remove a value indirectly
        if (!value.isDirect() && value.isEmpty()) {
            return false;
        }
        
        // TODO signed values cannot be replaced?
        
        return true;
    }
}

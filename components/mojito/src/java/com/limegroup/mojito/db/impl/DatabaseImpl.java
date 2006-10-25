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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.DHTValueBag;
import com.limegroup.mojito.db.Database;
import com.limegroup.mojito.db.DatabaseSecurityConstraint;

/*
 * Multiple values per key and one value per nodeId under a certain key
 * 
 * valueId
 *   nodeId
 *     value
 *   nodeId
 *     value
 * valueId
 *   nodeId
 *     value
 *   nodeId
 *     value
 *   nodeId
 *     value
 */

/**
 * This is a simple implementation of the Database interface.
 * 
 * TODO: For more advanced features we need some definition for
 * DHTValues (non-signed values cannot replace signed values and
 * what not).
 */
public class DatabaseImpl implements Database {
    
    private static final long serialVersionUID = -4857315774747734947L;
    
    private Map<KUID, DHTValueBag> database = new HashMap<KUID, DHTValueBag>();
    
    /**
     * The maximum database size. Can be negative to 
     * make the size unbound
     */
    final int maxDatabaseSize;
    
    /**
     * The maximum number of values per primary key. Can 
     * be negative to make the size unbound
     */
    final int maxValuesPerKey;
    
    /**
     * 
     */
    private DatabaseSecurityConstraint securityConstraint 
        = new DefaultDatabaseSecurityConstraint();
    
    public DatabaseImpl() {
        this(-1, -1);
    }
    
    public DatabaseImpl(int maxDatabaseSize, int maxValuesPerKey) {
        this.maxDatabaseSize = maxDatabaseSize;
        this.maxValuesPerKey = maxValuesPerKey;
    }
    
    public void setDatabaseSecurityConstraint(
            DatabaseSecurityConstraint securityConstraint) {
        
        if (securityConstraint == null) {
            securityConstraint = new DefaultDatabaseSecurityConstraint();
        }
        
        this.securityConstraint = securityConstraint;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#getKeyCount()
     */
    public synchronized int getKeyCount() {
        return database.size();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#getValueCount()
     */
    public synchronized int getValueCount() {
        int count = 0;
        for (DHTValueBag bag : database.values()) {
            count += bag.size();
        }
        return count;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#clear()
     */
    public synchronized void clear() {
        database.clear();
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#store(com.limegroup.mojito.db.DHTValue)
     */
    public synchronized boolean store(DHTValue value) {
        if (!allowStore(value)) {
            return false;
        }
        
        if (value.isEmpty()) {
            return remove(value);
        } else {
            return add(value);
        }
    }
    
    /**
     * 
     */
    private synchronized boolean add(DHTValue value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException();
        }
        
        KUID valueId = value.getValueID();
        DHTValueBag bag = database.get(valueId);
        
        if (bag == null) {
            bag = new DHTValueBagImpl(valueId);
        }
        
        if (bag.add(value)) {
            if (!database.containsKey(valueId)) {
                database.put(valueId, bag);
            }
            return true;
        }
        return false;
    }
    
    /*public synchronized boolean removeAll(Collection<? extends DHTValue> values) {
        boolean removedAll = true;
        for (DHTValue value : values) {
            if (!remove(value)) {
                removedAll = false;
            }
        }
        return removedAll;
    }*/
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#remove(com.limegroup.mojito.db.DHTValue)
     */
    public synchronized boolean remove(DHTValue value) {
        
        KUID valueId = value.getValueID();
        DHTValueBag bag = database.get(valueId);
        if (bag != null) {

            boolean removed = bag.remove(value);
            
            if (bag.isEmpty()) {
                database.remove(valueId);
            }
            
            return removed;
        }
        return false;
    }
    
    /**
     * 
     */
    private boolean allowStore(DHTValue value) {
        DatabaseSecurityConstraint dbsc = securityConstraint;
        if (dbsc == null) {
            return true;
        }
        
        DHTValueBag bag = database.get(value.getValueID());
        return dbsc.allowStore(this, bag, value);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#get(com.limegroup.mojito.KUID)
     */
    public synchronized DHTValueBag get(KUID valueId) {
        return database.get(valueId);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#contains(com.limegroup.mojito.db.DHTValue)
     */
    public synchronized boolean contains(DHTValue value) {
        DHTValueBag bag = get(value.getValueID()); 
        
        if (bag != null) {
            return bag.containsKey(value.getCreatorID());
        } 
        
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#keySet()
     */
    public synchronized Set<KUID> keySet() {
        return Collections.unmodifiableSet(new HashSet<KUID>(database.keySet()));
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#values()
     */
    public synchronized Collection<DHTValue> values() {
        List<DHTValue> values = new ArrayList<DHTValue>();
        for (DHTValueBag bag : database.values()) {
            values.addAll(bag.getAllValues());
        }
        return Collections.unmodifiableList(values);
    }
    
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();
        for (DHTValueBag bag : database.values()) {
            buffer.append(bag.toString());
        }
        buffer.append("-------------\n");
        buffer.append("TOTAL: ").append(getKeyCount())
            .append("/").append(getValueCount()).append("\n");
        return buffer.toString();
    }
}

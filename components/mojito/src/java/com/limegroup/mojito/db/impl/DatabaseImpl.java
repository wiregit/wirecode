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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.limegroup.mojito.KUID;
import com.limegroup.mojito.db.DHTValue;
import com.limegroup.mojito.db.Database;

public class DatabaseImpl implements Database {

    private static final long serialVersionUID = -4857315774747734947L;
    
    private transient Map<KUID, DHTValueBag> database = new HashMap<KUID, DHTValueBag>();
    
    private transient int valueCount = 0;
    
    private void init() {
        database = new HashMap<KUID, DHTValueBag>();
        valueCount = 0;
    }
    
    public synchronized int getKeyCount() {
        return database.size();
    }
    
    public synchronized int getValueCount() {
        return valueCount;
    }
    
    public synchronized boolean store(DHTValue value) {
        if (value.isEmpty()) {
            return remove(value);
        } else {
            return add(value);
        }
    }
    
    public synchronized boolean add(DHTValue value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException();
        }
        
        KUID valueId = value.getValueID();
        DHTValueBag valueMap = database.get(valueId);
        
        if (valueMap == null) {
            valueMap = new DHTValueBag();
            database.put(valueId, valueMap);
        }
        
        int size = valueMap.size();
        boolean added = valueMap.add(value);
        if (size != valueMap.size()) {
            valueCount++;
        }
        
        return added;
    }
    
    public synchronized boolean remove(DHTValue value) {
        KUID valueId = value.getValueID();
        DHTValueBag valueMap = database.get(valueId);
        if (valueMap != null) {
            int size = valueMap.size();
            valueMap.remove(value);
            if (valueMap.isEmpty()) {
                database.remove(valueId);
            }
            
            if (valueMap.size() != size) {
                valueCount--;
                return true;
            }
        }
        return false;
    }
    
    public synchronized Map<KUID, DHTValue> get(KUID valueId) {
        DHTValueBag valueMap = database.get(valueId);
        if (valueMap != null) {
            return Collections.unmodifiableMap(valueMap);
        }
        return Collections.emptyMap();
    }
    
    public synchronized boolean contains(DHTValue value) {
        DHTValueBag valueMap = database.get(value.getValueID());
        if (valueMap != null) {
            KUID nodeId = value.getOriginator().getNodeID();
            return valueMap.containsKey(nodeId);
        }
        return false;
    }
    
    public synchronized Set<KUID> keySet() {
        return Collections.unmodifiableSet(database.keySet());
    }
    
    public synchronized Collection<DHTValue> values() {
        List<DHTValue> values = new ArrayList<DHTValue>((int)(database.size() * 1.5f));
        for (DHTValueBag bag : database.values()) {
            for (DHTValue value : bag.values()) {
                values.add(value);
            }
        }
        return values;
    }
    
    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        List<DHTValue> values = null;
        
        for (DHTValueBag db : database.values()) {
            for (DHTValue value : db.values()) {
                if (value.isLocalValue()) {
                    if (values == null) {
                        values = new ArrayList<DHTValue>();
                    }
                    values.add(value);
                }
            }
        }
        
        out.writeObject(values);
    }
    
    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        
        init(); // Init transient fields
        
        List<DHTValue> values = (List<DHTValue>)in.readObject();
        if (values != null) {
            for (DHTValue value : values) {
                assert (value.isLocalValue());
                store(value);
            }
        }
    }
    
    @SuppressWarnings("serial")
    private class DHTValueBag extends LinkedHashMap<KUID, DHTValue> {

        public DHTValueBag() {
            super(5, 0.75f, true);
        }
        
        public boolean add(DHTValue value) {
            if (value.isDirect()) {
                return addDirect(value);
            } else {
                return addIndirect(value);
            }
        }
        
        protected boolean addDirect(DHTValue value) {
            assert (value.isDirect());
            
            KUID originatorId = value.getOriginator().getNodeID();
            
            // Do not replace local with non-local values
            DHTValue current = get(originatorId);
            if (current != null 
                    && current.isLocalValue() 
                    && !value.isLocalValue()) {
                return false;
            }
            
            // One value per originator
            put(originatorId, value);
            return true;
        }
        
        protected boolean addIndirect(DHTValue value) {
            assert (!value.isDirect());
            assert (!value.isLocalValue()); // cannot be a local value
            
            KUID originatorId = value.getOriginator().getNodeID();
            
            // Do not overwrite an directly stored value
            DHTValue current = get(originatorId);
            if (current != null 
                    && current.isDirect()) {
                return false;
            }
            
            // One value per originator
            put(originatorId, value);
            return true;
        }

        @Override
        public DHTValue remove(Object key) {
            DHTValue value = (DHTValue)key;
            
            if (value.isDirect() 
                    || value.isLocalValue() 
                    || value.isExpired()) {
                KUID originatorId = value.getOriginator().getNodeID();
                return super.remove(originatorId);
            }
            
            return null;
        }
    }
}

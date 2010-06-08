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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntHashMap;
import org.limewire.io.NetworkUtils;
import org.limewire.mojito.KUID;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.DatabaseSecurityConstraint;
import org.limewire.mojito.db.DefaultDatabaseSecurityConstraint;
import org.limewire.mojito.db.ValueTuple;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.util.ContactUtils;


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
 * Adds, removes and stores a {@link ValueTuple} to a 
 * database. Values are stored in-memory. 
 */ 
 /* TODO: For more advanced features we need some definition for
 * DHTValues (non-signed values cannot replace signed values and
 * what not).
 */
public class DatabaseImpl implements Database {
    
    private static final Log LOG = LogFactory.getLog(DatabaseImpl.class);
    
    public static final int IPV4_ADDRESS_NETMASK = 0xFFFFFFFF;
    
    /** LOCKING: this */
    private final Map<KUID, ValueTupleBag> database 
        = new HashMap<KUID, ValueTupleBag>();
    
    /**
     * The {@link DatabaseSecurityConstraint} handle.
     */
    private volatile DatabaseSecurityConstraint securityConstraint 
        = new DefaultDatabaseSecurityConstraint();
    
    /**
     * A Map of masked IP address to number of values.
     */
    private final IntHashMap<AtomicInteger> valuesPerNetwork 
        = new IntHashMap<AtomicInteger>();
    
    /**
     * A Map of IP address to number of values.
     */
    private final IntHashMap<AtomicInteger> valuesPerAddress 
        = new IntHashMap<AtomicInteger>();
    
    @Override
    public void setDatabaseSecurityConstraint(
            DatabaseSecurityConstraint securityConstraint) {
        
        if (securityConstraint == null) {
            securityConstraint = new DefaultDatabaseSecurityConstraint();
        }
        
        this.securityConstraint = securityConstraint;
    }
    
    @Override
    public synchronized int getKeyCount() {
        return database.size();
    }
    
    @Override
    public synchronized int getValueCount() {
        return values().size();
    }
    
    @Override
    public synchronized void clear() {
        database.clear();
    }
    
    @Override
    public synchronized boolean store(ValueTuple entity) {
        if (!allowStore(entity)) {
            return false;
        }
        
        if (entity.getValue().size() == 0) {
            return remove(entity.getPrimaryKey(), entity.getSecondaryKey()) != null;
        } else {
            return add(entity);
        }
    }
    
    /**
     * Adds the given {@link ValueTuple} to the Database succeeded.
     * @return true if adding the {@link ValueTuple} succeeded
     */
    public synchronized boolean add(ValueTuple entity) {
        KUID primaryKey = entity.getPrimaryKey();
        ValueTupleBag bag = database.get(primaryKey);
        
        if (bag == null) {
            bag = new ValueTupleBag(this, primaryKey);
        }
        
        if (bag.add(entity)) {
            if (!database.containsKey(primaryKey)) {
                database.put(primaryKey, bag);
            }
            
            incrementValuesPerAddress(entity);
            incrementValuesPerNetwork(entity);
            
            return true;
        }
        return false;
    }
    
    @Override
    public synchronized ValueTuple remove(KUID primaryKey, KUID secondaryKey) {
        
        ValueTuple entity = null;
        ValueTupleBag bag = database.get(primaryKey);
        if (bag != null && (entity = bag.remove(secondaryKey)) != null) {
            
            if (bag.isEmpty()) {
                database.remove(primaryKey);
            }
            
            decrementValuesPerAddress(entity);
            decrementValuesPerNetwork(entity);
        }
        
        return entity;
    }
    
    /**
     * Returns the number of values that are currently stored under
     * the same Class C Network.
     */
    public synchronized int getValuesPerNetwork(ValueTuple entity) {
        return getValueCount(entity, valuesPerNetwork, NetworkUtils.CLASS_C_NETMASK);
    }
    
    /**
     * Returns the number of values that are currently stored under
     * the same IP Address.
     */
    public synchronized int getValuesPerAddress(ValueTuple entity) {
        return getValueCount(entity, valuesPerAddress, IPV4_ADDRESS_NETMASK);
    }
    
    /**
     * A helper method to get the number of values that are currently stored
     * under a certain masked IP address.
     */
    private static int getValueCount(ValueTuple entity, IntHashMap<AtomicInteger> map, int netmask) {
        if (entity.isLocalValue()) {
            return 0;
        }
        
        Contact node = entity.getCreator();
        InetAddress addr = ((InetSocketAddress)node.getContactAddress()).getAddress();
        if (addr instanceof Inet4Address) {
            int masked = NetworkUtils.getMaskedIP(addr, netmask);
            AtomicInteger count = map.get(masked);
            if (count != null) {
                return count.get();
            }
        }
        
        return 0;
    }
    
    /**
     * Increments and returns the number of values that are stored under the
     * same Class C Network.
     */
    private int incrementValuesPerNetwork(ValueTuple entity) {
        return incrementValueCount(entity, valuesPerNetwork, NetworkUtils.CLASS_C_NETMASK);
    }
    
    /**
     * Increments and returns the number of values that are stored under the
     * same IP address.
     */
    private int incrementValuesPerAddress(ValueTuple entity) {
        return incrementValueCount(entity, valuesPerAddress, IPV4_ADDRESS_NETMASK);
    }
    
    /**
     * A helper method to increment the number of values that are stored
     * under a certain masked IP address.
     */
    private static int incrementValueCount(ValueTuple entity, IntHashMap<AtomicInteger> map, int netmask) {
        if (entity.isLocalValue()) {
            return 0;
        }
        
        Contact node = entity.getCreator();
        InetAddress addr = ((InetSocketAddress)node.getContactAddress()).getAddress();
        if (addr instanceof Inet4Address) {
            int masked = NetworkUtils.getMaskedIP(addr, netmask);
            
            AtomicInteger count = map.get(masked);
            if (count == null) {
                count = new AtomicInteger(0);
                map.put(masked, count);
            }
            
            return count.incrementAndGet();
        }
        return 0;
    }
    
    /**
     * Decrements and returns the number of values that are currently
     * stored under the same Class C Network.
     */
    private int decrementValuesPerNetwork(ValueTuple entity) {
        return decrementValueCount(entity, valuesPerNetwork, NetworkUtils.CLASS_C_NETMASK);
    }
    
    /**
     * Decrements and returns the number of values that are currently
     * stored under the same IP address.
     */
    private int decrementValuesPerAddress(ValueTuple entity) {
        return decrementValueCount(entity, valuesPerAddress, IPV4_ADDRESS_NETMASK);
    }
    
    /**
     * A helper method to decrement the number of values that are stored
     * under a certain masked IP address.
     */
    private static int decrementValueCount(ValueTuple entity, IntHashMap<AtomicInteger> map, int netmask) {
        if (entity.isLocalValue()) {
            return 0;
        }
        
        Contact node = entity.getCreator();
        InetAddress addr = ((InetSocketAddress)node.getContactAddress()).getAddress();
        if (addr instanceof Inet4Address) {
            int masked = NetworkUtils.getMaskedIP(addr, netmask);
            
            AtomicInteger count = map.get(masked);
            if (count != null) {
                int value = count.decrementAndGet();
                if (value == 0) {
                    map.remove(masked);
                }
                return value;
            }
        }
        
        return 0;
    }
    
    @Override
    public synchronized float getRequestLoad(KUID primaryKey, boolean incrementLoad) {
        ValueTupleBag bag = database.get(primaryKey);
        if (bag != null) {
            return bag.getRequestLoad(incrementLoad);
        }
        return 0f;
    }
    
    /**
     * An internal helper method that checks for possible flooding 
     * and then delegates calls to the <code>DatabaseSecurityConstraint</code> instance 
     * if possible.
     */
    private boolean allowStore(ValueTuple entity) {
        if (entity.isLocalValue()) {
            return true;
        }
        
        if (DatabaseSettings.VALIDATE_VALUE_CREATOR.getValue()
                && !entity.isDirect()) {
            
            if (!ContactUtils.isValidSocketAddress(entity.getCreator())) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("The Creator of " + entity + " has an invalid address");
                }
                return false;
            }
            
            if (ContactUtils.isPrivateAddress(entity.getCreator())) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("The Creator of " + entity + " has a private address");
                }
                return false;
            }
        }
        
        if (entity.getValue().size() != 0) {
            int valuesPerAddress = getValuesPerAddress(entity);
            if (DatabaseSettings.LIMIT_VALUES_PER_ADDRESS.getValue() 
                    && valuesPerAddress >= DatabaseSettings.MAX_VALUES_PER_ADDRESS.getValue()) {
                return false;
            }
            
            int valuesPerNetwork = getValuesPerNetwork(entity);
            if (DatabaseSettings.LIMIT_VALUES_PER_NETWORK.getValue()
                    && valuesPerNetwork >= DatabaseSettings.MAX_VALUES_PER_NETWORK.getValue()) {
                return false;
            }
        }
        
        // Check with the security constraint now
        ValueTupleBag bag = database.get(entity.getPrimaryKey());
        DatabaseSecurityConstraint dbsc = securityConstraint;
        if (dbsc != null && bag != null) {
            return dbsc.allowStore(this, bag.getValues(false), entity);
        }
        
        return true;
    }
    
    /**
     * For internal use only.
     */
    public synchronized ValueTupleBag getBag(KUID valueId) {
        return database.get(valueId);
    }
    
    @Override
    public synchronized Map<KUID, ValueTuple> get(KUID valueId) {
        ValueTupleBag bag = database.get(valueId);
        if (bag != null) {
            return bag.getValues(true);
        }
        return Collections.emptyMap();
    }
    
    @Override
    public synchronized boolean contains(KUID primaryKey, KUID secondaryKey) {
        ValueTupleBag bag = database.get(primaryKey); 
        return (bag != null && bag.contains(secondaryKey));
    }

    @Override
    public synchronized Set<KUID> keySet() {
        return new HashSet<KUID>(database.keySet());
    }

    @Override
    public synchronized Collection<ValueTuple> values() {
        List<ValueTuple> values = new ArrayList<ValueTuple>(getKeyCount() * 2);
        for (ValueTupleBag bag : database.values()) {
            values.addAll(bag.getValues(false).values());
        }
        return values;
    }
    
    @Override
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();
        for (ValueTupleBag bag : database.values()) {
            buffer.append(bag.toString());
        }
        
        buffer.append("-------------\n");
        buffer.append("TOTAL: ").append(getKeyCount())
            .append("/").append(getValueCount()).append("\n");
        return buffer.toString();
    }
}

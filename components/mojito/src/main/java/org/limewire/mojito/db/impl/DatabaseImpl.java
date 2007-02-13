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

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValueBag;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.DatabaseSecurityConstraint;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.util.HostFilter;
import org.limewire.util.ByteOrder;


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
    
    /** LOCKING: this */
    private final Map<KUID, DHTValueBag> database = new HashMap<KUID, DHTValueBag>();
    
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
     * The DatabaseSecurityConstraint handle
     */
    private volatile DatabaseSecurityConstraint securityConstraint 
        = new DefaultDatabaseSecurityConstraint();
    
    /**
     * A Map of raw IP address -> number of keys stored in this DB.
     */
    private Map<Integer, Integer> hostValuesMap;
    
    /**
     * The Host filter. Used to ban hosts when database flooding is detected.
     */
    private transient volatile HostFilter filter;

    
    public DatabaseImpl() {
        this(-1, -1);
    }
    
    public DatabaseImpl(int maxDatabaseSize, int maxValuesPerKey) {
        this.maxDatabaseSize = maxDatabaseSize;
        this.maxValuesPerKey = maxValuesPerKey;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#setDatabaseSecurityConstraint(com.limegroup.mojito.db.DatabaseSecurityConstraint)
     */
    public void setDatabaseSecurityConstraint(
            DatabaseSecurityConstraint securityConstraint) {
        
        if (securityConstraint == null) {
            securityConstraint = new DefaultDatabaseSecurityConstraint();
        }
        
        this.securityConstraint = securityConstraint;
    }
    
    public void setHostFilter(HostFilter filter) {
        this.filter = filter;
    }

    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#getMaxDatabaseSize()
     */
    public int getMaxDatabaseSize() {
        return maxDatabaseSize;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#getMaxValuesPerKey()
     */
    public int getMaxValuesPerKey() {
        return maxValuesPerKey;
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
    public synchronized boolean store(DHTValueEntity entity) {
        if (!allowStore(entity)) {
            return false;
        }
        
        if (entity.getValue().isEmpty()) {
            return remove(entity);
        } else {
            return add(entity);
        }
    }
    
    /**
     * Adds the given DHTValue to the Database. Returns
     * true if the operation succeeded.
     */
    private synchronized boolean add(DHTValueEntity entity) {
        if (entity.getValue().isEmpty()) {
            throw new IllegalArgumentException();
        }
        
        KUID valueId = entity.getKey();
        DHTValueBag bag = database.get(valueId);
        
        if (bag == null) {
            bag = createDHTValueBag(valueId);
        }
        
        if (bag.add(entity)) {
            if (!database.containsKey(valueId)) {
                database.put(valueId, bag);
            }
            return true;
        }
        return false;
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.mojito.db.Database#remove(com.limegroup.mojito.db.DHTValue)
     */
    public synchronized boolean remove(DHTValueEntity entity) {
        
        KUID primaryKey = entity.getKey();
        DHTValueBag bag = database.get(primaryKey);
        if (bag != null && bag.remove(entity)) {

            if (bag.isEmpty()) {
                database.remove(primaryKey);
            }
            
            if (hostValuesMap != null) {
                
                InetAddress addr = ((InetSocketAddress) 
                        entity.getCreator().getContactAddress()).getAddress();
                
                // TODO: Handle only IPv4 addresses for now. 
                // See allowStore(DHTValue) for more information!
                if (addr instanceof Inet4Address) {
                    Integer iaddr = new Integer(ByteOrder.beb2int(addr.getAddress(), 0));
                    
                    if (hostValuesMap.containsKey(iaddr)) {
                        int numKeys = hostValuesMap.get(iaddr);
                        
                        if (numKeys <= 1) {
                            hostValuesMap.remove(iaddr);
                            
                        } else if (numKeys > DatabaseSettings.MAX_KEY_PER_IP.getValue()) {
                            // The host went over the limit, thus either he is trying
                            // to legitimately remove a value, in which case we give him a chance
                            // or this method is called from the ban() method, in which case the 
                            // host should be filtered out by the HostFilter anyways
                            hostValuesMap.put(iaddr, 
                                    DatabaseSettings.MAX_KEY_PER_IP.getValue()-1);
                        } else {
                            
                            numKeys--;
                            hostValuesMap.put(iaddr, numKeys);
                        }
                    }
                }
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * An internal helper method that checks for possible flooding 
     * and then delegates calls to the DatabaseSecurityConstraint instance 
     * if possible
     */
    private boolean allowStore(DHTValueEntity entity) {
        
        DHTValueBag bag = database.get(entity.getKey());
        
        // TODO: exclude signed value also
        if (bag == null && !entity.isLocalValue()) {
            // First check if the node is flooding us with keys:
            // We can only check for flooding by the value creator, not by the sender, 
            // because that could be misused to prevent us from storing real values

            Contact creator = entity.getCreator();
            InetAddress addr = ((InetSocketAddress)creator
                                    .getContactAddress()).getAddress();
            
            // TODO: There's currently no real urge to have this for IPv6 addresses
            // in which case we'd have to use BigIntegers instead of Integers or 
            // some other type of wrapper for the address bytes... We could also use 
            // the InetAddress object as key but it has too much memory overhead...
            
            if (addr instanceof Inet4Address) {
                Integer iaddr = new Integer(ByteOrder.beb2int(addr.getAddress(), 0));
                
                int numKeys = 0;
                if (hostValuesMap == null) {
                    hostValuesMap = new HashMap<Integer, Integer>();
                    
                } else if (hostValuesMap.containsKey(iaddr)) {
                    numKeys = hostValuesMap.get(iaddr);
                }
                
                numKeys++;
                hostValuesMap.put(iaddr, numKeys);
                
                if (numKeys > DatabaseSettings.MAX_KEY_PER_IP.getValue()) {
                    
                    if(numKeys > DatabaseSettings.MAX_KEY_PER_IP_BAN_LIMIT.getValue()) {
                        // Banning will also remove the host from the Map
                        banContact(creator);
                    }
                    return false;
                }
            }
        }
        
        // Check with the security constraint now
        DatabaseSecurityConstraint dbsc = securityConstraint;
        if (dbsc != null) {
            return dbsc.allowStore(this, bag, entity);
        }
        
        return true;
    }
    
    /**
     * Bans the given Contact and removes all values the Contact
     * has ever stored in our Database
     */
    private void banContact(Contact contact) {
        // Remove all values by this contact
        for(DHTValueEntity entity: values()) {
            if(entity.getCreator().equals(contact)) {
                remove(entity);
            }
        }
        
        // Ban the Host if possible
        HostFilter f = filter;
        if (f != null) {
            f.ban(contact.getContactAddress());
        }
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
    public synchronized boolean contains(DHTValueEntity entity) {
        DHTValueBag bag = get(entity.getKey()); 
        
        if (bag != null) {
            return bag.containsKey(entity.getSecondaryKey());
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
    public synchronized Collection<DHTValueEntity> values() {
        List<DHTValueEntity> values = new ArrayList<DHTValueEntity>();
        for (DHTValueBag bag : database.values()) {
            synchronized(bag.getValuesLock()) {
                values.addAll(bag.getAllValues());
            }
        }
        return Collections.unmodifiableList(values);
    }
    
    /**
     * Factory method to create DHTValueBags
     */
    protected DHTValueBag createDHTValueBag(KUID valueId) {
        return new DHTValueBagImpl(valueId);
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

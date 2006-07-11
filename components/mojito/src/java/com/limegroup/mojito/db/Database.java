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
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.mojito.Context;
import com.limegroup.mojito.KUID;
import com.limegroup.mojito.settings.DatabaseSettings;
import com.limegroup.mojito.settings.KademliaSettings;
import com.limegroup.mojito.statistics.DatabaseStatisticContainer;
import com.limegroup.mojito.util.FixedSizeHashMap;
import com.limegroup.mojito.util.PatriciaTrie;

/**
 * 
 */
public class Database {

    private static final Log LOG = LogFactory.getLog(Database.class);

    private DatabaseMap database;

    private int localValueCount = 0;
    
    private Context context;

    private DatabaseStatisticContainer databaseStats;
    
    public Database(Context context) {
        this.context = context;
        
        database = new DatabaseMap(DatabaseSettings.MAX_DATABASE_SIZE.getValue());
        
        databaseStats = context.getDatabaseStats();
    }

    public synchronized int size() {
        return database.size();
    }

    public synchronized int getLocalValueCount() {
        return localValueCount;
    }
    
    public synchronized boolean isEmpty() {
        return database.isEmpty();
    }

    public synchronized boolean isFull() {
        return database.isFull();
    }

    public synchronized boolean add(KeyValue keyValue) {
        if (!keyValue.isEmptyValue()) {
            KUID key = keyValue.getKey();
            KeyValueBag bag = database.get(key);
            if (bag == null) {
                bag = new KeyValueBag(key);
                database.put(key, bag);
            }
    
            databaseStats.STORED_VALUES.incrementStat();
            
            if (bag.add(keyValue)) {
                if (keyValue.isLocalKeyValue()) {
                    localValueCount++;
                }
                return true;
            }
            return false;
            
        } else {
            return remove(keyValue);
        }
    }

    public synchronized boolean remove(KeyValue keyValue) {
        KUID key = keyValue.getKey();
        KeyValueBag bag = database.get(key);
        if (bag != null && bag.remove(keyValue)) {
            if (bag.isEmpty()) {
                database.remove(key);
            }
            
            databaseStats.REMOVED_VALUES.incrementStat();
            if (keyValue.isEmptyValue()) {
                localValueCount--;
            }
            return true;
        }
        return false;
    }

    public synchronized boolean contains(KeyValue keyValue) {
        KUID key = keyValue.getKey();
        KeyValueBag bag = database.get(key);
        if (bag != null && bag.contains(keyValue)) {
            return true;
        }
        return false;
    }

    public synchronized Collection<KeyValue> get(KUID key) {
        KeyValueBag bag = database.get(key);
        if (bag != null) {
            databaseStats.RETRIEVED_VALUES.incrementStat();
            return Collections.unmodifiableCollection(bag.values());
        }
        return Collections.emptyList();
    }

    public synchronized Collection<KeyValue> select(KUID key) {
        KeyValueBag bag = database.select(key);
        if (bag != null) {
            databaseStats.RETRIEVED_VALUES.incrementStat();
            return Collections.unmodifiableCollection(bag.values());
        }
        return Collections.emptyList();
    }

    public synchronized Set<KUID> getKeys() {
        HashSet<KUID> keys = new HashSet<KUID>(size());
        for (KeyValueBag kvb : database.values()) {
            keys.add(kvb.getKey());
        }
        return Collections.unmodifiableSet(keys);
    }
    
    public synchronized Collection<KeyValue> getValues() {
        ArrayList<KeyValue> keyValues = new ArrayList<KeyValue>((int)(size() * 1.5f));
        for (KeyValueBag kvb : database.values()) {
            keyValues.addAll(kvb.values());
        }
        return Collections.unmodifiableCollection(keyValues);
    }
    
    public synchronized Collection<KeyValueBag> getKeyValueBags() {
        ArrayList<KeyValueBag> bags = new ArrayList<KeyValueBag>(size());
        for(KeyValueBag kvb : database.values()) {
            bags.add(kvb);
        }
        return Collections.unmodifiableCollection(bags);
    }
    
    public boolean isOriginator(KeyValue value) 
            throws SignatureException, InvalidKeyException {
        PublicKey pubKey = context.getKeyPair().getPublic();
        return value.verify(pubKey);
    }

    public boolean isKeyValueExpired(KeyValue keyValue) {
        if (keyValue.isLocalKeyValue()) {
            return false;
        }

        // KUID key = keyValue.getKey();
        // ContactNode closest = context.getRouteTable().select(key);

        // TODO: we are not caching, expiration time constant for now

        // If RouteTable is empty. TODO: is expired or not?
        // if (closest == null) {
        // return false;
        // }

        // KUID closestId = closest.getNodeID();
        // KUID currentId = context.getLocalNodeID();
        // KUID xorId = currentId.xor(closestId);
        // int log = xorId.log();
        //
        // long creationTime = keyValue.getCreationTime();
        // long expirationTime = creationTime
        // + DatabaseSettings.MILLIS_PER_DAY/KUID.LENGTH * log;

        long expirationTime = keyValue.getCreationTime()
                + DatabaseSettings.EXPIRATION_TIME_CLOSEST_NODE.getValue();

        // TODO: this needs some finetuning. Anonymous KeyValues
        // expire 50% faster at the moment.
        try {
            if (keyValue.isAnonymous()
                    && !keyValue.verify(context.getMasterKey())
                    || !keyValue.isNearby()) {
                expirationTime = keyValue.getCreationTime()
                        + DatabaseSettings.EXPIRATION_TIME_UNKNOWN.getValue();
            }
        } catch (InvalidKeyException e) {
        } catch (SignatureException e) {
        }

        return System.currentTimeMillis() >= expirationTime;
    }

    public boolean isRepublishingRequired(KeyValue keyValue) {
        if (!keyValue.isLocalKeyValue()) {
            return false;
        }

        long t = (long)((keyValue.getNumLocs() 
                * DatabaseSettings.REPUBLISH_INTERVAL.getValue()) 
                    / KademliaSettings.REPLICATION_PARAMETER.getValue());
        
        // never republish more than every X minutes
        long nextPublishTime = Math.max(t, DatabaseSettings.MIN_REPUBLISH_INTERVAL.getValue());
        long time = keyValue.getLastPublishTime() + nextPublishTime;

        return System.currentTimeMillis() >= time;
    }
    
    public boolean isTrustworthy(KeyValue keyValue) 
            throws SignatureException, InvalidKeyException {
        PublicKey masterKey = context.getMasterKey();
        return masterKey != null && keyValue.verify(masterKey);
    }

    private static class DatabaseMap extends FixedSizeHashMap<KUID, KeyValueBag> 
            implements Serializable {

        private static final long serialVersionUID = -4796278962768822384L;

        private PatriciaTrie<KUID, KeyValueBag> trie;

        public DatabaseMap(int maxSize) {
            super(1024, 0.75f, true, maxSize);
            trie = new PatriciaTrie<KUID, KeyValueBag>();
        }

        public KeyValueBag put(KUID key, KeyValueBag value) {
            trie.put(key, value);
            return super.put(key, value);
        }

        public KeyValueBag remove(Object key) {
            trie.remove((KUID)key);
            return super.remove(key);
        }

        public KeyValueBag select(KUID key) {
            return trie.select(key);
        }

        protected boolean removeEldestEntry(Entry<KUID, KeyValueBag> eldest) {
            if (super.removeEldestEntry(eldest)) {
                remove(eldest.getKey());
            }
            return false;
        }
    }

    public class KeyValueBag implements Serializable {

        private static final long serialVersionUID = -1814254075001306181L;

        private KUID key;

        private FixedSizeHashMap<KUID, KeyValue> values;

        private KeyValueBag(KUID key) {
            this.key = key;
            values = new FixedSizeHashMap<KUID, KeyValue>(DatabaseSettings.MAX_KEY_VALUES.getValue());
        }

        public KUID getKey() {
            return key;
        }

        /**
         * 1) A trustworthy KeyValue will replace everything and 
         *    they cannot be replaced by anything except 
         *    trustworthy KeyValues.
         *    
         * 2) A local KeyValue will replace everything except
         *    trustworthy KeyValues and they cannot be replaced by
         *    anything except trustworthy or local KeyValues.
         *    
         * 3) A signed KeyValue will replace everything except
         *    trustworthy, local or an another signed KeyValue
         *    from a different originator (first comes, first serves).
         * 
         * 4) A non-anonymous KeyValue will replace every
         *    anonymous KeyValue or non-anonymous KeyValue
         *    if it is from the same source (IP:Port).
         *    
         * 5) An anonymous KeyValue will replace anonymous KeyValues.
         * 
         * TODO: We should talk about 4) and 5). It might be a good
         * Idea to not allow No. 5 and let an anonymous KeyValue
         * rather age faster...
         */
        private boolean add(KeyValue keyValue) {
            if (keyValue.isEmptyValue()) {
                throw new IllegalArgumentException();
            }
            
            return addOrRemove(keyValue);
        }
        
        /**
         * See add() for constraints!
         */
        private boolean remove(KeyValue keyValue) {
            // Network or local remove?
            if (keyValue.isEmptyValue()) {
                return addOrRemove(keyValue);
            } else {
                return values.remove(keyValue.getNodeID()) != null;
            }
        }
        
        /**
         * Combined add/remove method. See add() for constraints!
         */
        private boolean addOrRemove(KeyValue keyValue) {
            try {
                boolean isTrustworthy = isTrustworthy(keyValue);
                
                // No. 1
                if (isTrustworthy) {
                    return update(keyValue);
                }
                
                // If signed then make sure it's correct
                if (keyValue.isSigned()
                        && !keyValue.verify()) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Cannot add "
                            + keyValue 
                            + " because it is signed but the signature is invalid");
                    }
                    return false;
                }
                
                // If nothing to replace...
                KeyValue current = (KeyValue)values.get(keyValue.getNodeID());
                if (current == null) {
                    // ...add if not full or local
                    if (isFull() && !keyValue.isLocalKeyValue()) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Cannot store " + keyValue
                                + " because KeyValueBag is full");
                        }
                        return false;
                    }
                    
                    // No. 2
                    return update(keyValue);
                }
                
                // Cannot replace trustworthy KeyValues
                // No. 1
                if (isTrustworthy(current)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Cannot replace a trustworthy KeyValue "
                                + current
                                + " with a non-trustwothy KeyValue "
                                + keyValue);
                    }
                    return false;
                }
                
                // No. 2
                if (keyValue.isLocalKeyValue()) {
                    return update(keyValue);
                }
                
                // Cannot replace local KeyValues
                // No. 2
                if (current.isLocalKeyValue()) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Cannot replace a local KeyValue "
                                + current
                                + " with a non-local KeyValue "
                                + keyValue);
                    }
                    return false;
                }
                
                // No. 3
                if (current.isSigned()) {
                    if (keyValue.isSigned()) {
                        if (keyValue.verify(current.getPublicKey())
                                || current.verify(keyValue.getPublicKey())) {
                            return update(keyValue);
                        } else {
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Cannot replace "
                                        + current
                                        + " with "
                                        + keyValue
                                        + " because keys do not match");
                            }
                            return false;
                        }
                    } else {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Cannot replace signed KeyValue "
                                    + current
                                    + " with a non-signed KeyValue "
                                    + keyValue);
                        }
                        return false;
                    }
                    
                // A signed KeyValue will always replace  
                // a non-signed KeyValue
                } else if (keyValue.isSigned()) {
                    return update(keyValue);
                    
                // OK, neither of them is signed so our only option
                // is to check the IP:Port of the originator. If both
                // match we assume they're from the source which might
                // be wrong!
                } else {
                    SocketAddress currentSrc = current.getSocketAddress();
                    
                    // No. 4
                    if (currentSrc != null) {
                        if (currentSrc.equals(
                                keyValue.getSocketAddress())) {
                            
                            return update(keyValue);
                        } else {
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Cannot replace "
                                    + current
                                    + " with "
                                    + keyValue
                                    + " because originator addresses do not match");
                            }
                            return false;
                        }
                    } else {
                        
                        // An anonymous source cannot remove an anonymous KeyValue!
                        if (!keyValue.isEmptyValue()) {
                            // No. 5
                            return update(keyValue);
                        }
                    }
                }
            } catch (InvalidKeyException e) {
                LOG.error("InvalidKeyException", e);
            } catch (SignatureException e) {
                LOG.error("SignatureException", e);
            }
            return false;
        }
        
        /**
         * Adds the KeyValue if it is not empty and removes it
         * if it is empty.
         */
        private boolean update(KeyValue keyValue) {
            if (keyValue.isEmptyValue()) {
                return values.remove(keyValue.getNodeID()) != null;
            } else {
                values.put(keyValue.getNodeID(), keyValue);
                return true;
            }
        }

        public KeyValue get(KUID nodeId) {
            return values.get(nodeId);
        }
        
        public boolean contains(KeyValue keyValue) {
            KUID nodeId = keyValue.getNodeID();
            KeyValue current = values.get(nodeId);
            if (current == null) {
                return false;
            }
            return current.equals(keyValue);
        }
        
        public boolean isEmpty() {
            return values.isEmpty();
        }

        public int size() {
            return values.size();
        }

        private void clear() {
            values.clear();
        }

        public Collection<KeyValue> values() {
            return Collections.unmodifiableCollection(values.values());
        }

        public KeyValue[] toArray() {
            return (KeyValue[]) values().toArray(new KeyValue[0]);
        }
        
        public int removeAll(boolean nonLocalOnly) {
            synchronized (Database.this) {
                int size = size();
                if (nonLocalOnly) {
                    for(Iterator it = values.values().iterator(); it.hasNext(); ) {
                        KeyValue keyValue = (KeyValue)it.next();
                        if (!keyValue.isLocalKeyValue()) {
                            it.remove();
                        }
                    }
                } else {
                    clear();
                }
                
                if (isEmpty()) {
                    database.remove(key);
                }
                
                return (size - size());
            }
        }
    }
}

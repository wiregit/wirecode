/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.db;

import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.settings.DatabaseSettings;
import de.kapsi.net.kademlia.util.FixedSizeHashMap;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class Database {
    
    private static final Log LOG = LogFactory.getLog(Database.class);
    
    private Context context;
    
    private DatabaseMap database;
    
    public Database(Context context) {
        this.context = context;
        
        database = new DatabaseMap(getMaxSize());
    }
    
    public int getMaxSize() {
        return DatabaseSettings.getMaxSize();
    }
    
    public int size() {
        return database.size();
    }
    
    public boolean isFull() {
        return size() >= getMaxSize();
    }
    
    public synchronized boolean add(KeyValue keyValue) 
            throws SignatureException, InvalidKeyException {
        
        if (isKeyValueExpired(keyValue)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace(keyValue + " is expired!");
            }
            return false;
        }
        
        KUID key = keyValue.getKey();
        KeyValueCollection values = (KeyValueCollection)database.get(key);
        if (values == null) {
            values = new KeyValueCollection(context, key, DatabaseSettings.getMaxValues());
            database.put(key, values);
        }
        return values.add(keyValue);
    }
    
    public synchronized int addAll(Collection c) 
            throws SignatureException, InvalidKeyException {
        
        int count = 0;
        for(Iterator it = c.iterator(); it.hasNext(); ) {
            if (add((KeyValue)it.next())) {
                count++;
            }
        }
        return count;
    }
    
    public synchronized Collection get(KUID key) {
        KeyValueCollection values = (KeyValueCollection)database.get(key);
        if (values == null) {
            return null;
        }
        
        return Collections.unmodifiableCollection(values);
    }
    
    public synchronized Collection select(KUID key) {
        KeyValueCollection values = (KeyValueCollection)database.select(key);
        if (values == null) {
            return null;
        }
        
        return Collections.unmodifiableCollection(values);
    }
    
    public synchronized boolean remove(KeyValue value) {
        KUID key = value.getKey();
        KeyValueCollection values = (KeyValueCollection)database.get(key);
        if (values == null 
                || !values.remove(value)) {
            return false;
        }
        
        if (values.isEmpty()) {
            database.remove(key);
        }
        
        /*if (!bag.remove(value)) {
            throw new IllegalStateException("Could not remove Value from Bag!");
        }*/
        return true;
    }
    
    public synchronized void removeAll(Collection c) {
        for(Iterator it = c.iterator(); it.hasNext(); ) {
            remove((KeyValue)it.next());
        }
    }
    
    public synchronized boolean contains(KUID key) {
        return database.get(key) != null;
    }
    
    public synchronized boolean contains(KeyValue value) {
        KUID key = value.getKey();
        KeyValueCollection values = (KeyValueCollection)database.get(key);
        if (value == null) {
            return false;
        }
        return values.contains(value);
    }
    
    public synchronized List getAllValues() {
        ArrayList list = new ArrayList((int)(size() * 1.25f));
        for(Iterator it = database.values().iterator(); it.hasNext(); ) {
            list.addAll(((KeyValueCollection)it.next()));
        }
        return list;
    }
    
    public synchronized Collection getAllCollections() {
        return Collections.unmodifiableCollection(database.values());
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
        
        KUID key = keyValue.getKey();
        ContactNode closest = context.getRouteTable().select(key);
        
        //TODO: we are not caching, expiration time constant for now
        
        // If RouteTable is empty. TODO: is expired or not?
//        if (closest == null) {
//            return false;
//        }
        
        
//        KUID closestId = closest.getNodeID();
//        KUID currentId = context.getLocalNodeID();
//        KUID xorId = currentId.xor(closestId);
//        int log = xorId.log();
//        
//        long creationTime = keyValue.getCreationTime();
//        long expirationTime = creationTime 
//            + DatabaseSettings.MILLIS_PER_DAY/KUID.LENGTH * log;
        
        
        long expirationTime = keyValue.getCreationTime() + DatabaseSettings.MILLIS_PER_DAY;
        
        // TODO: this needs some finetuning. Anonymous KeyValues
        // expire 50% faster at the moment.
        try {
            if (keyValue.isAnonymous()
                    && !keyValue.verify(context.getMasterKey())) {
                expirationTime /= 2;
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
        
        long time = keyValue.getRepublishTime() 
                        + DatabaseSettings.MILLIS_PER_HOUR;
        
        return System.currentTimeMillis() >= time;
    }
    
    private class DatabaseMap extends FixedSizeHashMap {
        
        private PatriciaTrie trie;
        
        public DatabaseMap(int maxSize) {
            super(1024, 0.75f, true, maxSize);
            trie = new PatriciaTrie();
        }

        public Object put(Object key, Object value) {
            trie.put(key, value);
            return super.put(key, value);
        }
        
        public Object remove(Object key) {
            Object value = super.remove(key);
            if (value != null) {
                trie.remove(key);
            }
            return value;
        }
        
        public Object select(Object key) {
            return trie.select(key);
        }
        
        protected boolean removeEldestEntry(Entry eldest) {
            if (super.removeEldestEntry(eldest)) {
                remove(eldest.getKey());
            }
            return false;
        }
    }
}

/*
 * Lime Kademlia Distributed Hash Table (DHT)
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
 
package de.kapsi.net.kademlia.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.DataBaseStatisticContainer;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.settings.DatabaseSettings;
import de.kapsi.net.kademlia.util.FixedSizeHashMap;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class Database {
    
    private static final Log LOG = LogFactory.getLog(Database.class);
    
    private Context context;
    private DataBaseStatisticContainer databaseStats;
    
    private DatabaseMap database;
    
    public Database(Context context) {
        this.context = context;
        databaseStats = context.getDataBaseStats();
        
        database = new DatabaseMap(DatabaseSettings.MAX_DATABASE_SIZE.getValue());
    }
    
    public int getMaxSize() {
        return database.getMaxSize();
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
        
        KUID key = (KUID)keyValue.getKey();
        KeyValueCollection values = (KeyValueCollection)database.get(key);
        if (values == null) {
            values = new KeyValueCollection(context, key, DatabaseSettings.MAX_KEY_VALUES.getValue());
            database.put(key, values);
        }
        databaseStats.STORED_VALUES.incrementStat();
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
        databaseStats.RETRIEVED_VALUES.incrementStat();
        return Collections.unmodifiableCollection(values);
    }
    
    public synchronized Collection select(KUID key) {
        KeyValueCollection values = (KeyValueCollection)database.select(key);
        if (values == null) {
            return null;
        }
        
        return Collections.unmodifiableCollection(values);
    }
    
    public synchronized boolean remove(KeyValue keyValue) {
        KUID key = (KUID)keyValue.getKey();
        KeyValueCollection values = (KeyValueCollection)database.get(key);
        if (values == null 
                || !values.remove(keyValue)) {
            return false;
        }
        
        if (values.isEmpty()) {
            database.remove(key);
        }
        
        /*if (!bag.remove(value)) {
            throw new IllegalStateException("Could not remove Value from Bag!");
        }*/
        databaseStats.REMOVED_VALUES.incrementStat();
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
    
    public synchronized boolean contains(KeyValue keyValue) {
        KUID key = (KUID)keyValue.getKey();
        KeyValueCollection values = (KeyValueCollection)database.get(key);
        if (keyValue == null) {
            return false;
        }
        return values.contains(keyValue);
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
        
        KUID key = (KUID)keyValue.getKey();
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
        
        
        long expirationTime = keyValue.getCreationTime() + DatabaseSettings.EXPIRATION_TIME_CLOSEST_NODE;
        
        // TODO: this needs some finetuning. Anonymous KeyValues
        // expire 50% faster at the moment.
        try {
            if (keyValue.isAnonymous()
                    && !keyValue.verify(context.getMasterKey())
                    || !keyValue.isClose()) {
                expirationTime = keyValue.getCreationTime() + DatabaseSettings.EXPIRATION_TIME_UNKNOWN;
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
    
    public synchronized boolean store() {
        File file = new File(DatabaseSettings.DATABASE_FILE);
        
        ObjectOutputStream out = null;
        
        try {
            FileOutputStream fos = new FileOutputStream(file);
            GZIPOutputStream gzout = new GZIPOutputStream(fos);
            out = new ObjectOutputStream(gzout);
            out.writeObject(context.getLocalNodeID());
            out.writeObject(database);
            out.flush();
            return true;
        } catch (FileNotFoundException e) {
            LOG.error("Database File not found error: ",e);
        } catch (IOException e) {
            LOG.error("Database IO error: ", e);
        } finally {
            try { if (out != null) { out.close(); } } catch (IOException ignore) {}
        }
        return false;
    }
    
    public synchronized boolean load() {
        File file = new File(DatabaseSettings.DATABASE_FILE);
        if (file.exists() && file.isFile() && file.canRead()) {
            
            ObjectInputStream in = null;
            try {
                FileInputStream fin = new FileInputStream(file);
                GZIPInputStream gzin = new GZIPInputStream(fin);
                in = new ObjectInputStream(gzin);
                
                KUID nodeId = (KUID)in.readObject();
                if (!nodeId.equals(context.getLocalNodeID())) {
                    return false;
                }
                
                DatabaseMap database = (DatabaseMap)in.readObject();
                for(Iterator it = database.values().iterator(); it.hasNext(); ) {
                    // Set the context
                    ((KeyValueCollection)it.next()).setContext(context);
                }
                this.database = database;
                return true;
            } catch (FileNotFoundException e) {
                LOG.error("Database File not found error: ",e);
            } catch (IOException e) {
                LOG.error("Database IO error: ", e);
            } catch (ClassNotFoundException e) {
                LOG.error("Database Class not found error: ", e);
            } finally {
                try { if (in != null) { in.close(); } } catch (IOException ignore) {}
            }
        }
        return false;
    }
    
    private static class DatabaseMap extends FixedSizeHashMap implements Serializable {
        
        private static final long serialVersionUID = -4796278962768822384L;
        
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
            trie.remove(key);
            return super.remove(key);
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

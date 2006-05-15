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
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.dht.statistics.DataBaseStatisticContainer;

import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.settings.DatabaseSettings;
import de.kapsi.net.kademlia.settings.KademliaSettings;
import de.kapsi.net.kademlia.util.FixedSizeHashMap;
import de.kapsi.net.kademlia.util.PatriciaTrie;

public class Database implements Serializable {

    private static final long serialVersionUID = 3736527702913131617L;

    private static final Log LOG = LogFactory.getLog(Database.class);

    private DatabaseMap database;

    private Context context;

    private DataBaseStatisticContainer databaseStats;
    
    public Database(Context context) {
        this.context = context;
        
        database = new DatabaseMap(DatabaseSettings.MAX_DATABASE_SIZE.getValue());
        
        databaseStats = context.getDataBaseStats();
    }

    public synchronized int size() {
        return database.size();
    }

    public synchronized boolean isEmpty() {
        return database.isEmpty();
    }

    public synchronized boolean isFull() {
        return database.isFull();
    }

    public synchronized boolean add(KeyValue keyValue) {
        if (keyValue.getValue().length > 0) {
            KUID key = keyValue.getKey();
            KeyValueBag bag = (KeyValueBag) database.get(key);
            if (bag == null) {
                bag = new KeyValueBag(key);
                database.put(key, bag);
            }
    
            databaseStats.STORED_VALUES.incrementStat();
            return bag.add(keyValue);
        } else {
            return remove(keyValue);
        }
    }

    public synchronized boolean remove(KeyValue keyValue) {
        KUID key = keyValue.getKey();
        KeyValueBag bag = (KeyValueBag) database.get(key);
        if (bag != null && bag.remove(keyValue)) {
            if (bag.isEmpty()) {
                database.remove(key);
            }
            
            databaseStats.REMOVED_VALUES.incrementStat();
            return true;
        }
        return false;
    }

    public synchronized boolean contains(KeyValue keyValue) {
        KUID key = keyValue.getKey();
        KeyValueBag bag = (KeyValueBag) database.get(key);
        if (bag != null && bag.contains(keyValue)) {
            return true;
        }
        return false;
    }

    public synchronized Collection get(KUID key) {
        KeyValueBag bag = (KeyValueBag) database.get(key);
        if (bag != null) {
            databaseStats.RETRIEVED_VALUES.incrementStat();
            return Collections.unmodifiableCollection(bag.values());
        }
        return Collections.EMPTY_LIST;
    }

    public synchronized Collection select(KUID key) {
        KeyValueBag bag = (KeyValueBag) database.select(key);
        if (bag != null) {
            databaseStats.RETRIEVED_VALUES.incrementStat();
            return Collections.unmodifiableCollection(bag.values());
        }
        return Collections.EMPTY_LIST;
    }

    public synchronized Collection getKeys() {
        ArrayList keys = new ArrayList(size());
        for(Iterator it = database.values().iterator(); it.hasNext(); ) {
            keys.add(((KeyValueBag)it.next()).getKey());
        }
        return Collections.unmodifiableCollection(keys);
    }
    
    public synchronized Collection getValues() {
        ArrayList keyValues = new ArrayList((int)(size() * 1.5f));
        for(Iterator it = database.values().iterator(); it.hasNext(); ) {
            keyValues.addAll(((KeyValueBag)it.next()).values());
        }
        return Collections.unmodifiableCollection(keyValues);
    }
    
    public synchronized Collection getKeyValueBags() {
        ArrayList bags = new ArrayList(size());
        for(Iterator it = database.values().iterator(); it.hasNext(); ) {
            bags.add((KeyValueBag)it.next());
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
                    || !keyValue.isClose()) {
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
            LOG.error("Database File not found error: ", e);
        } catch (IOException e) {
            LOG.error("Database IO error: ", e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ignore) {
            }
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

                KUID nodeId = (KUID) in.readObject();
                if (!nodeId.equals(context.getLocalNodeID())) {
                    return false;
                }

                this.database = (DatabaseMap) in.readObject();
                return true;
            } catch (FileNotFoundException e) {
                LOG.error("Database File not found error: ", e);
            } catch (IOException e) {
                LOG.error("Database IO error: ", e);
            } catch (ClassNotFoundException e) {
                LOG.error("Database Class not found error: ", e);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ignore) {
                }
            }
        }
        return false;
    }

    public boolean isTrustworthy(KeyValue keyValue) 
            throws SignatureException, InvalidKeyException {
        PublicKey masterKey = context.getMasterKey();
        return masterKey != null && keyValue.verify(masterKey);
    }

    private static class DatabaseMap extends FixedSizeHashMap implements
            Serializable {

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

    public class KeyValueBag implements Serializable {

        private static final long serialVersionUID = -1814254075001306181L;

        private KUID key;

        private FixedSizeHashMap values;

        private KeyValueBag(KUID key) {
            this.key = key;
            values = new FixedSizeHashMap(DatabaseSettings.MAX_KEY_VALUES.getValue());
        }

        public KUID getKey() {
            return key;
        }

        private boolean add(KeyValue keyValue) {
            try {
                boolean isTrustworthy = isTrustworthy(keyValue);
                boolean isLocalKeyValue = keyValue.isLocalKeyValue();

                KUID nodeId = keyValue.getNodeID();
                KeyValue current = (KeyValue) values.get(nodeId);

                if (current == null) {
                    if (values.isFull() && (!isTrustworthy || !isLocalKeyValue)) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("Cannot store " + keyValue
                                    + " because KeyValueCollection is full");
                        }
                        return false;
                    }
                } else if (!isLocalKeyValue) {

                    if (!isTrustworthy) {

                        if (isTrustworthy(current)) {
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Cannot replace "
                                                + current
                                                + " with "
                                                + keyValue
                                                + " because new KeyValue is not trustworthy");
                            }
                            return false;
                        }

                        SocketAddress currentSrc = current.getSocketAddress();
                        if (currentSrc != null
                                && !currentSrc.equals(keyValue
                                        .getSocketAddress())) {
                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Cannot replace "
                                                + current
                                                + " with "
                                                + keyValue
                                                + " because originator addresses do not match");
                            }
                            return false;
                        }
                    }
                }

                if (LOG.isTraceEnabled()) {
                    if (current != null) {
                        LOG.trace("Replacing " + current + " with " + keyValue);
                    } else {
                        LOG.trace("Adding KeyValue " + keyValue);
                    }
                }

                values.put(nodeId, keyValue);
                return true;

            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (SignatureException e) {
                throw new RuntimeException(e);
            }
        }

        public KeyValue get(KUID nodeId) {
            return (KeyValue)values.get(nodeId);
        }
        
        public boolean contains(KeyValue keyValue) {
            KUID nodeId = keyValue.getNodeID();
            KeyValue current = (KeyValue) values.get(nodeId);
            if (current == null) {
                return false;
            }
            return current.equals(keyValue);
        }

        private boolean remove(KeyValue keyValue) {
            try {
                KUID nodeId = keyValue.getNodeID();
                KeyValue current = (KeyValue) values.get(nodeId);
                if (current == null) {
                    return false;
                }
                SocketAddress currentSrc = current.getSocketAddress();
                
                boolean isTrustworthy = isTrustworthy(keyValue);
                boolean isLocalKeyValue = keyValue.isLocalKeyValue();
                
                if (isTrustworthy 
                        || (isLocalKeyValue && current.isLocalKeyValue())
                        || (currentSrc != null && currentSrc.equals(keyValue.getSocketAddress()))) {
                    
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Removed KeyValue " + current);
                    }
                    
                    values.remove(nodeId);
                    return true;
                }
                return false;
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (SignatureException e) {
                throw new RuntimeException(e);
            }
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

        public Collection values() {
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

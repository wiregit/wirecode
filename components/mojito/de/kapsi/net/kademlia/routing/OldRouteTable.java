/*
 * This is an unreleased work of Roger Kapsi.
 * All rights reserved.
 */

package de.kapsi.net.kademlia.routing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import de.kapsi.net.kademlia.ContactNode;
import de.kapsi.net.kademlia.Context;
import de.kapsi.net.kademlia.KUID;
import de.kapsi.net.kademlia.settings.RouteTableSettings;
import de.kapsi.net.kademlia.util.FixedSizeHashMap;
import de.kapsi.net.kademlia.util.PatriciaTrie;


//TODO not used anymore -- delete
public class OldRouteTable {
    
    private Context context;
    
    private RouteTableMap routeTableMap;
    private Cache cache;
    
    private HashMap staleNodes;
    
    public OldRouteTable(Context context) {
        this.context = context;
        
        routeTableMap = new RouteTableMap(65536);
        cache = new Cache(getMaxCacheSize());
        
        staleNodes = new HashMap();
    }
    
    public int getMaxCacheSize() {
        return RouteTableSettings.getMaxCacheSize();
    }
    
    public boolean isFull() {
        return routeTableMap.isFull();
    }
    
    public void clear() {
        routeTableMap.clear();
        staleNodes.clear();
        cache.clear();
    }
    
    public boolean isEmpty() {
        return routeTableMap.isEmpty();
    }
    
    public int size() {
        return routeTableMap.size();
    }
    
    public boolean isClose(KUID nodeId) {
        float min = 100f * size() / 65536;
        float closeness = 100f * context.getLocalNodeID().bitIndex(nodeId) / KUID.LENGTH;
        return closeness > min;
    }
    
    public void add(ContactNode node) {
        put(node.getNodeID(), node);
    }
    
    private void put(KUID nodeId, ContactNode node) {
        if (nodeId == null) {
            throw new IllegalArgumentException("NodeID is null");
        }
        
        if (node == null) {
            throw new IllegalArgumentException("Node is null");
        }
        
        if (!nodeId.equals(node.getNodeID())) {
            throw new IllegalArgumentException("NodeID and the ID returned by Node do not match");
        }
        
        staleNodes.remove(nodeId);
        cache.remove(nodeId);
        routeTableMap.put(nodeId, node);
    }
    
    public ContactNode get(KUID nodeId) {
        return get(nodeId, false);
    }
    
    public ContactNode get(KUID nodeId, boolean checkAndUpdateCache) {
        ContactNode node = (ContactNode)routeTableMap.get(nodeId);
        if (node == null && checkAndUpdateCache) {
            node = (ContactNode)cache.get(nodeId);
        }
        return node;
    }
    
    public ContactNode select(KUID key) {
        return (ContactNode)routeTableMap.select(key);
    }
    
    /** 
     * Returns a List of buckts sorted by their 
     * closeness to the provided Key. Use BucketList's
     * sort method to sort the Nodes by last-recently 
     * and most-recently seen.
     */
    public List select(KUID lookup, int k) {
        return routeTableMap.select(lookup, k);
    }

    public boolean containsNode(KUID nodeId) {
        return routeTableMap.containsKey(nodeId);
    }
    
    public void remove(KUID key) {
        staleNodes.remove(key);
        routeTableMap.remove(key);
        cache.remove(key);
    }
    
    public boolean updateTimeStamp(ContactNode node) {
        if (node != null) {
            node.alive();
            staleNodes.remove(node.getNodeID());
            updateIfCached(node.getNodeID());
            return true;
        }
        return false;
    }
    
    public int getFailureCount(ContactNode node) {
        if (node != null) {
            return node.getFailureCount();
        }
        return 0;
    }
    
    /**
     * Increments ContactNode's failure counter, marks it as stale
     * if a certain error level is exceeded and returns 
     * true if it's the case.
     */
    public boolean handleFailure(ContactNode node) {
        if (node != null) {
            if (node.failure() >= RouteTableSettings.getMaxNodeFailures()) {
                staleNodes.put(node.getNodeID(), node);
                return true;
            }
        }
        return false;
    }
    
    public void markStale(ContactNode node) {
        staleNodes.put(node.getNodeID(), node);
    }
    
    public boolean isStale(ContactNode node) {
        return staleNodes.containsKey(node.getNodeID());
    }
    
    public String toString() {
        return routeTableMap.toString();
    }
    
    public Map getRouteTableMap() {
        return new HashMap(routeTableMap);
    }
    
    
    /* LRU CACHE */
    
    // TODO implement!
    public  ContactNode addToCache(ContactNode node) {
        return (ContactNode)cache.put(node.getNodeID(), node);
    }
    
    public boolean isCacheEmpty() {
        return cache.isEmpty();
    }
    
    public boolean updateIfCached(KUID key) {
        return cache.get(key) != null;
    }
    
    public ContactNode getMostRecentlySeen(boolean remove) {
        return (ContactNode)cache.getMostRecentlySeen(remove);
    }
    
    public ContactNode replaceWithMostRecentlySeenNode(KUID nodeId) {        
        if (!containsNode(nodeId)) {
            return null;
        }
        
        ContactNode mostRecentlySeen = getMostRecentlySeen(true);
        // don't remove if replacement cache is empty!
        if (mostRecentlySeen != null) {
            remove(nodeId);
            put(mostRecentlySeen.getNodeID(), mostRecentlySeen);
        }
        return mostRecentlySeen;
    }
    
    public Collection getAllNodes() {
        return routeTableMap.values();
    }
    
    /**
     * A combined HashMap and PatriciaTrie. The HashMap is for
     * fast and excact O(1) lookups and the PatriciaTrie for 
     * finding the closest KUIDs in O(logN) time.
     */
    private static class RouteTableMap extends FixedSizeHashMap {
        
        private static final boolean ACCESS_ORDER = true;
        
        private final PatriciaTrie trie;
        
        private RouteTableMap(int maxSize) {
            super(1024, 0.75f, ACCESS_ORDER, maxSize);
            trie = new PatriciaTrie();
        }
        
        public Object put(Object key, Object value) {
            trie.put(key, value);
            return super.put(key, value);
        }
        
        public Object remove(Object key) {
            if (super.remove(key) != null) {
                return trie.remove(key);
            }
            return null;
        }
        
        public void clear() {
            trie.clear();
            super.clear();
        }
        
        public Object select(Object key) {
            Object value = trie.select(key);
            if (ACCESS_ORDER && value != null) {
                get(((ContactNode)value).getNodeID());
            }
            return value;
        }
        
        private List updateAccessOrder(List list) {
            if (ACCESS_ORDER) {
                for(int i = list.size()-1; i >= 0; i--) {
                    get(((ContactNode)list.get(i)).getNodeID());
                }
            }
            return list;
        }
        
        public List select(Object key, int k) {
            return updateAccessOrder(trie.select(key, k));
        }
        
        public Object getLeastRecentlySeen(boolean remove) {
            if (isEmpty()) {
                return null;
            }
            
            Iterator it = values().iterator();
            Object value = it.next();
            if (remove) {
                it.remove();
            }
            return value;
        }
        
        protected boolean removeEldestEntry(Entry eldest) {
            if (super.removeEldestEntry(eldest)) {
                trie.remove(eldest.getKey());
                return true;
            }
            return false;
        }

        public String toString() {
            return trie.toString();
        }
    }
    
    /**
     * LRU replacement cache
     */
    private static class Cache extends FixedSizeHashMap {
        
        private Cache(int maxSize) {
            super(maxSize, 0.75f, true, maxSize);
        }
        
        // O(n)
        public Object getMostRecentlySeen(boolean remove) {
            if (isEmpty()) {
                return null;
            }
            
            Object value = null;
            Iterator it = values().iterator();
            while (it.hasNext()) {
                value = it.next();
            }
            
            if (remove && value != null) {
                it.remove();
            }
            return value;
        }
    }
    
    /**
     * A combined Stale ContactNode and excude ContactNode Delegate.
     * Pass it to the Trie to exclude all stale Nodes
     * and one specific ContactNode.
     */
    private static class StaleExcludeDelegate implements Collection {
        
        private Set stale;
        private KUID exclude;
        
        public StaleExcludeDelegate(Set stale, KUID exclude) {
            this.stale = stale;
            this.exclude = exclude;
        }

        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public boolean contains(Object o) {
            return stale.contains(o) || exclude.equals(o);
        }

        public boolean containsAll(Collection c) {
            for(Iterator it = c.iterator(); it.hasNext(); ) {
                if (!contains(it.next())) {
                    return false;
                }
            }
            return true;
        }

        public boolean isEmpty() {
            return false;
        }

        public Iterator iterator() {
            return Collections.EMPTY_LIST.iterator();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            int size = stale.size();
            if (!stale.contains(exclude)) {
                size++;
            }
            return size;
        }

        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        public Object[] toArray(Object[] a) {
            throw new UnsupportedOperationException();
        }
    }
}
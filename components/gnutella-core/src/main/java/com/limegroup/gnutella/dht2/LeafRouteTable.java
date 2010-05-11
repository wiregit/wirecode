package com.limegroup.gnutella.dht2;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.collection.FixedSizeHashMap;
import org.limewire.mojito2.routing.Bucket;
import org.limewire.mojito2.routing.ClassfulNetworkCounter;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.ContactFactory;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.settings.KademliaSettings;
import org.limewire.mojito2.util.CollectionUtils;

public class LeafRouteTable implements RouteTable {
    
    private static final long serialVersionUID = 2378400850935282184L;

    private final Bucket bucket;
    
    private final Contact localNode;
    
    public LeafRouteTable(Vendor vendor, Version version) {
        localNode = ContactFactory.createLocalContact(vendor, version, true);
        bucket = new BucketImpl(this, KademliaSettings.REPLICATION_PARAMETER.getValue());
    }
    
    @Override
    public synchronized void add(Contact node) {
        if (isLocalNode(node)) {
            return;
        }
        
        if (node.isFirewalled()) {
            return;
        }
        
        ClassfulNetworkCounter counter = bucket.getClassfulNetworkCounter();
        if (counter == null || counter.isOkayToAdd(node)) {
            bucket.addActiveContact(node);
        }
    }

    @Override
    public void bind(ContactPinger pinger) {
    }

    @Override
    public void addRouteTableListener(RouteTableListener l) {
    }

    @Override
    public void removeRouteTableListener(RouteTableListener l) {
    }
    
    @Override
    public synchronized void clear() {
        bucket.clear();
    }

    @Override
    public synchronized Contact get(KUID nodeId) {
        if (nodeId.equals(localNode.getNodeID())) {
            return localNode;
        }
        
        return bucket.get(nodeId);
    }

    @Override
    public synchronized Collection<Contact> getActiveContacts() {
        List<Contact> nodes = new ArrayList<Contact>(bucket.getActiveContacts());
        nodes.add(localNode);
        return nodes;
    }

    @Override
    public synchronized Bucket getBucket(KUID nodeId) {
        return bucket;
    }

    @Override
    public synchronized Collection<Bucket> getBuckets() {
        return Collections.singleton(bucket);
    }

    @Override
    public synchronized Collection<Contact> getCachedContacts() {
        return bucket.getCachedContacts();
    }

    @Override
    public synchronized Collection<Contact> getContacts() {
        return bucket.getActiveContacts();
    }

    @Override
    public synchronized Contact getLocalNode() {
        return localNode;
    }

    @Override
    public synchronized Collection<KUID> getRefreshIDs(boolean bootstrapping) {
        return Collections.emptySet();
    }

    @Override
    public synchronized void handleFailure(KUID nodeId, SocketAddress address) {
        bucket.remove(nodeId);
    }

    @Override
    public synchronized boolean isLocalNode(Contact node) {
        return localNode.equals(node);
    }

    @Override
    public synchronized void purge(long elapsedTimeSinceLastContact) {
    }

    @Override
    public synchronized void purge(PurgeMode first, PurgeMode... rest) {
    }
    
    @Override
    public synchronized Collection<Contact> select(KUID nodeId, int count, SelectMode mode) {
        Collection<Contact> nodes = bucket.select(nodeId, count);
        if (nodes.size() >= count || mode == SelectMode.ALIVE) {
            return nodes;
        }
        
        nodes = new ArrayList<Contact>(nodes);
        nodes.add(localNode);
        return nodes;
    }

    @Override
    public synchronized Contact select(KUID nodeId) {
        Contact c = bucket.select(nodeId);
        if (c != null) {
            return c;
        }
        return localNode;
    }

    @Override
    public synchronized int size() {
        return bucket.size() + 1;
    }
    
    @Override
    public synchronized String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getLocalNode()).append("\n");
        buffer.append(bucket);
        return buffer.toString();
    }
    
    private static class BucketImpl implements Bucket {
        
        private static final long serialVersionUID = 7625655390844705296L;
        
        private final RouteTable routeTable;
        
        private final int k;
        
        private final Map<KUID, Contact> map;
        
        private final ClassfulNetworkCounter counter;
        
        public BucketImpl(RouteTable routeTable, int k) {
            this.routeTable = routeTable;
            this.k = k;
            
            counter = new ClassfulNetworkCounter(this);
            
            map = new FixedSizeHashMap<KUID, Contact>(k, 0.75f, true, k) {
                private static final long serialVersionUID = 4026436727356877846L;

                @Override
                protected boolean removeEldestEntry(Entry<KUID, Contact> eldest) {
                    if (super.removeEldestEntry(eldest)) {
                        getClassfulNetworkCounter().decrementAndGet(eldest.getValue());
                        return true;
                    }
                    return false;
                }
            };
        }
        
        @Override
        public ClassfulNetworkCounter getClassfulNetworkCounter() {
            return counter;
        }

        @Override
        public boolean isLocalNode(Contact node) {
            return routeTable.isLocalNode(node);
        }
        
        @Override
        public void addActiveContact(Contact node) {
            updateContact(node);
        }

        @Override
        public Contact updateContact(Contact node) {
            Contact existing = map.remove(node.getNodeID());
            if (existing != null) {
                getClassfulNetworkCounter().decrementAndGet(existing);
            }
            
            Contact previous = map.put(node.getNodeID(), node);
            assert (previous == null);
            getClassfulNetworkCounter().incrementAndGet(node);
            
            return existing;
        }
        
        @Override
        public Contact addCachedContact(Contact node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public boolean contains(KUID nodeId) {
            return map.containsKey(nodeId);
        }

        @Override
        public boolean containsActiveContact(KUID nodeId) {
            return contains(nodeId);
        }

        @Override
        public boolean containsCachedContact(KUID nodeId) {
            return false;
        }

        @Override
        public Contact get(KUID nodeId) {
            return map.get(nodeId);
        }

        @Override
        public Contact getActiveContact(KUID nodeId) {
            return get(nodeId);
        }

        @Override
        public Collection<Contact> getActiveContacts() {
            return new ArrayList<Contact>(map.values());
        }

        @Override
        public Collection<Contact> getCachedContacts() {
            return Collections.emptySet();
        }

        @Override
        public int getActiveSize() {
            return map.size();
        }
        
        @Override
        public int getCacheSize() {
            return getCachedContacts().size();
        }
        
        @Override
        public KUID getBucketID() {
            return KUID.MINIMUM;
        }

        @Override
        public Contact getCachedContact(KUID nodeId) {
            return null;
        }
        
        @Override
        public int getDepth() {
            return 0;
        }

        @Override
        public Contact getLeastRecentlySeenActiveContact() {
            Contact lrs = null;
            for (Contact c : map.values()) {
                lrs = c;
            }
            return lrs;
        }

        @Override
        public Contact getLeastRecentlySeenCachedContact() {
            return null;
        }

        @Override
        public Contact getMostRecentlySeenActiveContact() {
            for (Contact c : map.values()) {
                return c;
            }
            return null;
        }

        @Override
        public Contact getMostRecentlySeenCachedContact() {
            return null;
        }

        @Override
        public long getTimeStamp() {
            return Long.MAX_VALUE;
        }

        @Override
        public boolean isActiveFull() {
            return false;
        }

        @Override
        public int getMaxActiveSize() {
            return k;
        }

        @Override
        public boolean isCacheFull() {
            return false;
        }

        @Override
        public boolean isRefreshRequired() {
            return false;
        }

        @Override
        public boolean isTooDeep() {
            return false;
        }

        @Override
        public void purge() {
        }

        @Override
        public boolean remove(KUID nodeId) {
            Contact node = map.remove(nodeId);
            if (node != null) {
                getClassfulNetworkCounter().decrementAndGet(node);
                return true;
            }
            return false;
        }

        @Override
        public boolean removeActiveContact(KUID nodeId) {
            return remove(nodeId);
        }

        @Override
        public boolean removeCachedContact(KUID nodeId) {
            return false;
        }

        @Override
        public Collection<Contact> select(KUID nodeId, int count) {
            Contact[] nodes = new Contact[Math.min(count, map.size())];
            int index = 0;
            for (Contact c : map.values()) {
                if (index >= nodes.length) {
                    break;
                }
                
                nodes[index++] = c;
            }
            return Arrays.asList(nodes);
        }

        @Override
        public Contact select(KUID nodeId) {
            for (Contact c : map.values()) {
                return c;
            }
            return null;
        }

        @Override
        public int size() {
            return getActiveSize() + getCacheSize();
        }

        @Override
        public List<Bucket> split() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void touch() {
        }

        @Override
        public String toString() {
            return CollectionUtils.toString(map.values());
        }

        @Override
        public boolean isInSmallestSubtree() {
            return false;
        }
    }
}

package org.limewire.mojito2;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.NodeEntity;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.MessageDispatcher;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.LocalContact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.settings.BootstrapSettings;
import org.limewire.mojito2.settings.LookupSettings;
import org.limewire.mojito2.settings.NetworkSettings;
import org.limewire.mojito2.settings.StoreSettings;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueFactoryManager;
import org.limewire.mojito2.storage.Database;
import org.limewire.mojito2.storage.Storable;
import org.limewire.mojito2.storage.StorableModelManager;
import org.limewire.mojito2.util.ContactUtils;
import org.limewire.mojito2.util.HostFilter;
import org.limewire.util.ExceptionUtils;

public class DefaultMojitoDHT implements MojitoDHT {

    private final DHT dht;
    
    public DefaultMojitoDHT(DHT dht) {
        this.dht = dht;
    }
    
    @Override
    public Context getContext() {
        return (Context)dht;
    }
    
    @Override
    public Vendor getVendor() {
        return getLocalNode().getVendor();
    }

    @Override
    public Version getVersion() {
        return getLocalNode().getVersion();
    }

    @Override
    public void bind(Transport transport) throws IOException {
        dht.bind(transport);
    }

    @Override
    public DHTFuture<BootstrapEntity> bootstrap(Contact dst, 
            long timeout, TimeUnit unit) {
        return dht.bootstrap(dst, timeout, unit);
    }
    
    @Override
    public DHTFuture<BootstrapEntity> bootstrap(SocketAddress dst, 
            long timeout, TimeUnit unit) {
        return dht.bootstrap(dst, timeout, unit);
    }

    @Override
    public DHTFuture<ValueEntity> get(KUID lookupId, long timeout, TimeUnit unit) {
        return dht.get(lookupId, timeout, unit);
    }

    @Override
    public DHTFuture<ValueEntity> get(EntityKey key, long timeout, TimeUnit unit) {
        return dht.get(key, timeout, unit);
    }

    @Override
    public Database getDatabase() {
        return dht.getDatabase();
    }

    @Override
    public HostFilter getHostFilter() {
        return dht.getHostFilter();
    }

    @Override
    public LocalContact getLocalNode() {
        return (LocalContact)dht.getLocalNode();
    }

    @Override
    public MessageDispatcher getMessageDispatcher() {
        return dht.getMessageDispatcher();
    }

    @Override
    public MessageFactory getMessageFactory() {
        return dht.getMessageFactory();
    }

    @Override
    public String getName() {
        return dht.getName();
    }

    @Override
    public RouteTable getRouteTable() {
        return dht.getRouteTable();
    }

    @Override
    public DHTValueFactoryManager getDHTValueFactoryManager() {
        return dht.getDHTValueFactoryManager();
    }
    
    @Override
    public StorableModelManager getStorableModelManager() {
        return dht.getStorableModelManager();
    }

    @Override
    public boolean isBound() {
        return dht.isBound();
    }

    @Override
    public boolean isFirewalled() {
        return dht.isFirewalled();
    }
    
    @Override
    public boolean isReady() {
        return dht.isReady();
    }

    @Override
    public boolean isBooting() {
        return dht.isBooting();
    }

    @Override
    public KUID getLocalNodeID() {
        return getLocalNode().getNodeID();
    }
    
    @Override
    public SocketAddress getContactAddress() {
        return getLocalNode().getContactAddress();
    }
    
    @Override
    public void setContactId(KUID contactId) {
        getLocalNode().setNodeID(contactId);
    }
    
    @Override
    public void setContactAddress(SocketAddress address) {
        getLocalNode().setContactAddress(address);
    }
    
    @Override
    public void setExternalAddress(SocketAddress address) {
        getLocalNode().setExternalAddress(address);
    }
    
    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            long timeout, TimeUnit unit) {
        return dht.lookup(lookupId, timeout, unit);
    }

    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            Contact[] dst, long timeout, TimeUnit unit) {
        return dht.lookup(lookupId, dst, timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(String address, int port, 
            long timeout, TimeUnit unit) {
        return dht.ping(address, port, timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(InetAddress address, 
            int port, long timeout, TimeUnit unit) {
        return dht.ping(address, port, timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(SocketAddress dst, 
            long timeout, TimeUnit unit) {
        return dht.ping(dst, timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(Contact dst, 
            long timeout, TimeUnit unit) {
        return dht.ping(dst, timeout, unit);
    }

    @Override
    public DHTFuture<StoreEntity> put(Storable storable, 
            long timeout, TimeUnit unit) {
        return dht.put(storable, timeout, unit);
    }

    @Override
    public DHTFuture<StoreEntity> put(DHTValueEntity value, 
            long timeout, TimeUnit unit) {
        return dht.put(value, timeout, unit);
    }
    
    private Contact[] getActiveContacts() {
        Set<Contact> nodes = new LinkedHashSet<Contact>();
        Collection<Contact> active = getRouteTable().getActiveContacts();
        active = ContactUtils.sort(active);
        nodes.addAll(active);
        nodes.remove(getLocalNode());
        return nodes.toArray(new Contact[0]);
    }
    
    @Override
    public DHTFuture<PingEntity> findActiveContact() {
        Contact localhost = getLocalNode();
        Contact[] dst = getActiveContacts();
        
        long timeout = NetworkSettings.DEFAULT_TIMEOUT.getValue() * dst.length;
        return ping(localhost, dst, timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setHostFilter(HostFilter hostFilter) {
        dht.setHostFilter(hostFilter);
    }

    @Override
    public BigInteger size() {
        return dht.size();
    }

    @Override
    public <T> DHTFuture<T> submit(AsyncProcess<T> process, 
            long timeout, TimeUnit unit) {
        return dht.submit(process, timeout, unit);
    }

    @Override
    public Transport unbind() {
        return dht.unbind();
    }

    @Override
    public void close() throws IOException {
        dht.close();
    }
    
    @Override
    public DHTFuture<PingEntity> ping(Contact src, Contact[] dst, 
            long timeout, TimeUnit unit) {
        return dht.ping(src, dst, timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(String address, int port) {
        return ping(new InetSocketAddress(address, port));
    }
    
    @Override
    public DHTFuture<PingEntity> ping(InetAddress address, int port) {
        return ping(new InetSocketAddress(address, port));
    }
    
    @Override
    public DHTFuture<PingEntity> ping(SocketAddress addr) {
        long timeout = NetworkSettings.DEFAULT_TIMEOUT.getValue();
        return ping(addr, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<PingEntity> collisionPing(Contact dst) {
        
        Contact src = ContactUtils.createCollisionPingSender(
                dht.getLocalNode());
        
        long timeout = NetworkSettings.DEFAULT_TIMEOUT.getValue();
        return ping(src, new Contact[] { dst }, 
                timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<BootstrapEntity> bootstrap(String address, int port) {
        return bootstrap(new InetSocketAddress(address, port));
    }
    
    @Override
    public DHTFuture<BootstrapEntity> bootstrap(InetAddress address, int port) {
        return bootstrap(new InetSocketAddress(address, port));
    }
    
    @Override
    public DHTFuture<BootstrapEntity> bootstrap(SocketAddress addr) {
        long timeout = BootstrapSettings.BOOTSTRAP_TIMEOUT.getValue();
        return bootstrap(addr, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<BootstrapEntity> bootstrap(Contact contact) {
        long timeout = BootstrapSettings.BOOTSTRAP_TIMEOUT.getValue();
        return bootstrap(contact, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<StoreEntity> store(Storable storable) {
        long timeout = StoreSettings.STORE_TIMEOUT.getValue();
        return dht.put(storable, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value) {
        DHTValueEntity entity = DHTValueEntity.createFromValue(dht, key, value);
        long timeout = StoreSettings.STORE_TIMEOUT.getValue();
        return dht.put(entity, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<StoreEntity> remove(KUID key) {
        return put(key, DHTValue.EMPTY_VALUE);
    }
    
    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId) {
        long timeout = LookupSettings.FIND_NODE_LOOKUP_TIMEOUT.getValue();
        return dht.lookup(lookupId, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<ValueEntity> get(EntityKey key) {
        long timeout = LookupSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue();
        return dht.get(key, timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public DHTFuture<ValueEntity[]> getAll(EntityKey key) {
        final Object lock = new Object();
        
        synchronized (lock) {
            
            final List<DHTFuture<ValueEntity>> futures 
                = new ArrayList<DHTFuture<ValueEntity>>();
            
            long timeout = LookupSettings.FIND_VALUE_LOOKUP_TIMEOUT.getValue();
            AsyncProcess<ValueEntity[]> process = NopProcess.process();
            final DHTFuture<ValueEntity[]> lookup = submit(process, 
                    timeout, TimeUnit.MILLISECONDS);
            
            lookup.addFutureListener(new EventListener<FutureEvent<ValueEntity[]>>() {
                @Override
                public void handleEvent(FutureEvent<ValueEntity[]> event) {
                    synchronized (lock) {
                        for (DHTFuture<?> future : futures) {
                            future.cancel(true);
                        }
                    }
                }
            });
            
            final List<EntityKey> keys 
                = new ArrayList<EntityKey>();
            
            final List<ValueEntity> entities 
                = new ArrayList<ValueEntity>();
            
            final AtomicBoolean first = new AtomicBoolean(true);
            
            DHTFuture<ValueEntity> future = get(key);
            futures.add(future);
            
            future.addFutureListener(new EventListener<FutureEvent<ValueEntity>>() {
                @Override
                public void handleEvent(FutureEvent<ValueEntity> event) {
                    synchronized (lock) {
                        try {
                            if (!lookup.isDone()) {
                                switch (event.getType()) {
                                    case SUCCESS:
                                        onSuccess(event.getResult());
                                        break;
                                    case EXCEPTION:
                                        onException(event.getException());
                                        break;
                                    case CANCELLED:
                                        onCancellation();
                                        break;
                                }
                            }
                        } catch (Throwable t) {
                            onException(t);
                            ExceptionUtils.reportIfUnchecked(t);
                        }
                    }
                }
                
                private void onSuccess(ValueEntity entity) {
                    entities.add(entity);
                    
                    if (first.getAndSet(false)) {
                        EntityKey[] more 
                            = entity.getEntityKeys();
                        keys.addAll(Arrays.asList(more));
                    }
                    
                    doNext();
                }
                
                private void onException(Throwable t) {
                    if (keys.isEmpty() && entities.isEmpty()) {
                        lookup.setException(t);
                    } else {
                        doNext();
                    }
                }
                
                private void onCancellation() {
                    lookup.cancel(true);
                }
                
                private void doNext() {
                    if (keys.isEmpty()) {
                        ValueEntity[] values = entities.toArray(
                                new ValueEntity[0]);
                        lookup.setValue(values);
                        return;
                    }
                    
                    DHTFuture<ValueEntity> future 
                        = get(keys.remove(0));
                    futures.add(future);
                    future.addFutureListener(this);
                }
            });
            
            return lookup;
        }
    }
    
    private static class NopProcess implements AsyncProcess<Object> {

        private static final NopProcess NOP = new NopProcess();
        
        @SuppressWarnings("unchecked")
        public static <V> AsyncProcess<V> process() {
            return (AsyncProcess<V>)NOP;
        }
        
        @Override
        public void start(DHTFuture<Object> future) {
        }

        @Override
        public void stop(DHTFuture<Object> future) {
        }
    }
}

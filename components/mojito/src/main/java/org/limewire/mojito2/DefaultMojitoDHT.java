package org.limewire.mojito2;

import java.io.IOException;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.NopAsyncProcess;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.NodeEntity;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.entity.SecurityTokenEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.MessageDispatcher;
import org.limewire.mojito2.io.SecurityTokenResponseHandler;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.message.RequestMessage;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.LocalContact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.Vendor;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.routing.RouteTable.SelectMode;
import org.limewire.mojito2.settings.BootstrapSettings;
import org.limewire.mojito2.settings.ContextSettings;
import org.limewire.mojito2.settings.KademliaSettings;
import org.limewire.mojito2.settings.LookupSettings;
import org.limewire.mojito2.settings.NetworkSettings;
import org.limewire.mojito2.settings.StoreSettings;
import org.limewire.mojito2.storage.DHTValue;
import org.limewire.mojito2.storage.Database;
import org.limewire.mojito2.util.ContactUtils;
import org.limewire.util.ExceptionUtils;

/**
 * 
 */
public class DefaultMojitoDHT extends DefaultDHT implements MojitoDHT {
    
    private static final Log LOG 
        = LogFactory.getLog(DefaultMojitoDHT.class);
    
    public DefaultMojitoDHT(String name, 
            MessageFactory messageFactory, 
            RouteTable routeTable,
            Database database) {
        super(name, messageFactory, routeTable, database);
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
    public KUID getLocalNodeID() {
        return getLocalNode().getNodeID();
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
    public boolean isLocalNode(Contact contact) {
        return getLocalNode().equals(contact);
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
        
        long timeout = NetworkSettings.DEFAULT_TIMEOUT.getTimeInMillis() * dst.length;
        return ping(localhost, dst, timeout, TimeUnit.MILLISECONDS);
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
        long timeout = NetworkSettings.DEFAULT_TIMEOUT.getTimeInMillis();
        return ping(addr, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<PingEntity> ping(Contact src, Contact[] dst) {
        long timeout = NetworkSettings.DEFAULT_TIMEOUT.getTimeInMillis();
        return ping(src, dst, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<PingEntity> collisionPing(Contact dst) {
        
        Contact src = ContactUtils.createCollisionPingSender(getLocalNode());
        
        long timeout = NetworkSettings.DEFAULT_TIMEOUT.getTimeInMillis();
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
        long timeout = BootstrapSettings.BOOTSTRAP_TIMEOUT.getTimeInMillis();
        return bootstrap(addr, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<BootstrapEntity> bootstrap(Contact contact) {
        long timeout = BootstrapSettings.BOOTSTRAP_TIMEOUT.getTimeInMillis();
        return bootstrap(contact, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<StoreEntity> put(KUID key, DHTValue value) {
        long timeout = StoreSettings.STORE_TIMEOUT.getTimeInMillis();
        return put(key, value, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<StoreEntity> remove(KUID key) {
        return put(key, DHTValue.EMPTY_VALUE);
    }
    
    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId) {
        long timeout = LookupSettings.FIND_NODE_LOOKUP_TIMEOUT.getTimeInMillis();
        return lookup(lookupId, timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public DHTFuture<ValueEntity> get(EntityKey key) {
        long timeout = LookupSettings.FIND_VALUE_LOOKUP_TIMEOUT.getTimeInMillis();
        return get(key, timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public DHTFuture<ValueEntity[]> getAll(EntityKey key) {
        final Object lock = new Object();
        
        synchronized (lock) {
            
            final List<DHTFuture<ValueEntity>> futures 
                = new ArrayList<DHTFuture<ValueEntity>>();
            
            long timeout = LookupSettings.FIND_VALUE_LOOKUP_TIMEOUT.getTimeInMillis();
            AsyncProcess<ValueEntity[]> process = NopAsyncProcess.process();
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

    @Override
    public DHTFuture<SecurityTokenEntity> getSecurityToken(Contact dst, 
            long timeout, TimeUnit unit) {
        
        KUID lookupId = KUID.createRandomID();
        AsyncProcess<SecurityTokenEntity> process 
            = new SecurityTokenResponseHandler(this, 
                    dst, lookupId, timeout, unit);
        
        return submit(process, timeout, unit);
    }
    
    @Override
    public void close() {
        if (isBound()) {
            try {
                shutdown(0, TimeUnit.MILLISECONDS);
            } catch (InterruptedException err) {
                LOG.error("InterruptedException", err);
            }
        }
        
        super.close();
    }
    
    @Override
    public Transport unbind() {
        if (isBound()) {
            try {
                shutdown(0, TimeUnit.MILLISECONDS);
            } catch (InterruptedException err) {
                LOG.error("InterruptedException", err);
            }
        }
        
        return super.unbind();
    }
    
    private boolean shutdown(long timeout, TimeUnit unit) 
            throws InterruptedException {
        if (isFirewalled() 
                || !ContextSettings.SEND_SHUTDOWN_MESSAGE.getValue()) {
            return false;
        }
        
        MessageFactory messageFactory = getMessageFactory();
        
        // Shutdown the local Node
        Contact localhost = getLocalNode();
        localhost.shutdown(true);
        
        Contact shutdown = new LocalContact(
                localhost.getVendor(), 
                localhost.getVersion(),
                localhost.getNodeID(), 
                localhost.getInstanceID(), 
                Contact.SHUTDOWN_FLAG);
        
        
        // We're nice guys and send shutdown messages to the 2*k-closest
        // Nodes which should help to reduce the overall latency.
        int m = ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.getValue();
        int count = m*KademliaSettings.K;
        
        RouteTable routeTable = getRouteTable();
        Collection<Contact> contacts = routeTable.select(
                localhost.getNodeID(), count, SelectMode.ALIVE);
        
        MessageDispatcher messageDispatcher = getMessageDispatcher();
        
        for (Contact contact : contacts) {
            if (!contact.equals(localhost)) {
                // We are not interested in the responses as we're going
                // to shutdown. Send pings without a response handler.
                RequestMessage request = messageFactory.createPingRequest(
                        shutdown, contact.getContactAddress());
                
                try {
                    messageDispatcher.send(null, contact, request, 
                            -1L, TimeUnit.MILLISECONDS);
                } catch (IOException err) {
                    LOG.error("IOException", err);
                }
            }
        }
        
        return true;
    }
}

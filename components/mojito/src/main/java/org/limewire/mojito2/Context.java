package org.limewire.mojito2;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.listener.EventListener;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueFactoryManager;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.Storable;
import org.limewire.mojito.db.StorableModelManager;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.manager.BootstrapConfig;
import org.limewire.mojito.manager.BootstrapManager2;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.util.DHTSizeEstimator;
import org.limewire.mojito.util.HostFilter;
import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.io.DefaultMessageDispatcher;
import org.limewire.mojito2.io.DefaultStoreForward;
import org.limewire.mojito2.io.MessageDispatcher;
import org.limewire.mojito2.io.NodeResponseHandler;
import org.limewire.mojito2.io.PingResponseHandler;
import org.limewire.mojito2.io.StoreForward;
import org.limewire.mojito2.io.StoreResponseHandler;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.io.ValueResponseHandler;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.message.MessageHelper;
import org.limewire.mojito2.util.ContactUtils;
import org.limewire.util.ExceptionUtils;

/**
 * 
 */
public class Context implements DHT {
    
    /**
     * 
     */
    private final DHTFutureAdapter<PingEntity> onPong 
            = new DHTFutureAdapter<PingEntity>() {
        @Override
        protected void operationComplete(FutureEvent<PingEntity> event) {
            if (event.getType() == Type.SUCCESS) {
                update(event.getResult());
            }
        }
    };
    
    /**
     * 
     */
    private final DHTSizeEstimator estimator = new DHTSizeEstimator();
    
    /**
     * 
     */
    private final FutureManager futureManager = new FutureManager();
    
    /**
     * 
     */
    private final DHTValueFactoryManager factoryManager 
        = new DHTValueFactoryManager();
    
    /**
     * 
     */
    private final String name;
    
    /**
     * 
     */
    private final MessageDispatcher messageDispatcher;
    
    /**
     * 
     */
    private final RouteTable routeTable;
    
    /**
     * 
     */
    private final Database database;
    
    /**
     * 
     */
    private final StorableModelManager modelManager 
        = new StorableModelManager();
    
    /**
     * 
     */
    private final MessageHelper messageHelper;
    
    /**
     * 
     */
    private volatile HostFilter hostFilter = null;
    
    /**
     * 
     */
    public Context(String name,
            Transport transport,
            MessageFactory messageFactory,
            RouteTable routeTable, 
            Database database) {
        
        this.name = name;
        this.routeTable = routeTable;
        this.database = database;
        
        this.messageHelper = new MessageHelper(this, messageFactory);
        
        StoreForward storeForward 
            = new DefaultStoreForward(routeTable, database);
        
        this.messageDispatcher = new DefaultMessageDispatcher(
                this, transport, storeForward);
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void close() {
        futureManager.close();
        estimator.clear();
    }
    
    @Override
    public void start() {
        messageDispatcher.bind();
    }

    @Override
    public void stop() {
        messageDispatcher.unbind();
    }

    @Override
    public BigInteger size() {
        return estimator.getEstimatedSize(routeTable);
    }
    
    @Override
    public MessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }
    
    @Override
    public RouteTable getRouteTable() {
        return routeTable;
    }
    
    @Override
    public Database getDatabase() {
        return database;
    }
    
    @Override
    public MessageFactory getMessageFactory() {
        return messageHelper.getMessageFactory();
    }
    
    /**
     * 
     */
    public MessageHelper getMessageHelper() {
        return messageHelper;
    }
    
    /**
     * 
     */
    public DHTValueFactoryManager getDHTValueFactoryManager() {
        return factoryManager;
    }
    
    @Override
    public StorableModelManager getStorableModelManager() {
        return modelManager;
    }
    
    @Override
    public LocalContact getLocalNode() {
        return (LocalContact)routeTable.getLocalNode();
    }
    
    /**
     * 
     */
    public SocketAddress getContactAddress() {
        return getLocalNode().getContactAddress();
    }
    
    /**
     * 
     */
    public int getExternalPort() {
        return getLocalNode().getExternalPort();
    }
    
    /**
     * 
     */
    public KUID getLocalNodeID() {
        return getLocalNode().getNodeID();
    }
    
    /**
     * 
     */
    public boolean isLocalNodeID(KUID contactId) {
        return getLocalNodeID().equals(contactId);
    }
    
    /**
     * 
     */
    public boolean isLocalNode(Contact contact) {
        return getLocalNode().equals(contact);
    }
    
    /**
     * 
     */
    public boolean isLocalContactAddress(SocketAddress address) {
        return getContactAddress().equals(address);
    }
    
    /**
     * 
     */
    public boolean isBootstrapping() {
        return false;
    }
    
    @Override
    public boolean isFirewalled() {
        return getLocalNode().isFirewalled();
    }
    
    @Override
    public HostFilter getHostFilter() {
        return hostFilter;
    }
    
    @Override
    public void setHostFilter(HostFilter hostFilter) {
        this.hostFilter = hostFilter;
    }
    
    @Override
    public DHTFuture<BootstrapEntity> bootstrap(Contact dst, 
            long timeout, TimeUnit unit) {
        
        BootstrapConfig config = new BootstrapConfig();
        AsyncProcess<BootstrapEntity> process 
            = new BootstrapManager2(this, dst, config);
        return futureManager.submit(process, timeout, unit);
    }
    
    @Override
    public DHTFuture<PingEntity> ping(InetAddress address, int port, 
            long timeout, TimeUnit unit) {
        return ping(new InetSocketAddress(address, port), timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(String address, int port, 
            long timeout, TimeUnit unit) {
        return ping(new InetSocketAddress(address, port), timeout, unit);
    }
    
    @Override
    public DHTFuture<PingEntity> ping(SocketAddress dst, 
            long timeout, TimeUnit unit) {
        AsyncProcess<PingEntity> process = new PingResponseHandler(
                this, dst, timeout, unit);
        
        DHTFuture<PingEntity> future 
            = futureManager.submit(process, timeout, unit);
        future.addFutureListener(onPong);
        return future;
    }
    
    @Override
    public DHTFuture<PingEntity> ping(Contact dst, long timeout, TimeUnit unit) {
        AsyncProcess<PingEntity> process = new PingResponseHandler(
                this, dst, timeout, unit);
        
        DHTFuture<PingEntity> future 
            = futureManager.submit(process, timeout, unit);
        future.addFutureListener(onPong);
        return future;
    }
    
    public DHTFuture<PingEntity> collisionPing(Contact[] dst, 
            long timeout, TimeUnit unit) {
        
        Contact src = ContactUtils.createCollisionPingSender(getLocalNode());
        
        AsyncProcess<PingEntity> process = new PingResponseHandler(
                this, src, dst, timeout, unit);
        
        return futureManager.submit(process, timeout, unit);
    }

    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            long timeout, TimeUnit unit) {
        AsyncProcess<NodeEntity> process = new NodeResponseHandler(
                this, lookupId, timeout, unit);
        
        return futureManager.submit(process, timeout, unit);
    }
    
    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            Contact[] contacts, long timeout, TimeUnit unit) {
        AsyncProcess<NodeEntity> process = new NodeResponseHandler(
                this, lookupId, contacts, timeout, unit);
        
        return futureManager.submit(process, timeout, unit);
    }
    
    @Override
    public DHTFuture<ValueEntity> get(KUID lookupId, 
            long timeout, TimeUnit unit) {
        
        EntityKey key = EntityKey.createEntityKey(
                lookupId, DHTValueType.ANY);
        
        return get(key, timeout, unit);
    }
    
    @Override
    public DHTFuture<ValueEntity> get(EntityKey key, 
            long timeout, TimeUnit unit) {
        
        AsyncProcess<ValueEntity> process = new ValueResponseHandler(
                this, key, timeout, unit);
        
        return futureManager.submit(process, timeout, unit);
    }
    
    @Override
    public DHTFuture<StoreEntity> put(Storable storable, 
            long timeout, TimeUnit unit) {
        
        DHTValueEntity value = DHTValueEntity.createFromStorable(
                this, storable);
        
        return put(value, timeout, unit);
    }
    
    @Override
    public DHTFuture<StoreEntity> put(DHTValueEntity value, 
            long timeout, TimeUnit unit) {
        
        // The store operation is a composition of two DHT operations.
        // In the first step we're doing a FIND_NODE lookup to find
        // the K-closest nodes to a given key. In the second step we're
        // performing a STORE operation on the K-closest nodes we found.
        
        final Object lock = new Object();
        
        synchronized (lock) {
            
            // The DHTFuture for the STORE operation. We initialize it 
            // at the very end of this block of code.
            final AtomicReference<DHTFuture<StoreEntity>> futureRef 
                = new AtomicReference<DHTFuture<StoreEntity>>();
            
            final StoreResponseHandler process 
                = new StoreResponseHandler(
                    this, new DHTValueEntity[] { value }, timeout, unit);
            
            KUID lookupId = value.getPrimaryKey();
            final DHTFuture<NodeEntity> lookup 
                = lookup(lookupId, timeout, unit);
            
            lookup.addFutureListener(new EventListener<FutureEvent<NodeEntity>>() {
                @Override
                public void handleEvent(FutureEvent<NodeEntity> event) {
                    synchronized (lock) {
                        try {
                            // The reference can be null if the FutureManager was
                            // unable to execute the STORE process. This can happen
                            // if the Context is being shutdown.
                            if (futureRef.get() != null) {
                                switch (event.getType()) {
                                    case SUCCESS:
                                        handleNodeEntity(event.getResult());
                                        break;
                                    case EXCEPTION:
                                        handleException(event.getException());
                                        break;
                                    default:
                                        handleCancellation();
                                        break;
                                }
                            }
                        } catch (Throwable t) {
                            handleException(t);
                            ExceptionUtils.reportIfUnchecked(t);
                        }
                    }
                }
                
                private void handleNodeEntity(NodeEntity entity) 
                        throws IOException {
                    process.store(entity.getContacts());
                }
                
                private void handleException(Throwable t) {
                    futureRef.get().setException(t);
                }
                
                private void handleCancellation() {
                    futureRef.get().cancel(true);
                }
            });
            
            DHTFuture<StoreEntity> store 
                = futureManager.submit(process, timeout, unit);
            futureRef.set(store);
            
            store.addFutureListener(new EventListener<FutureEvent<StoreEntity>>() {
                @Override
                public void handleEvent(FutureEvent<StoreEntity> event) {
                    lookup.cancel(true);
                }
            });
            
            return store;
        }
    }
    
    @Override
    public <T> DHTFuture<T> submit(AsyncProcess<T> process, 
            long timeout, TimeUnit unit) {
        return futureManager.submit(process, timeout, unit);
    }

    /**
     * 
     */
    private void update(PingEntity entity) {
        SocketAddress externalAddress 
            = entity.getExternalAddress();
        BigInteger estimatedSize 
            = entity.getEstimatedSize();
        
        LocalContact localhost = (LocalContact)routeTable.getLocalNode();
        boolean changed = localhost.setExternalAddress(externalAddress);
        
        if (changed) {
            modelManager.handleContactChange(this);
        }
        
        estimator.addEstimatedRemoteSize(estimatedSize);
    }
}

package org.limewire.mojito2;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.NodeEntity;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
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
import org.limewire.mojito2.routing.BucketRefresher;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.LocalContact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.settings.BucketRefresherSettings;
import org.limewire.mojito2.settings.DatabaseSettings;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.Database;
import org.limewire.mojito2.storage.DatabaseCleaner;
import org.limewire.mojito2.util.DHTSizeEstimator;
import org.limewire.mojito2.util.HostFilter;
import org.limewire.util.ExceptionUtils;

/**
 * 
 */
public class DefaultDHT extends AbstractDHT implements Context {
    
    /**
     * 
     */
    private final EventListener<FutureEvent<PingEntity>> onPong 
            = new EventListener<FutureEvent<PingEntity>>() {
        @Override
        public void handleEvent(FutureEvent<PingEntity> event) {
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
    private final BucketRefresher bucketRefresher 
        = new BucketRefresher(this, 
                BucketRefresherSettings.BUCKET_REFRESHER_DELAY.getTimeInMillis(), 
                TimeUnit.MILLISECONDS);
    
    /**
     * 
     */
    private final DatabaseCleaner databaseCleaner;
    
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
    private final MessageHelper messageHelper;
    
    /**
     * 
     */
    private volatile HostFilter hostFilter = null;
    
    /**
     * 
     */
    public DefaultDHT(String name,
            MessageFactory messageFactory,
            RouteTable routeTable, 
            Database database) {
        
        this.name = name;
        this.routeTable = routeTable;
        this.database = database;
        
        this.databaseCleaner = new DatabaseCleaner(
                routeTable, database, 
                DatabaseSettings.DATABASE_CLEANER_PERIOD.getTimeInMillis(), 
                TimeUnit.MILLISECONDS);
        
        this.messageHelper = new MessageHelper(this, messageFactory);
        
        StoreForward storeForward 
            = new DefaultStoreForward(routeTable, database);
        
        this.messageDispatcher = new DefaultMessageDispatcher(
                this, messageFactory, storeForward);
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void close() {
        super.close();
        
        estimator.clear();
        
        databaseCleaner.close();
        bucketRefresher.close();
        
        messageDispatcher.close();
    }
    
    @Override
    public void bind(Transport transport) throws IOException {
        routeTable.bind(this);
        messageDispatcher.bind(transport);
        
        databaseCleaner.start();
    }

    @Override
    public Transport unbind() {
        databaseCleaner.stop();
        bucketRefresher.stop();
        
        return messageDispatcher.unbind();
    }
    
    @Override
    public boolean isBound() {
        return messageDispatcher.isBound();
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
    
    @Override
    public MessageHelper getMessageHelper() {
        return messageHelper;
    }
    
    @Override
    public LocalContact getLocalNode() {
        return (LocalContact)routeTable.getLocalNode();
    }
    
    @Override
    public SocketAddress getContactAddress() {
        return getLocalNode().getContactAddress();
    }
    
    @Override
    public int getExternalPort() {
        return getLocalNode().getExternalPort();
    }
    
    @Override
    public KUID getLocalNodeID() {
        return getLocalNode().getNodeID();
    }
    
    @Override
    public boolean isLocalNodeID(KUID contactId) {
        return getLocalNodeID().equals(contactId);
    }
    
    @Override
    public boolean isLocalNode(Contact contact) {
        return getLocalNode().equals(contact);
    }
    
    @Override
    public boolean isLocalContactAddress(SocketAddress address) {
        return getContactAddress().equals(address);
    }
    
    /**
     * Returns the {@link BucketRefresher}
     */
    public BucketRefresher getBucketRefresher() {
        return bucketRefresher;
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
    protected void bootstrapped(BootstrapEntity entity) {
        // We start the BucketRefresher as soon as our
        // not is done with bootstrapping.
        bucketRefresher.start();
    }
    
    @Override
    public DHTFuture<PingEntity> ping(SocketAddress dst, 
            long timeout, TimeUnit unit) {
        AsyncProcess<PingEntity> process = new PingResponseHandler(
                this, dst, timeout, unit);
        
        DHTFuture<PingEntity> future 
            = submit(process, timeout, unit);
        future.addFutureListener(onPong);
        return future;
    }
    
    @Override
    public DHTFuture<PingEntity> ping(Contact dst, long timeout, TimeUnit unit) {
        
        AsyncProcess<PingEntity> process 
            = new PingResponseHandler(
                this, dst, timeout, unit);
        
        DHTFuture<PingEntity> future 
            = submit(process, timeout, unit);
        future.addFutureListener(onPong);
        return future;
    }
    
    @Override
    public DHTFuture<PingEntity> ping(Contact src, Contact[] dst, 
            long timeout, TimeUnit unit) {
        
        AsyncProcess<PingEntity> process 
            = new PingResponseHandler(
                this, src, dst, timeout, unit);
        
        DHTFuture<PingEntity> future 
            = submit(process, timeout * dst.length, unit);
        
        if (src.equals(getLocalNode())) {
            future.addFutureListener(onPong);
        }
        
        return future;
    }

    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            long timeout, TimeUnit unit) {
        
        AsyncProcess<NodeEntity> process 
            = new NodeResponseHandler(
                this, lookupId, timeout, unit);
        
        return submit(process, timeout, unit);
    }
    
    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            Contact[] contacts, long timeout, TimeUnit unit) {
        AsyncProcess<NodeEntity> process = new NodeResponseHandler(
                this, lookupId, contacts, timeout, unit);
        
        return submit(process, timeout, unit);
    }
    
    @Override
    public DHTFuture<ValueEntity> get(EntityKey key, 
            long timeout, TimeUnit unit) {
        
        AsyncProcess<ValueEntity> process = new ValueResponseHandler(
                this, key, timeout, unit);
        
        return submit(process, timeout, unit);
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
                = new StoreResponseHandler(this, value, timeout, unit);
            
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
                            
                            DHTFuture<StoreEntity> future = futureRef.get();
                            if (future != null && !future.isDone()) {
                                switch (event.getType()) {
                                    case SUCCESS:
                                        onSuccess(event.getResult());
                                        break;
                                    case EXCEPTION:
                                        onException(event.getException());
                                        break;
                                    default:
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
                
                private void onSuccess(NodeEntity entity) 
                        throws IOException {
                    process.store(entity.getContacts());
                }
                
                private void onException(Throwable t) {
                    futureRef.get().setException(t);
                }
                
                private void onCancellation() {
                    futureRef.get().cancel(true);
                }
            });
            
            DHTFuture<StoreEntity> store 
                = submit(process, timeout, unit);
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

    /**
     * 
     */
    private void update(PingEntity entity) {
        SocketAddress externalAddress 
            = entity.getExternalAddress();
        BigInteger estimatedSize 
            = entity.getEstimatedSize();
        
        LocalContact localhost = (LocalContact)routeTable.getLocalNode();
        localhost.setExternalAddress(externalAddress);
        estimator.addEstimatedRemoteSize(estimatedSize);
    }
}

package org.limewire.mojito2;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.NodeEntity;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.entity.SecurityTokenEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.BootstrapConfig;
import org.limewire.mojito2.io.BootstrapProcess;
import org.limewire.mojito2.io.DefaultMessageDispatcher;
import org.limewire.mojito2.io.DefaultStoreForward;
import org.limewire.mojito2.io.MessageDispatcher;
import org.limewire.mojito2.io.NodeResponseHandler;
import org.limewire.mojito2.io.PingResponseHandler;
import org.limewire.mojito2.io.SecurityTokenResponseHandler;
import org.limewire.mojito2.io.StoreForward;
import org.limewire.mojito2.io.StoreResponseHandler;
import org.limewire.mojito2.io.Transport;
import org.limewire.mojito2.io.ValueResponseHandler;
import org.limewire.mojito2.message.MessageFactory;
import org.limewire.mojito2.message.MessageHelper;
import org.limewire.mojito2.message.RequestMessage;
import org.limewire.mojito2.routing.BucketRefresher;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.LocalContact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.routing.RouteTable.ContactPinger;
import org.limewire.mojito2.routing.RouteTable.SelectMode;
import org.limewire.mojito2.settings.BucketRefresherSettings;
import org.limewire.mojito2.settings.ContextSettings;
import org.limewire.mojito2.settings.DatabaseSettings;
import org.limewire.mojito2.settings.KademliaSettings;
import org.limewire.mojito2.settings.NetworkSettings;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueFactoryManager;
import org.limewire.mojito2.storage.Database;
import org.limewire.mojito2.storage.DatabaseCleaner;
import org.limewire.mojito2.storage.DatabasePublisher;
import org.limewire.mojito2.storage.StorableModelManager;
import org.limewire.mojito2.util.DHTSizeEstimator;
import org.limewire.mojito2.util.HostFilter;
import org.limewire.util.ExceptionUtils;

/**
 * 
 */
public class Context extends AbstractDHT {
    
    private static final Log LOG = LogFactory.getLog(Context.class);
    
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
    private final DHTValueFactoryManager factoryManager 
        = new DHTValueFactoryManager();
    
    /**
     * 
     */
    private final BucketRefresher bucketRefresher 
        = new BucketRefresher(this, 
                BucketRefresherSettings.BUCKET_REFRESHER_DELAY.get(), 
                TimeUnit.MILLISECONDS);
    
    /**
     * 
     */
    private final DatabasePublisher databasePublisher 
        = new DatabasePublisher(this, 
                DatabaseSettings.STORABLE_PUBLISHER_PERIOD.get(), 
                TimeUnit.MILLISECONDS);
    
    /**
     * 
     */
    private final DatabaseCleaner databaseCleaner;
    
    /**
     * 
     */
    private volatile DHTFuture<BootstrapEntity> bootstrap = null;
    
    
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
    private boolean open = true;
    
    /**
     * 
     */
    public Context(String name,
            MessageFactory messageFactory,
            RouteTable routeTable, 
            Database database) {
        
        this.name = name;
        this.routeTable = routeTable;
        this.database = database;
        
        this.databaseCleaner = new DatabaseCleaner(
                routeTable, database, 
                DatabaseSettings.DATABASE_CLEANER_PERIOD.get(), 
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
        
        synchronized (this) {
            open = false;
        }
        
        estimator.clear();
        
        if (isBound()) {
            shutdown();
        }
        
        databasePublisher.close();
        databaseCleaner.close();
        bucketRefresher.close();
        
        messageDispatcher.close();
    }
    
    @Override
    public void bind(Transport transport) throws IOException {
        ContactPinger pinger = new ContactPinger() {
            @Override
            public DHTFuture<PingEntity> ping(Contact contact) {
                long timeout = NetworkSettings.DEFAULT_TIMEOUT.getValue();
                return Context.this.ping(contact, 
                        timeout, TimeUnit.MILLISECONDS);
            }
        };
        
        routeTable.bind(pinger);
        messageDispatcher.bind(transport);
    }

    @Override
    public Transport unbind() {
        if (isBound()) {
            shutdown();
        }
        
        databasePublisher.stop();
        databaseCleaner.stop();
        bucketRefresher.stop();
        
        return messageDispatcher.unbind();
    }
    
    private void shutdown() {
        if (isFirewalled() || !ContextSettings.SEND_SHUTDOWN_MESSAGE.getValue()) {
            return;
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
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        int count = m*k;
        
        Collection<Contact> contacts = routeTable.select(
                localhost.getNodeID(), count, SelectMode.ALIVE);
        
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
    
    /**
     * 
     */
    public MessageHelper getMessageHelper() {
        return messageHelper;
    }
    
    /**
     * 
     */
    @Override
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
    public synchronized boolean isBooting() {
        DHTFuture<BootstrapEntity> bootstrap = this.bootstrap;
        return open && bootstrap != null && !bootstrap.isDone();
    }
    
    @Override
    public synchronized boolean isReady() {
        DHTFuture<BootstrapEntity> bootstrap = this.bootstrap;
        return open && bootstrap != null && bootstrap.isDone() 
                && !bootstrap.isCompletedAbnormally();
    }
    
    @Override
    protected synchronized DHTFuture<BootstrapEntity> bootstrap(
            BootstrapConfig config, long timeout, TimeUnit unit) {
        
        // There can be only one bootstrap process active!
        if (bootstrap != null) {
            bootstrap.cancel(true);
        }
        
        AsyncProcess<BootstrapEntity> process 
            = new BootstrapProcess(this, config, timeout, unit);
        
        bootstrap = submit(process, timeout, unit);
        
        bootstrap.addFutureListener(
                new EventListener<FutureEvent<BootstrapEntity>>() {
            @Override
            public void handleEvent(FutureEvent<BootstrapEntity> event) {
                if (event.getType() == Type.SUCCESS) {
                    databasePublisher.start();
                    databaseCleaner.start();
                    bucketRefresher.start();
                }
            }
        });
        
        return bootstrap;
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
        AsyncProcess<PingEntity> process = new PingResponseHandler(
                this, dst, timeout, unit);
        
        DHTFuture<PingEntity> future 
            = submit(process, timeout, unit);
        future.addFutureListener(onPong);
        return future;
    }
    
    @Override
    public DHTFuture<PingEntity> ping(Contact src, Contact[] dst, 
            long timeout, TimeUnit unit) {
        
        AsyncProcess<PingEntity> process = new PingResponseHandler(
                this, src, dst, timeout, unit);
        
        return submit(process, timeout, unit);
    }

    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            long timeout, TimeUnit unit) {
        AsyncProcess<NodeEntity> process = new NodeResponseHandler(
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
    
    public DHTFuture<SecurityTokenEntity> getSecurityToken(
            Contact src, long timeout, TimeUnit unit) {
        
        KUID lookupId = KUID.createRandomID();
        AsyncProcess<SecurityTokenEntity> process 
            = new SecurityTokenResponseHandler(this, 
                    src, lookupId, timeout, unit);
        
        return submit(process, timeout, unit);
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

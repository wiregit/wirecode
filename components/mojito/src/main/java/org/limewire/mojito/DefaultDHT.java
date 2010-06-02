package org.limewire.mojito;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.listener.EventListener;
import org.limewire.mojito.concurrent.DHTFutureProcess;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.SecurityTokenEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.io.DefaultMessageDispatcher;
import org.limewire.mojito.io.DefaultStoreForward;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.NodeResponseHandler;
import org.limewire.mojito.io.PingResponseHandler;
import org.limewire.mojito.io.SecurityTokenResponseHandler;
import org.limewire.mojito.io.StoreForward;
import org.limewire.mojito.io.Transport;
import org.limewire.mojito.io.ValueResponseHandler;
import org.limewire.mojito.message.MessageFactory;
import org.limewire.mojito.message.MessageHelper;
import org.limewire.mojito.routing.BucketRefresher;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.LocalContact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.settings.BucketRefresherSettings;
import org.limewire.mojito.settings.DatabaseSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.settings.StoreSettings;
import org.limewire.mojito.storage.Database;
import org.limewire.mojito.storage.DatabaseCleaner;
import org.limewire.mojito.storage.Value;
import org.limewire.mojito.storage.ValueTuple;
import org.limewire.mojito.util.DHTSizeEstimator;
import org.limewire.mojito.util.HostFilter;
import org.limewire.mojito.util.IoUtils;
import org.limewire.security.SecurityToken;

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
    private final DHTSizeEstimator estimator;
    
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
    private final StoreManager storeManager = new StoreManager(this, 
            StoreSettings.PARALLEL_STORES.getValue());
    
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
        
        this.estimator = new DHTSizeEstimator(routeTable);
        
        this.databaseCleaner = new DatabaseCleaner(
                routeTable, database, 
                DatabaseSettings.DATABASE_CLEANER_PERIOD.getTimeInMillis(), 
                TimeUnit.MILLISECONDS);
        
        this.messageHelper = new MessageHelper(this, messageFactory);
        
        DefaultStoreForward.StoreProvider provider 
                = new DefaultStoreForward.StoreProvider() {

            @Override
            public boolean isReady() {
                return DefaultDHT.this.isReady();
            }

            @Override
            public void store(Contact dst, SecurityToken securityToken,
                    Collection<? extends ValueTuple> values) {
                DefaultDHT.this.store(dst, securityToken, values);
            }
        };
        
        StoreForward storeForward = new DefaultStoreForward(
                routeTable, database, provider);
        
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
        storeManager.close();
        
        IoUtils.close(messageDispatcher);
    }
    
    @Override
    public void bind(Transport transport) throws IOException {
        routeTable.bind(this);
        messageDispatcher.bind(transport);
        
        databaseCleaner.start();
    }

    @Override
    public Transport unbind() {
        storeManager.clear();
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
        return estimator.getEstimatedSize();
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
        return getLocalNode().getContactId();
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
        DHTFutureProcess<PingEntity> process = new PingResponseHandler(
                this, dst, timeout, unit);
        
        DHTFuture<PingEntity> future 
            = submit(process, timeout, unit);
        future.addFutureListener(onPong);
        return future;
    }
    
    @Override
    public DHTFuture<PingEntity> ping(Contact dst, long timeout, TimeUnit unit) {
        
        DHTFutureProcess<PingEntity> process 
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
        
        DHTFutureProcess<PingEntity> process 
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
        
        DHTFutureProcess<NodeEntity> process 
            = new NodeResponseHandler(
                this, lookupId, timeout, unit);
        
        return submit(process, timeout, unit);
    }
    
    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            Contact[] contacts, long timeout, TimeUnit unit) {
        DHTFutureProcess<NodeEntity> process = new NodeResponseHandler(
                this, lookupId, contacts, timeout, unit);
        
        return submit(process, timeout, unit);
    }
    
    @Override
    public DHTFuture<ValueEntity> get(ValueKey key, 
            long timeout, TimeUnit unit) {
        
        DHTFutureProcess<ValueEntity> process = new ValueResponseHandler(
                this, key, timeout, unit);
        
        return submit(process, timeout, unit);
    }
    
    @Override
    public DHTFuture<StoreEntity> put(KUID key, Value value, 
            long timeout, TimeUnit unit) {
        return storeManager.put(key, value, timeout, unit);
    }
    
    @Override
    public DHTFuture<StoreEntity> enqueue(KUID key, Value value, 
            long timeout, TimeUnit unit) {
        return storeManager.enqueue(key, value, timeout, unit);
    }

    /**
     * Stores the given {@link ValueTuple}ies.
     */
    private void store(final Contact dst, final SecurityToken securityToken, 
            final Collection<? extends ValueTuple> values) {
        
        if (securityToken == null) {
            long timeout = NetworkSettings.DEFAULT_TIMEOUT.getTimeInMillis();
            DHTFuture<SecurityTokenEntity> future 
                = getSecurityToken(dst, timeout, TimeUnit.MILLISECONDS);
            future.addFutureListener(
                    new EventListener<FutureEvent<SecurityTokenEntity>>() {
                @Override
                public void handleEvent(FutureEvent<SecurityTokenEntity> event) {
                    if (event.getType() == Type.SUCCESS) {
                        SecurityTokenEntity entity = event.getResult();
                        SecurityToken securityToken = entity.getSecurityToken();
                        store(dst, securityToken, values);
                    }
                }
            });
            return;
        }
        
        long timeout = StoreSettings.STORE_TIMEOUT.getTimeInMillis();
        for (ValueTuple entity : values) {
            storeManager.enqueue(dst, securityToken, entity, 
                    timeout, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Retrieves the {@link Contact}'s {@link SecurityToken}.
     */
    protected DHTFuture<SecurityTokenEntity> getSecurityToken(
            Contact dst, long timeout, TimeUnit unit) {
        
        KUID lookupId = KUID.createRandomID();
        DHTFutureProcess<SecurityTokenEntity> process 
            = new SecurityTokenResponseHandler(this, 
                    dst, lookupId, timeout, unit);
        
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
        localhost.setExternalAddress(externalAddress);
        estimator.addEstimatedRemoteSize(estimatedSize);
    }
}

package org.limewire.mojito;

import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.listener.EventListener;
import org.limewire.mojito.concurrent.AsyncProcess;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueFactoryManager;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.StorableModelManager;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.handler.response.NodeResponseHandler2;
import org.limewire.mojito.handler.response.PingResponseHandler2;
import org.limewire.mojito.handler.response.StoreResponseHandler2;
import org.limewire.mojito.handler.response.ValueResponseHandler2;
import org.limewire.mojito.io.MessageDispatcher2;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.messages.MessageHelper2;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.security.SecurityTokenHelper2;
import org.limewire.mojito.util.DHTSizeEstimator;
import org.limewire.mojito.util.HostFilter;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken;

public class Context2 implements MojitoDHT2 {
    
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
    private final MACCalculatorRepositoryManager calculator 
        = new MACCalculatorRepositoryManager();
    
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
    private final MessageDispatcher2 messageDispatcher;
    
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
    private final MessageHelper2 messageHelper;
    
    /**
     * 
     */
    private final SecurityTokenHelper2 tokenHelper;
    
    /**
     * 
     */
    private volatile HostFilter hostFilter = null;
    
    /**
     * 
     */
    public Context2(String name,
            MessageDispatcher2 messageDispatcher, 
            MessageFactory messageFactory,
            RouteTable routeTable, Database database) {
        
        this.name = name;
        this.messageDispatcher = messageDispatcher;
        this.routeTable = routeTable;
        this.database = database;
        
        this.messageHelper = new MessageHelper2(this, messageFactory);
        
        SecurityToken.TokenProvider tokenProvider 
            = new SecurityToken.AddressSecurityTokenProvider(calculator);
        
        this.tokenHelper = new SecurityTokenHelper2(tokenProvider);
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
    public BigInteger size() {
        return estimator.getEstimatedSize(routeTable);
    }
    
    @Override
    public MessageDispatcher2 getMessageDispatcher() {
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
    public MessageHelper2 getMessageHelper() {
        return messageHelper;
    }
    
    /**
     * 
     */
    public SecurityTokenHelper2 getSecurityTokenHelper() {
        return tokenHelper;
    }
    
    /**
     * 
     */
    public MACCalculatorRepositoryManager getMACCalculatorRepositoryManager() {
        return calculator;
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
    
    //@Override
    /*public DHTFuture<BootstrapEntity> bootstrap(SocketAddress dst, 
            long timeout, TimeUnit unit) {
        return null;
    }*/
    
    @Override
    public DHTFuture<PingEntity> ping(SocketAddress dst, 
            long timeout, TimeUnit unit) {
        AsyncProcess<PingEntity> process = new PingResponseHandler2(
                this, dst, timeout, unit);
        
        DHTFuture<PingEntity> future 
            = futureManager.submit(process, timeout, unit);
        future.addFutureListener(onPong);
        return future;
    }
    
    @Override
    public DHTFuture<PingEntity> ping(Contact dst, long timeout, TimeUnit unit) {
        AsyncProcess<PingEntity> process = new PingResponseHandler2(
                this, dst, timeout, unit);
        
        DHTFuture<PingEntity> future 
            = futureManager.submit(process, timeout, unit);
        future.addFutureListener(onPong);
        return future;
    }
    
    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            long timeout, TimeUnit unit) {
        AsyncProcess<NodeEntity> process = new NodeResponseHandler2(
                this, lookupId, timeout, unit);
        
        return futureManager.submit(process, timeout, unit);
    }
    
    @Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, 
            Contact[] contacts, long timeout, TimeUnit unit) {
        AsyncProcess<NodeEntity> process = new NodeResponseHandler2(
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
        
        AsyncProcess<ValueEntity> process = new ValueResponseHandler2(
                this, key, timeout, unit);
        
        return futureManager.submit(process, timeout, unit);
    }
    
    private final Object putLock = new Object();
    
    //@Override
    public DHTFuture<StoreEntity> put(DHTValueEntity value, 
            long timeout, TimeUnit unit) {
        
        synchronized (putLock) {
            
            final AtomicReference<DHTFuture<StoreEntity>> futureRef 
                = new AtomicReference<DHTFuture<StoreEntity>>();
            
            final StoreResponseHandler2 process 
                = new StoreResponseHandler2(
                    this, new DHTValueEntity[] { value }, timeout, unit);
            
            KUID lookupId = value.getPrimaryKey();
            DHTFuture<NodeEntity> future 
                = lookup(lookupId, timeout2, unit2);
            
            future.addFutureListener(new EventListener<FutureEvent<NodeEntity>>() {
                @Override
                public void handleEvent(FutureEvent<NodeEntity> event) {
                    switch (event.getType()) {
                        case SUCCESS:
                            NodeEntity entity = event.getResult();
                            process.store(entity.getContacts());
                            break;
                        case EXCEPTION:
                            
                    }
                }
            });
            
            DHTFuture<StoreEntity> store 
                = futureManager.submit(process, timeout, unit);
            futureRef.set(store);
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
        boolean changed = localhost.setExternalAddress(externalAddress);
        
        if (changed) {
            modelManager.handleContactChange(null);
        }
        
        estimator.addEstimatedRemoteSize(estimatedSize);
    }
}

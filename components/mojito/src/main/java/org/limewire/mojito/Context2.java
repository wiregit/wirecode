package org.limewire.mojito;

import java.io.Closeable;
import java.math.BigInteger;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.mojito.concurrent.AsyncProcess;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureAdapter;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.StorableModelManager;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.handler.response.NodeResponseHandler2;
import org.limewire.mojito.handler.response.PingResponseHandler2;
import org.limewire.mojito.handler.response.ValueResponseHandler2;
import org.limewire.mojito.io.MessageDispatcher2;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.impl.LocalContact;
import org.limewire.mojito.util.DHTSizeEstimator;

public class Context2 implements Closeable {
    
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
    private final StorableModelManager modelManager = null;
    
    /**
     * 
     */
    public Context2(MessageDispatcher2 messageDispatcher, 
            RouteTable routeTable, Database database) {
        
        this.messageDispatcher = messageDispatcher;
        this.routeTable = routeTable;
        this.database = database;
    }
    
    @Override
    public void close() {
        futureManager.close();
    }
    
    /**
     * 
     */
    public MessageDispatcher2 getMessageDispatcher() {
        return messageDispatcher;
    }
    
    /**
     * 
     */
    public RouteTable getRouteTable() {
        return routeTable;
    }
    
    /**
     * 
     */
    public Database getDatabase() {
        return database;
    }
    
    //@Override
    public DHTFuture<PingEntity> ping(SocketAddress dst, 
            long timeout, TimeUnit unit) {
        AsyncProcess<PingEntity> process = new PingResponseHandler2(
                this, messageDispatcher, dst, timeout, unit);
        
        DHTFuture<PingEntity> future 
            = futureManager.submit(process, timeout, unit);
        future.addFutureListener(onPong);
        return future;
    }
    
    //@Override
    public DHTFuture<PingEntity> ping(Contact dst, long timeout, TimeUnit unit) {
        AsyncProcess<PingEntity> process = new PingResponseHandler2(
                this, messageDispatcher, dst, timeout, unit);
        
        DHTFuture<PingEntity> future 
            = futureManager.submit(process, timeout, unit);
        future.addFutureListener(onPong);
        return future;
    }
    
    //@Override
    public DHTFuture<NodeEntity> lookup(KUID lookupId, long timeout, TimeUnit unit) {
        AsyncProcess<NodeEntity> process = new NodeResponseHandler2(
                this, messageDispatcher, lookupId, timeout, unit);
        
        return futureManager.submit(process, timeout, unit);
    }
    
    //@Override
    public DHTFuture<ValueEntity> get(KUID lookupId, long timeout, TimeUnit unit) {
        AsyncProcess<NodeEntity> process = new ValueResponseHandler2(
                this, messageDispatcher, lookupId, timeout, unit);
        
        return futureManager.submit(process, timeout, unit);
    }
    
    //@Override
    /*public DHTFuture<StoreEntity> put(KUID lookupId, long timeout, TimeUnit unit) {
        AsyncProcess<StoreEntity> process = new StoreResponseHandler2(
                this, messageDispatcher, lookupId, timeout, unit);
        
        return futureManager.submit(process, timeout, unit);
    }*/
    
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

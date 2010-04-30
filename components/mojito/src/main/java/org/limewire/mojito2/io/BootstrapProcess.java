package org.limewire.mojito2.io;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.CollectionUtils;
import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito2.Context;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.CollisionException;
import org.limewire.mojito2.entity.DefaultBootstrapEntity;
import org.limewire.mojito2.entity.NodeEntity;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.routing.RouteTable;
import org.limewire.mojito2.util.MaxStack;
import org.limewire.mojito2.util.TimeAwareIterable;
import org.limewire.util.ExceptionUtils;

public class BootstrapProcess implements AsyncProcess<BootstrapEntity> {
    
    private final Context context;
    
    private final Contact contact;
    
    private final SocketAddress address;
    
    private final BootstrapConfig config;
    
    private final MaxStack refreshStack;

    private long startTime;
    
    private DHTFuture<BootstrapEntity> future = null;
    
    private DHTFuture<PingEntity> pingFuture = null;
    
    private DHTFuture<NodeEntity> lookupFuture = null;
    
    private final List<DHTFuture<NodeEntity>> refreshFutures 
        = new ArrayList<DHTFuture<NodeEntity>>();
    
    private DHTFuture<PingEntity> collisitonFuture = null;
    
    private Iterator<KUID> bucketsToRefresh = null;
    
    public BootstrapProcess(Context context, Contact contact, 
            BootstrapConfig config) {
        this(context, contact, null, config);
    }
    
    public BootstrapProcess(Context context, SocketAddress address, 
            BootstrapConfig config) {
        this(context, null, address, config);
    }
    
    private BootstrapProcess(Context context, Contact contact,
            SocketAddress address, BootstrapConfig config) {
        
        this.context = context;
        this.contact = contact;
        this.address = address;
        this.config = config;
        
        refreshStack = new MaxStack(config.getAlpha());
    }
    
    @Override
    public void start(DHTFuture<BootstrapEntity> future) {
        synchronized (future) {
            this.future = future;
            
            startTime = System.currentTimeMillis();
            start();
        }
    }

    @Override
    public void stop(DHTFuture<BootstrapEntity> future) {
        stop();
    }
    
    private void start() {
        if (contact != null) {
            lookup(contact);
        } else {
            ping(address);
        }
    }
    
    private void stop() {
        synchronized (future) {
            if (pingFuture != null) {
                pingFuture.cancel(true);
            }
            
            if (lookupFuture != null) {
                lookupFuture.cancel(true);
            }
            
            if (collisitonFuture != null) {
                collisitonFuture.cancel(true);
            }
            
            for (DHTFuture<?> future : refreshFutures) {
                future.cancel(true);
            }
            
            refreshFutures.clear();
        }
    }
    
    /**
     * 
     */
    private void onException(Throwable t) {
        future.setException(t);
    }
    
    /**
     * 
     */
    private void onCancellation() {
        future.cancel(true);
    }
    
    /**
     * 
     */
    private void onCollision(PingEntity entity) {
        Contact contact = entity.getContact();
        future.setException(new CollisionException(contact));
    }
    
    /**
     * 
     */
    private void onCompletation(BootstrapEntity entity) {
        future.setValue(entity);
    }
    
    /**
     * 
     */
    private void uncaughtException(Throwable t) {
        future.setException(t);
        ExceptionUtils.reportIfUnchecked(t);
    }
    
    // --- PING ---
    
    /**
     * 
     */
    private void ping(SocketAddress address) {
        long timeout = config.getPingTimeoutInMillis();
        
        pingFuture = context.ping(address, 
                timeout, TimeUnit.MILLISECONDS);
        
        pingFuture.addFutureListener(new EventListener<FutureEvent<PingEntity>>() {
            @Override
            public void handleEvent(FutureEvent<PingEntity> event) {
                handlePong(event);
            }
        });
    }
    
    /**
     * 
     */
    private void handlePong(FutureEvent<PingEntity> event) {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            try {
                switch (event.getType()) {
                    case SUCCESS:
                        onPong(event.getResult());
                        break;
                    case EXCEPTION:
                        onException(event.getException());
                        break;
                    default:
                        onCancellation();
                        break;
                }
            } catch (Throwable t) {
                uncaughtException(t);
            }
        }
    }
    
    /**
     * 
     */
    private void onPong(PingEntity entity) {
        Contact contact = entity.getContact();
        lookup(contact);
    }
    
    /**
     * 
     */
    private void lookup(Contact contact) {
        Contact localhost = context.getLocalNode();
        KUID lookupId = localhost.getNodeID();
        
        long timeout = config.getLookupTimeoutInMillis();
        lookupFuture = context.lookup(lookupId, 
                new Contact[] { contact }, 
                timeout, TimeUnit.MILLISECONDS);
        
        lookupFuture.addFutureListener(new EventListener<FutureEvent<NodeEntity>>() {
            @Override
            public void handleEvent(FutureEvent<NodeEntity> event) {
                handleLookup(event);
            }
        });
    }
    
    // --- LOOKUP ---
    
    private void handleLookup(FutureEvent<NodeEntity> event) {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            try {
                switch (event.getType()) {
                    case SUCCESS:
                        onLookup(event.getResult());
                        break;
                    case EXCEPTION:
                        onException(event.getException());
                        break;
                    default:
                        onCancellation();
                        break;
                }
            } catch (Throwable t) {
                uncaughtException(t);
            }
        }
    }
    
    private void onLookup(NodeEntity entity) {
        Contact[] collisions = entity.getCollisions();
        
        if (collisions.length == 0) {
            doRefreshAll();
            return;
        }
        
        long timeout = config.getPingTimeoutInMillis();
        collisitonFuture = context.collisionPing(
                collisions, timeout, TimeUnit.MILLISECONDS);
        
        collisitonFuture.addFutureListener(new EventListener<FutureEvent<PingEntity>>() {
            @Override
            public void handleEvent(FutureEvent<PingEntity> event) {
                handleCollision(event);
            }
        });
    }
    
    private void handleCollision(FutureEvent<PingEntity> event) {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            try {
                switch (event.getType()) {
                    case SUCCESS:
                        onCollision(event.getResult());
                        break;
                    case EXCEPTION:
                        onCollisionException(event.getException());
                        break;
                    default:
                        onCancellation();
                        break;
                }
            } catch (Throwable t) {
                uncaughtException(t);
            }
        }
    }
    
    private void onCollisionException(ExecutionException err) {
        doRefreshAll();
    }
    
    private void doRefreshAll() {
        KUID[] bucketIds = getBucketsToRefresh();
        
        long timeout = config.getRefreshTimeoutInMillis();
        bucketsToRefresh = new TimeAwareIterable<KUID>(
                bucketIds, timeout, TimeUnit.MILLISECONDS).iterator();
        
        doRefreshNext(0);
    }
    
    private KUID[] getBucketsToRefresh() {
        RouteTable routeTable = context.getRouteTable();
        List<KUID> bucketIds = CollectionUtils.toList(
                routeTable.getRefreshIDs(true));
        Collections.reverse(bucketIds);
        return bucketIds.toArray(new KUID[0]);
    }
    
    // --- REFRESH ---
    
    private void doRefreshNext(int count) {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            try {
                preProcess(count);
                
                while (refreshStack.hasFree()) {
                    if (!bucketsToRefresh.hasNext()) {
                        break;
                    }
                    
                    KUID bucketId = bucketsToRefresh.next();
                    
                    long timeout = config.getLookupTimeoutInMillis();
                    DHTFuture<NodeEntity> future = context.lookup(
                            bucketId, timeout, TimeUnit.MILLISECONDS);
                    
                    future.addFutureListener(new EventListener<FutureEvent<NodeEntity>>() {
                        @Override
                        public void handleEvent(FutureEvent<NodeEntity> event) {
                            try {
                                doRefreshNext(1);
                            } catch (Throwable t) {
                                uncaughtException(t);
                            }
                        }
                    });
                    
                    refreshFutures.add(future);
                    refreshStack.push();
                }
            } finally {
                postProcess();
            }
        }
    }
    
    private void preProcess(int count) {
        refreshStack.pop(count);
    }
    
    private void postProcess() {
        int count = refreshStack.poll();
        if (count == 0) {
            complete();
        }
    }
    
    private void complete() {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            long time = System.currentTimeMillis() - startTime;
            onCompletation(new DefaultBootstrapEntity(
                    context, time, TimeUnit.MILLISECONDS));
        }
    }
}

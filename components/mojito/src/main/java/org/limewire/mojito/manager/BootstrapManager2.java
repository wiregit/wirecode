package org.limewire.mojito.manager;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.CollectionUtils;
import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT2;
import org.limewire.mojito.concurrent.AsyncProcess;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.entity.DefaultBootstrapEntity;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.util.MaxStack;
import org.limewire.mojito.util.TimeAwareIterable;

public class BootstrapManager2 implements AsyncProcess<BootstrapEntity> {
    
    private final MojitoDHT2 dht;
    
    private final SocketAddress address;
    
    private final BootstrapConfig config;
    
    private final List<DHTFuture<NodeEntity>> phaseTwo 
        = new ArrayList<DHTFuture<NodeEntity>>();
    
    private final MaxStack lookupCounter;

    private long startTime;
    
    private DHTFuture<BootstrapEntity> future = null;
    
    private DHTFuture<PingEntity> pingFuture = null;
    
    private DHTFuture<NodeEntity> phaseOne = null;
    
    private Iterator<KUID> bucketsToRefresh = null;
    
    public BootstrapManager2(MojitoDHT2 dht, 
            SocketAddress address, BootstrapConfig config) {
        
        this.dht = dht;
        this.address = address;
        this.config = config;
        
        lookupCounter = new MaxStack(config.getAlpha());
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
        ping();
    }
    
    private void stop() {
        synchronized (future) {
            if (pingFuture != null) {
                pingFuture.cancel(true);
            }
            
            if (phaseOne != null) {
                phaseOne.cancel(true);
            }
            
            for (DHTFuture<?> future : phaseTwo) {
                future.cancel(true);
            }
            
            phaseTwo.clear();
        }
    }
    
    private void ping() {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            long timeout = config.getPingTimeoutInMillis();
            pingFuture = dht.ping(address, timeout, TimeUnit.MILLISECONDS);
            pingFuture.addFutureListener(new EventListener<FutureEvent<PingEntity>>() {
                @Override
                public void handleEvent(FutureEvent<PingEntity> event) {
                    handlePingEvent(event);
                }
            });
        }
    }
    
    private void handlePingEvent(FutureEvent<PingEntity> event) {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            switch (event.getType()) {
                case SUCCESS:
                    handlePingSuccess(event.getResult());
                    break;
                default:
                    handlePingError();
                    break;
            }
        }
    }
    
    private void handlePingSuccess(PingEntity entity) {
        Contact localhost = dht.getLocalNode();
        KUID lookupId = localhost.getNodeID();
        
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            Contact contact 
                = entity.getContact();
            
            long timeout = config.getLookupTimeoutInMillis();
            phaseOne = dht.lookup(lookupId, 
                    new Contact[] { contact }, 
                    timeout, TimeUnit.MILLISECONDS);
            
            phaseOne.addFutureListener(new EventListener<FutureEvent<NodeEntity>>() {
                @Override
                public void handleEvent(FutureEvent<NodeEntity> event) {
                    handlePhaseOneEvent(event);
                }
            });
        }
    }
    
    private void handlePingError() {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            future.setException(new IOException());
        }
    }
    
    private void handlePhaseOneEvent(FutureEvent<NodeEntity> event) {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            switch (event.getType()) {
                case SUCCESS:
                    handlePhaseOneSuccess(event.getResult());
                    break;
                default:
                    handlePhaseOneError();
                    break;
            }
        }
    }
    
    private void handlePhaseOneSuccess(NodeEntity entity) {
        
        KUID[] bucketIds = getBucketsToRefresh();
        
        long timeout = config.getRefreshTimeoutInMillis();
        bucketsToRefresh = new TimeAwareIterable<KUID>(
                bucketIds, timeout, TimeUnit.MILLISECONDS).iterator();
        
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            processNextBucket(0);
        }
    }
    
    private void processNextBucket(int count) {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            try {
                preProcess(count);
                
                while (lookupCounter.hasFree()) {
                    if (!bucketsToRefresh.hasNext()) {
                        break;
                    }
                    
                    KUID bucketId = bucketsToRefresh.next();
                    
                    long timeout = config.getLookupTimeoutInMillis();
                    DHTFuture<NodeEntity> future = dht.lookup(
                            bucketId, timeout, TimeUnit.MILLISECONDS);
                    
                    future.addFutureListener(new EventListener<FutureEvent<NodeEntity>>() {
                        @Override
                        public void handleEvent(FutureEvent<NodeEntity> event) {
                            processNextBucket(1);
                        }
                    });
                    
                    phaseTwo.add(future);
                    lookupCounter.push();
                }
            } finally {
                postProcess();
            }
        }
    }
    
    private void preProcess(int count) {
        lookupCounter.pop(count);
    }
    
    private void postProcess() {
        int count = lookupCounter.poll();
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
            future.setValue(new DefaultBootstrapEntity(
                    dht, time, TimeUnit.MILLISECONDS));
        }
    }
    
    private void handlePhaseOneError() {
        synchronized (future) {
            if (future.isDone()) {
                return;
            }
            
            future.setException(new IOException());
        }
    }
    
    private KUID[] getBucketsToRefresh() {
        RouteTable routeTable = dht.getRouteTable();
        List<KUID> bucketIds = CollectionUtils.toList(
                routeTable.getRefreshIDs(true));
        Collections.reverse(bucketIds);
        return bucketIds.toArray(new KUID[0]);
    }
}

package org.limewire.mojito.routing;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT2;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.ManagedRunnable;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.settings.BucketRefresherSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.util.EventUtils;

/**
 * 
 */
public class BucketRefresher2 implements Closeable {
    
    private static final ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory("BucketRefresherThread"));
    
    private final MojitoDHT2 dht;
    
    private final ScheduledFuture<?> future;
    
    /**
     * 
     */
    private final AtomicBoolean barrier 
        = new AtomicBoolean(true);
    
    /**
     * 
     */
    private PingTask pingTask = null;
    
    /**
     * 
     */
    private LookupTask lookupTask = null;
    
    /**
     * 
     */
    public BucketRefresher2(MojitoDHT2 dht, 
            long frequency, TimeUnit unit) {
        
        this.dht = dht;
        
        Runnable task = new ManagedRunnable() {
            @Override
            protected void doRun() {
                process();
            }
        };
        
        future = EXECUTOR.scheduleWithFixedDelay(
                task, frequency, frequency, unit);
    }
    
    @Override
    public synchronized void close() {
        if (future != null) {
            future.cancel(true);
        }
        
        if (pingTask != null) {
            pingTask.close();
        }
        
        if (lookupTask != null) {
            lookupTask.close();
        }
    }
    
    /**
     * 
     */
    private synchronized void process() {
        if (barrier.getAndSet(false)) {
            ping();
        }
    }
    
    private synchronized void ping() {
        
        Runnable callback = new Runnable() {
            @Override
            public void run() {
                lookup();
            }
        };
        
        pingTask = new PingTask(dht, callback, timeout, unit);
    }
    
    private synchronized void lookup() {
        Runnable callback = new Runnable() {
            @Override
            public void run() {
                barrier.set(true);
            }
        };
        
        lookupTask = new LookupTask(dht, callback, timeout, unit);
    }
    
    private static abstract class AbstractTask implements Closeable {
        
        protected final MojitoDHT2 dht;
        
        protected final Runnable callback;
        
        protected final long timeout;
        
        protected final TimeUnit unit;
        
        protected volatile boolean open = true;
        
        public AbstractTask(MojitoDHT2 dht, Runnable callback, 
                long timeout, TimeUnit unit) {
            
            this.dht = dht;
            this.callback = callback;
            this.timeout = timeout;
            this.unit = unit;
        }
        
        @Override
        public void close() {
            open = false;
        }
    }
    
    private static class PingTask extends AbstractTask {
        
        private final EventListener<FutureEvent<PingEntity>> listener 
                = new EventListener<FutureEvent<PingEntity>>() {
            @Override
            public void handleEvent(FutureEvent<PingEntity> event) {
                doNext();
            }
        };
        
        private final Contact[] contacts;
        
        private int index = 0;
        
        public PingTask(MojitoDHT2 dht, 
                Runnable callback, 
                long timeout, TimeUnit unit) {
            super(dht, callback, timeout, unit);
            
            List<Contact> contacts = new ArrayList<Contact>();
            
            long pingNearest = BucketRefresherSettings.BUCKET_REFRESHER_PING_NEAREST.getValue();
            
            if (0L < pingNearest) {
                Contact localhost = dht.getLocalNode();
                RouteTable routeTable = dht.getRouteTable();
                Collection<Contact> nodes = routeTable.select(
                        localhost.getNodeID(),
                        KademliaSettings.REPLICATION_PARAMETER.getValue(), 
                        SelectMode.ALL);
                
                
                for (Contact node : nodes) {
                    if (localhost.equals(node)) {
                        continue;
                    }
                    
                    long timeStamp = node.getTimeStamp();
                    long time = System.currentTimeMillis() - timeStamp;
                    
                    if (time >= pingNearest) {
                        contacts.add(node);
                    }
                }
            }
            
            this.contacts = contacts.toArray(new Contact[0]);
            
            doNext();
        }
        
        private void doNext() {
            if (!open || contacts == null 
                    || index >= contacts.length) {
                EventUtils.fireEvent(callback);
                return;
            }
            
            Contact dst = contacts[index++];
            DHTFuture<PingEntity> future = dht.ping(dst, timeout, unit);
            future.addFutureListener(listener);
        }
    }
    
    private static class LookupTask extends AbstractTask {
        
        private final EventListener<FutureEvent<NodeEntity>> listener 
                = new EventListener<FutureEvent<NodeEntity>>() {
            @Override
            public void handleEvent(FutureEvent<NodeEntity> event) {
                doNext();
            }
        };
        
        private final KUID[] lookupIds;
        
        private int index = 0;
        
        public LookupTask(MojitoDHT2 dht, 
                Runnable callback, 
                long timeout, TimeUnit unit) {
            
            super(dht, callback, timeout, unit);
            
            RouteTable routeTable = dht.getRouteTable();
            Collection<KUID> list = routeTable.getRefreshIDs(false);
            this.lookupIds = list.toArray(new KUID[0]);
            
            doNext();
        }
        
        private void doNext() {
            if (!open || lookupIds == null 
                    || index >= lookupIds.length) {
                EventUtils.fireEvent(callback);
                return;
            }
            
            KUID next = lookupIds[index++];
            DHTFuture<NodeEntity> future = dht.lookup(next, timeout, unit);
            future.addFutureListener(listener);
        }
    }
}

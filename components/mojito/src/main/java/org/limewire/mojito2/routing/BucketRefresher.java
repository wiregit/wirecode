package org.limewire.mojito2.routing;

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
import org.limewire.mojito2.DHT;
import org.limewire.mojito2.KUID;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.concurrent.ManagedRunnable;
import org.limewire.mojito2.entity.NodeEntity;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.routing.RouteTable.SelectMode;
import org.limewire.mojito2.settings.BucketRefresherSettings;
import org.limewire.mojito2.settings.KademliaSettings;
import org.limewire.mojito2.settings.LookupSettings;
import org.limewire.mojito2.settings.NetworkSettings;
import org.limewire.mojito2.util.EventUtils;

/**
 * 
 */
public class BucketRefresher implements Closeable {
    
    private static final ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory("BucketRefresherThread"));
    
    private final DHT dht;
    
    private final Config config;
    
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
    public BucketRefresher(DHT dht, Config config,
            long frequency, TimeUnit unit) {
        
        this.dht = dht;
        this.config = config;
        
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
        
        long timeout = config.getPingTimeoutInMillis();
        pingTask = new PingTask(dht, callback, 
                timeout, TimeUnit.MILLISECONDS);
    }
    
    private synchronized void lookup() {
        Runnable callback = new Runnable() {
            @Override
            public void run() {
                barrier.set(true);
            }
        };
        
        long timeout = config.getLookupTimeoutInMillis();
        lookupTask = new LookupTask(dht, callback, 
                timeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 
     */
    private static abstract class AbstractTask implements Closeable {
        
        protected final DHT dht;
        
        protected final Runnable callback;
        
        protected final long timeout;
        
        protected final TimeUnit unit;
        
        protected volatile boolean open = true;
        
        public AbstractTask(DHT dht, Runnable callback, 
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
        
        private volatile DHTFuture<PingEntity> future = null;
        
        public PingTask(DHT dht, 
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
        
        @Override
        public void close() {
            super.close();
            
            DHTFuture<PingEntity> future = this.future;
            if (future != null) {
                future.cancel(true);
            }
        }
        
        private void doNext() {
            if (!open || contacts == null 
                    || index >= contacts.length) {
                EventUtils.fireEvent(callback);
                return;
            }
            
            Contact dst = contacts[index++];
            
            future = dht.ping(dst, timeout, unit);
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
        
        private volatile DHTFuture<NodeEntity> future = null;
        
        public LookupTask(DHT dht, 
                Runnable callback, 
                long timeout, TimeUnit unit) {
            
            super(dht, callback, timeout, unit);
            
            RouteTable routeTable = dht.getRouteTable();
            Collection<KUID> list = routeTable.getRefreshIDs(false);
            this.lookupIds = list.toArray(new KUID[0]);
            
            doNext();
        }
        
        @Override
        public void close() {
            super.close();
            
            DHTFuture<NodeEntity> future = this.future;
            if (future != null) {
                future.cancel(true);
            }
        }
        
        private void doNext() {
            if (!open || lookupIds == null 
                    || index >= lookupIds.length) {
                EventUtils.fireEvent(callback);
                return;
            }
            
            KUID next = lookupIds[index++];
            
            future = dht.lookup(next, timeout, unit);
            future.addFutureListener(listener);
        }
    }
    
    /**
     * 
     */
    public static class Config {

        private volatile long pingTimeout 
            = NetworkSettings.DEFAULT_TIMEOUT.getValue();
        
        private volatile long lookupTimeout 
            = LookupSettings.FIND_NODE_LOOKUP_TIMEOUT.getValue();
        
        public Config() {
            
        }
        
        /**
         * 
         */
        public long getPingTimeout(TimeUnit unit) {
            return unit.convert(pingTimeout, TimeUnit.MILLISECONDS);
        }
        
        /**
         * 
         */
        public long getPingTimeoutInMillis() {
            return getPingTimeout(TimeUnit.MILLISECONDS);
        }
        
        /**
         * 
         */
        public void setPingTimeout(long timeout, TimeUnit unit) {
            this.pingTimeout = unit.toMillis(timeout);
        }
        
        /**
         * 
         */
        public long getLookupTimeout(TimeUnit unit) {
            return unit.convert(lookupTimeout, TimeUnit.MILLISECONDS);
        }
        
        /**
         * 
         */
        public long getLookupTimeoutInMillis() {
            return getLookupTimeout(TimeUnit.MILLISECONDS);
        }
        
        /**
         * 
         */
        public void setLookupTimeout(long timeout, TimeUnit unit) {
            this.lookupTimeout = unit.toMillis(timeout);
        }
    }
}

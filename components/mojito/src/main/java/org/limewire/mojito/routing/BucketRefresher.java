package org.limewire.mojito.routing;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito.DHT;
import org.limewire.mojito.KUID;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.ManagedRunnable;
import org.limewire.mojito.entity.NodeEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.routing.RouteTable.SelectMode;
import org.limewire.mojito.settings.BucketRefresherSettings;
import org.limewire.mojito.settings.KademliaSettings;
import org.limewire.mojito.settings.LookupSettings;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.EventUtils;
import org.limewire.mojito.util.SchedulingUtils;

/**
 * The {@link BucketRefresher}'s job is to perform <tt>PINGs</tt>
 * and <tt>FIND_NODE</tt> in periodic intervals to refresh the
 * {@link RouteTable}'s buckets.
 */
public class BucketRefresher implements Closeable {
    
    private final DHT dht;
    
    private final Config config;
    
    private final long frequency;
    
    private final TimeUnit unit;
    
    private ScheduledFuture<?> future;
    
    /**
     * A flag (barrier) that's set to {@code true} if the 
     * {@link BucketRefresher} is currently active.
     */
    private final AtomicBoolean active 
        = new AtomicBoolean(false);
    
    private PingTask pingTask = null;
    
    private LookupTask lookupTask = null;
    
    private boolean open = true;
    
    /**
     * Creates a {@link BucketRefresher}
     */
    public BucketRefresher(DHT dht) {
        this (dht, BucketRefresherSettings.BUCKET_REFRESHER_DELAY.getTimeInMillis(), 
                TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a {@link BucketRefresher}
     */
    public BucketRefresher(DHT dht, long frequency, TimeUnit unit) {
        this(dht, new Config(), frequency, unit);
    }
    
    /**
     * Creates a {@link BucketRefresher}
     */
    public BucketRefresher(DHT dht, Config config,
            long frequency, TimeUnit unit) {
        
        this.dht = dht;
        this.config = config;
        this.frequency = frequency;
        this.unit = unit;
    }
    
    /**
     * Starts the {@link BucketRefresher}
     */
    public synchronized void start() {
        
        if (!open) {
            return;
        }
        
        if (future != null && !future.isDone()) {
            return;
        }
        
        Runnable task = new ManagedRunnable() {
            @Override
            protected void doRun() {
                if (dht.isReady()) {
                    process();
                }
            }
        };
        
        active.set(false);
        future = SchedulingUtils.scheduleWithFixedDelay(
                task, frequency, frequency, unit);
    }
    
    /**
     * Stops the {@link BucketRefresher}
     */
    public synchronized void stop() {
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
    
    @Override
    public synchronized void close() {
        open = false;
        stop();
    }
    
    /**
     * 
     */
    private synchronized void process() {
        if (!active.getAndSet(true)) {
            ping();
        }
    }
    
    /**
     * Sends a bunch of PINGs and calls {@link #lookup()} when done.
     */
    private synchronized void ping() {
        
        // Called if the PING-ing is complete.
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
    
    /**
     * Performs <tt>FIND_NODE</tt> lookups to refresh all buckets.
     */
    private synchronized void lookup() {
        
        // Called if FIND_NODE-ing is complete.
        Runnable callback = new Runnable() {
            @Override
            public void run() {
                active.set(false);
            }
        };
        
        long timeout = config.getLookupTimeoutInMillis();
        lookupTask = new LookupTask(dht, callback, 
                timeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * An abstract task.
     */
    private static abstract class Task implements Closeable {
        
        protected final DHT dht;
        
        protected final Runnable callback;
        
        protected final long timeout;
        
        protected final TimeUnit unit;
        
        protected volatile boolean open = true;
        
        public Task(DHT dht, Runnable callback, 
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
    
    /**
     * A {@link Task} that sends <tt>PING</tt> requests.
     */
    private static class PingTask extends Task {
        
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
            
            long pingNearest = BucketRefresherSettings.BUCKET_REFRESHER_PING_NEAREST.getTimeInMillis();
            
            if (0L < pingNearest) {
                Contact localhost = dht.getLocalhost();
                RouteTable routeTable = dht.getRouteTable();
                Collection<Contact> nodes = routeTable.select(
                        localhost.getContactId(),
                        KademliaSettings.K, 
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
    
    /**
     * A {@link Task} that performs <tt>FIND_NODE</tt> lookups.
     */
    private static class LookupTask extends Task {
        
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
     * The configuration for the {@link BucketRefresher}.
     */
    public static class Config {

        private volatile long pingTimeout 
            = NetworkSettings.DEFAULT_TIMEOUT.getTimeInMillis();
        
        private volatile long lookupTimeout 
            = LookupSettings.FIND_NODE_LOOKUP_TIMEOUT.getTimeInMillis();
        
        public Config() {
            
        }
        
        /**
         * Returns the <tt>PING</tt> timeout.
         */
        public long getPingTimeout(TimeUnit unit) {
            return unit.convert(pingTimeout, TimeUnit.MILLISECONDS);
        }
        
        /**
         * Returns the <tt>PING</tt> timeout in milliseconds.
         */
        public long getPingTimeoutInMillis() {
            return getPingTimeout(TimeUnit.MILLISECONDS);
        }
        
        /**
         * Sets the <tt>PING</tt> timeout.
         */
        public void setPingTimeout(long timeout, TimeUnit unit) {
            this.pingTimeout = unit.toMillis(timeout);
        }
        
        /**
         * Returns the <tt>FIND_NODE</tt> timeout.
         */
        public long getLookupTimeout(TimeUnit unit) {
            return unit.convert(lookupTimeout, TimeUnit.MILLISECONDS);
        }
        
        /**
         * Returns the <tt>FIND_NODE</tt> timeout in milliseconds.
         */
        public long getLookupTimeoutInMillis() {
            return getLookupTimeout(TimeUnit.MILLISECONDS);
        }
        
        /**
         * Sets the <tt>FIND_NODE</tt> timeout.
         */
        public void setLookupTimeout(long timeout, TimeUnit unit) {
            this.lookupTimeout = unit.toMillis(timeout);
        }
    }
}

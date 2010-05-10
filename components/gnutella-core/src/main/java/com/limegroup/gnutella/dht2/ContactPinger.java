package com.limegroup.gnutella.dht2;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.FixedSizeLIFOSet;
import org.limewire.collection.FixedSizeLIFOSet.EjectionPolicy;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.DHTSettings;
import org.limewire.mojito2.MojitoDHT;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.util.Objects;

class ContactPinger implements Closeable {

    private static final ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory("ContactPingerThread"));
    
    private final Set<SocketAddress> addresses 
        = new FixedSizeLIFOSet<SocketAddress>(30, EjectionPolicy.FIFO);
    
    private final MojitoDHT dht;
    
    private final long frequency;
    
    private final TimeUnit unit;
    
    private ScheduledFuture<?> future = null;
    
    private DHTFuture<PingEntity> pingFuture = null;
    
    private boolean open = true;
    
    public ContactPinger(MojitoDHT dht) {
        this(dht, DHTSettings.DHT_NODE_ADDER_DELAY.getValue(), TimeUnit.MILLISECONDS);
    }
    
    public ContactPinger(MojitoDHT dht, 
            long frequency, TimeUnit unit) {
        
        this.dht = dht;
        this.frequency = frequency;
        this.unit = unit;
    }
    
    public synchronized void addAddress(SocketAddress address) {
        Objects.nonNull(address, "address");
        
        if (open) {
            addresses.add(address);
            
            if (future == null || future.isDone()) {
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        ping();
                    }
                };
                
                future = EXECUTOR.scheduleWithFixedDelay(
                        task, frequency, frequency, unit);
            }
        }
    }
    
    private synchronized void ping() {
        if (addresses.isEmpty()) {
            future.cancel(true);
            return;
        }
        
        Iterator<SocketAddress> it = addresses.iterator();
        
        SocketAddress address = it.next();
        it.remove();
        
        if (pingFuture != null) {
            pingFuture.cancel(true);
        }
        
        pingFuture = dht.ping(address);
    }
    
    @Override
    public synchronized void close() {
        open = false;
        
        if (future != null) {
            future.cancel(true);
        }
        
        if (pingFuture != null) {
            pingFuture.cancel(true);
        }
        
        addresses.clear();
    }
}

package com.limegroup.gnutella.dht;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.FixedSizeLIFOSet;
import org.limewire.collection.FixedSizeLIFOSet.EjectionPolicy;
import org.limewire.core.settings.DHTSettings;
import org.limewire.mojito.AddressPinger;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.SchedulingUtils;
import org.limewire.util.Objects;

/**
 * The {@link ContactSink} receives {@link SocketAddress}es of active 
 * DHT nodes from the Gnutella Network (or any other source) and tries
 * to ping them.
 */
public class ContactSink implements Closeable {
    
    private final Set<SocketAddress> addresses 
        = new FixedSizeLIFOSet<SocketAddress>(30, EjectionPolicy.FIFO);
    
    private final AddressPinger pinger;
    
    private final long frequency;
    
    private final TimeUnit unit;
    
    private ScheduledFuture<?> future = null;
    
    private DHTFuture<PingEntity> pingFuture = null;
    
    private boolean open = true;
    
    /**
     * Creates a {@link ContactSink} with the given {@link AddressPinger}.
     */
    public ContactSink(AddressPinger pinger) {
        this(pinger, DHTSettings.DHT_NODE_ADDER_DELAY.getTimeInMillis(), 
                TimeUnit.MILLISECONDS);
    }
    
    /**
     * Creates a {@link ContactSink} with the given {@link AddressPinger}.
     */
    public ContactSink(AddressPinger pinger, 
            long frequency, TimeUnit unit) {
        
        this.pinger = pinger;
        this.frequency = frequency;
        this.unit = unit;
    }
    
    /**
     * Returns {@code true} if the {@link ContactSink} is active.
     */
    public synchronized boolean isRunning() {
        return future != null && !future.isDone();
    }
    
    /**
     * Adds the given {@link SocketAddress} to the {@link ContactSink}.
     */
    public synchronized boolean addActiveNode(SocketAddress address) {
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
                
                future = SchedulingUtils.scheduleWithFixedDelay(
                        task, frequency, frequency, unit);
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Sends a DHT ping to the next DHT node in the FIFO queue.
     */
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
        
        long timeout = NetworkSettings.DEFAULT_TIMEOUT.getTimeInMillis();
        pingFuture = pinger.ping(address, timeout, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Stops the {@link ContactSink}.
     * 
     * <p>NOTE: The {@link ContactSink} will start automatically if
     * {@link #addActiveNode(SocketAddress)} is being called!
     */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
        }
        
        if (pingFuture != null) {
            pingFuture.cancel(true);
        }
        
        addresses.clear();
    }
    
    /**
     * Closes the {@link ContactSink}.
     */
    @Override
    public synchronized void close() {
        open = false;
        stop();
    }
}

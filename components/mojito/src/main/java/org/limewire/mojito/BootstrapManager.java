package org.limewire.mojito;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureProcess;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.io.BootstrapConfig;
import org.limewire.mojito.io.BootstrapProcess;
import org.limewire.util.Objects;

/**
 * The {@link BootstrapManager} is managing the bootstrap process.
 */
public class BootstrapManager implements Closeable {
    
    /**
     * The {@link State} of the {@link BootstrapManager}.
     */
    public static enum State {
        /**
         * The initial state (i.e. not booting nor ready).
         */
        UNDEFINED,
        
        /**
         * The {@link DHT} is booting
         */
        BOOTING,
        
        /**
         * The {@link DHT} is ready
         */
        READY;
    }
    
    private final List<EventListener<FutureEvent<BootstrapEntity>>> listeners 
        = new ArrayList<EventListener<FutureEvent<BootstrapEntity>>>();
    
    private final DHT dht;
    
    private State customState = State.UNDEFINED;
    
    private DHTFuture<BootstrapEntity> future = null;
    
    private boolean open = true;
    
    BootstrapManager(DHT dht) {
        this.dht = dht;
    }
    
    /**
     * Changes the internal {@link State} to the given value.
     */
    public synchronized void setCustomState(State customState) {
        this.customState = Objects.nonNull(customState, "state");
    }
    
    /**
     * Returns the internal {@link State}.
     */
    public synchronized State getCustomState() {
        return customState;
    }
    
    /**
     * Returns {@code true} if the {@link DHT} is booting
     */
    public synchronized boolean isBooting() {
        if (!open) {
            return false;
        }
        
        if (customState == State.BOOTING) {
            return true;
        }
        
        if (future != null && !future.isDone()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Returns {@code true} if the {@link DHT} is ready
     */
    public synchronized boolean isReady() {
        if (!open) {
            return false;
        }
        
        if (customState == State.READY) {
            return true;
        }
        
        if (future != null && future.isDone()
                && !future.isCompletedAbnormally()) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public synchronized void close() {
        open = false;
        
        if (future != null) {
            future.cancel(true);
        }
    }
    
    /**
     * Starts the bootstrap process.
     */
    public synchronized DHTFuture<BootstrapEntity> bootstrap(
            BootstrapConfig config, long timeout, TimeUnit unit) {
        
        if (!open) {
            throw new IllegalStateException();
        }
        
        // There can be only one bootstrap process active!
        if (future != null) {
            future.cancel(true);
        }
        
        DHTFutureProcess<BootstrapEntity> process 
            = new BootstrapProcess(dht, config, timeout, unit);
        
        future = dht.submit(process, timeout, unit);
        
        for (EventListener<FutureEvent<BootstrapEntity>> l : listeners) {
            future.addFutureListener(l);
        }
        
        return future;
    }
    
    /**
     * Adds a {@link EventListener} that is automatically added
     * to all {@link DHTFuture}s.
     */
    public synchronized void addFutureListener(
            EventListener<FutureEvent<BootstrapEntity>> l) {
        if (future != null) {
            future.addFutureListener(l);
        }
        
        listeners.add(l);
    }
    
    /**
     * Removes the {@link EventListener}.
     */
    public synchronized void removeFutureListener(
            EventListener<FutureEvent<BootstrapEntity>> l) {
        listeners.remove(l);
    }
}
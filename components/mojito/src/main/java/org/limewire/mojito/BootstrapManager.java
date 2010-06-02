package org.limewire.mojito;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito.concurrent.DHTFutureProcess;
import org.limewire.mojito.concurrent.DHTFuture;
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
         * The initial state (i.e. not ready).
         */
        INIT,
        
        /**
         * The {@link DHT} is booting
         */
        BOOTING,
        
        /**
         * The {@link DHT} is ready
         */
        READY;
    }
    
    private final DHT dht;
    
    private State state = State.INIT;
    
    private DHTFuture<BootstrapEntity> future = null;
    
    private boolean open = true;
    
    BootstrapManager(DHT dht) {
        this.dht = dht;
    }
    
    /**
     * Changes the internal {@link State} to the given value.
     */
    public synchronized void setState(State state) {
        this.state = Objects.nonNull(state, "state");
    }
    
    /**
     * Returns {@code true} if the {@link DHT} is booting
     */
    public synchronized boolean isBooting() {
        if (!open) {
            return false;
        }
        
        if (state == State.BOOTING) {
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
        
        if (state == State.READY) {
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
        return future;
    }
}
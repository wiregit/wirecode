package org.limewire.mojito2;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.io.BootstrapConfig;
import org.limewire.mojito2.io.BootstrapProcess;
import org.limewire.util.Objects;

/**
 * The {@link BootstrapManager} is managing the bootstrap process.
 */
public class BootstrapManager implements Closeable {
    
    /**
     * 
     */
    public static enum State {
        /**
         * The initial state
         */
        INIT,
        
        /**
         * The node is booting
         */
        BOOTING,
        
        /**
         * The node is ready
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
    
    public synchronized void setState(State state) {
        this.state = Objects.nonNull(state, "state");
    }
    
    public synchronized boolean isBooting() {
        if (!open) {
            return false;
        }
        
        if (future != null && !future.isDone()) {
            return true;
        }
        
        return state == State.BOOTING;
    }
    
    public synchronized boolean isReady() {
        if (!open) {
            return false;
        }
        
        if (future != null && future.isDone()
                && !future.isCompletedAbnormally()) {
            return true;
        }
        
        return state == State.READY;
    }
    
    @Override
    public synchronized void close() {
        open = false;
        
        if (future != null) {
            future.cancel(true);
        }
    }
    
    public synchronized DHTFuture<BootstrapEntity> bootstrap(
            BootstrapConfig config, long timeout, TimeUnit unit) {
        
        if (!open) {
            throw new IllegalStateException();
        }
        
        // There can be only one bootstrap process active!
        if (future != null) {
            future.cancel(true);
        }
        
        AsyncProcess<BootstrapEntity> process 
            = new BootstrapProcess(dht, config, timeout, unit);
        
        future = dht.submit(process, timeout, unit);
        return future;
    }
}
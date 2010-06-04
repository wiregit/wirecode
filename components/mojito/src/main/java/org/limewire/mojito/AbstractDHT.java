package org.limewire.mojito;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.FutureEvent;
import org.limewire.listener.EventListener;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureProcess;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.io.BootstrapConfig;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.storage.ValueType;

/**
 * An abstract implementation of {@link DHT}.
 */
public abstract class AbstractDHT implements DHT {
    
    /**
     * The {@link FutureManager} that manages all {@link DHTFuture}s.
     */
    private final FutureManager futureManager 
        = new FutureManager();
    
    /**
     * The {@link BootstrapManager}.
     */
    private final BootstrapManager bootstrapManager 
        = new BootstrapManager(this);
    
    public AbstractDHT() {
        bootstrapManager.addFutureListener(
                new EventListener<FutureEvent<BootstrapEntity>>() {
            @Override
            public void handleEvent(FutureEvent<BootstrapEntity> event) {
                bootstrapped(event);
            }
        });
    }
    
    @Override
    public void close() {
        bootstrapManager.close();
        futureManager.close();
    }
    
    @Override
    public boolean isBooting() {
        return bootstrapManager.isBooting();
    }
    
    @Override
    public boolean isReady() {
        return bootstrapManager.isReady();
    }
    
    /**
     * Returns the {@link BootstrapManager}
     */
    public BootstrapManager getBootstrapManager() {
        return bootstrapManager;
    }
    
    @Override
    public DHTFuture<BootstrapEntity> bootstrap(Contact dst, 
            long timeout, TimeUnit unit) {
        BootstrapConfig config = new BootstrapConfig(dst);
        return bootstrap(config, timeout, unit);
    }
    
    @Override
    public DHTFuture<BootstrapEntity> bootstrap(SocketAddress dst, 
            long timeout, TimeUnit unit) {
        BootstrapConfig config = new BootstrapConfig(dst);
        return bootstrap(config, timeout, unit);
    }
    
    /**
     * Attempts to bootstrap this Node with the given {@link BootstrapConfig}.
     */
    protected DHTFuture<BootstrapEntity> bootstrap(
            BootstrapConfig config, long timeout, TimeUnit unit) {
        return bootstrapManager.bootstrap(config, timeout, unit);
    }
    
    /**
     * A callback method that's called if this Node finished bootstrapping.
     */
    protected void bootstrapped(FutureEvent<BootstrapEntity> event) {
    }
    
    @Override
    public DHTFuture<PingEntity> ping(InetAddress address, int port, 
            long timeout, TimeUnit unit) {
        return ping(new InetSocketAddress(address, port), timeout, unit);
    }

    @Override
    public DHTFuture<PingEntity> ping(String address, int port, 
            long timeout, TimeUnit unit) {
        return ping(new InetSocketAddress(address, port), timeout, unit);
    }
    
    @Override
    public DHTFuture<ValueEntity> get(KUID lookupId, 
            long timeout, TimeUnit unit) {
        
        ValueKey key = ValueKey.createValueKey(
                lookupId, ValueType.ANY);
        
        return get(key, timeout, unit);
    }
    
    @Override
    public <T> DHTFuture<T> submit(DHTFutureProcess<T> process, 
            long timeout, TimeUnit unit) {
        return futureManager.submit(process, timeout, unit);
    }
}

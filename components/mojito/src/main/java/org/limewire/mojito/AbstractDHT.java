package org.limewire.mojito;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.FutureEvent.Type;
import org.limewire.listener.EventListener;
import org.limewire.mojito.concurrent.AsyncProcess;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.entity.BootstrapEntity;
import org.limewire.mojito.entity.PingEntity;
import org.limewire.mojito.entity.StoreEntity;
import org.limewire.mojito.entity.ValueEntity;
import org.limewire.mojito.io.BootstrapConfig;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.storage.DHTValue;
import org.limewire.mojito.storage.DHTValueEntity;
import org.limewire.mojito.storage.DHTValueType;

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
     * 
     */
    protected DHTFuture<BootstrapEntity> bootstrap(
            BootstrapConfig config, long timeout, TimeUnit unit) {
        DHTFuture<BootstrapEntity> future 
            = bootstrapManager.bootstrap(config, timeout, unit);
        
        future.addFutureListener(
                new EventListener<FutureEvent<BootstrapEntity>>() {
            @Override
            public void handleEvent(FutureEvent<BootstrapEntity> event) {
                if (event.getType() == Type.SUCCESS) {
                    bootstrapped(event.getResult());
                }
            }
        });
        
        return future;
    }
    
    /**
     * 
     */
    protected void bootstrapped(BootstrapEntity entity) {
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
        
        EntityKey key = EntityKey.createEntityKey(
                lookupId, DHTValueType.ANY);
        
        return get(key, timeout, unit);
    }
    
    @Override
    public <T> DHTFuture<T> submit(AsyncProcess<T> process, 
            long timeout, TimeUnit unit) {
        return futureManager.submit(process, timeout, unit);
    }
}

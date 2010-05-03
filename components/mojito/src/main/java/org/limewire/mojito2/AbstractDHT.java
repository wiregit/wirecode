package org.limewire.mojito2;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.limewire.mojito2.concurrent.AsyncProcess;
import org.limewire.mojito2.concurrent.DHTFuture;
import org.limewire.mojito2.entity.BootstrapEntity;
import org.limewire.mojito2.entity.PingEntity;
import org.limewire.mojito2.entity.StoreEntity;
import org.limewire.mojito2.entity.ValueEntity;
import org.limewire.mojito2.io.BootstrapConfig;
import org.limewire.mojito2.routing.Contact;
import org.limewire.mojito2.storage.DHTValueEntity;
import org.limewire.mojito2.storage.DHTValueType;
import org.limewire.mojito2.storage.Storable;

/**
 * An abstract implementation of {@link DHT}.
 */
public abstract class AbstractDHT implements DHT {
    
    /**
     * The {@link FutureManager} that manages all {@link DHTFuture}s.
     */
    private final FutureManager futureManager = new FutureManager();
    
    @Override
    public void close() {
        futureManager.close();
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
    protected abstract DHTFuture<BootstrapEntity> bootstrap(
            BootstrapConfig config, long timeout, TimeUnit unit);
    
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
    public DHTFuture<StoreEntity> put(Storable storable, 
            long timeout, TimeUnit unit) {
        
        DHTValueEntity value = DHTValueEntity.createFromStorable(
                this, storable);
        
        return put(value, timeout, unit);
    }
    
    @Override
    public <T> DHTFuture<T> submit(AsyncProcess<T> process, 
            long timeout, TimeUnit unit) {
        return futureManager.submit(process, timeout, unit);
    }
}

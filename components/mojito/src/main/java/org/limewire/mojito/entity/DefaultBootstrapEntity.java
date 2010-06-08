package org.limewire.mojito.entity;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito.DHT;

/**
 * A default implementation of {@link BootstrapEntity}.
 */
public class DefaultBootstrapEntity extends AbstractEntity 
        implements BootstrapEntity {

    private final DHT dht;
    
    public DefaultBootstrapEntity(DHT dht, long time, TimeUnit unit) {
        super(time, unit);
        
        this.dht = dht;
    }

    @Override
    public DHT getDHT() {
        return dht;
    }
}

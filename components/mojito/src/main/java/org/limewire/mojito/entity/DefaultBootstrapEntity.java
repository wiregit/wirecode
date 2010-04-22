package org.limewire.mojito.entity;

import java.util.concurrent.TimeUnit;

import org.limewire.mojito.MojitoDHT2;

public class DefaultBootstrapEntity extends AbstractEntity 
        implements BootstrapEntity {

    private final MojitoDHT2 dht;
    
    public DefaultBootstrapEntity(MojitoDHT2 dht, long time, TimeUnit unit) {
        super(time, unit);
        
        this.dht = dht;
    }

    @Override
    public MojitoDHT2 getDHT() {
        return dht;
    }
}

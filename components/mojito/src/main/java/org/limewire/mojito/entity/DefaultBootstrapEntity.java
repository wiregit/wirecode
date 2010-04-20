package org.limewire.mojito.entity;

import java.util.concurrent.TimeUnit;

public class DefaultBootstrapEntity extends AbstractEntity 
        implements BootstrapEntity {

    public DefaultBootstrapEntity(long time, TimeUnit unit) {
        super(time, unit);
    }
}

package org.limewire.mojito.entity;

import java.util.concurrent.TimeUnit;

public class DefaultStoreEntity extends AbstractEntity implements StoreEntity {

    public DefaultStoreEntity(long time, TimeUnit unit) {
        super(time, unit);
    }
}

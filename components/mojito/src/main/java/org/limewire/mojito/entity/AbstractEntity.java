package org.limewire.mojito.entity;

import java.util.concurrent.TimeUnit;

public class AbstractEntity implements Entity {

    private final long time;
    
    private final TimeUnit unit;
    
    public AbstractEntity(long time, TimeUnit unit) {
        this.time = time;
        this.unit = unit;
    }
    
    @Override
    public long getTime(TimeUnit unit) {
        return unit.convert(time, this.unit);
    }

    @Override
    public long getTimeInMillis() {
        return getTime(TimeUnit.MILLISECONDS);
    }
}

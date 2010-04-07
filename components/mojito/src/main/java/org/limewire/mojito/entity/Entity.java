package org.limewire.mojito.entity;

import java.util.concurrent.TimeUnit;

/**
 * 
 */
public interface Entity {

    /**
     * 
     */
    public long getTime(TimeUnit unit);
    
    /**
     * 
     */
    public long getTimeInMillis();
}

package org.limewire.mojito2.entity;

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

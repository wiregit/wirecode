package org.limewire.mojito.entity;

import java.util.concurrent.TimeUnit;

/**
 * An {@link Entity} is the result of a DHT operation.
 */
public interface Entity {

    /**
     * The amount of time it took to process the request.
     */
    public long getTime(TimeUnit unit);
    
    /**
     * The amount of time it took to process the request in milliseconds.
     */
    public long getTimeInMillis();
}

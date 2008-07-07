package org.limewire.swarm.http;

import org.apache.http.protocol.HttpContext;

/**
 * Shared constants data stored in an {@link HttpContext} for swarm downloads.
 */
public interface SwarmExecutionContext {
    
    public static final String HTTP_AVAILABLE_RANGES = "swarm.http.availableRanges";
    public static final String HTTP_SWARM_SOURCE = "swarm.http.source";

}

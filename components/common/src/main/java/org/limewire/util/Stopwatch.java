package org.limewire.util;

import org.apache.commons.logging.Log;

public class Stopwatch {
    
    private final Log log;
    private long start = System.currentTimeMillis();
    
    public Stopwatch(Log log) {
        this.log = log;
    }

    /**
     * Resets and returns elapsed time in milliseconds.
     */
    public long reset() {
        if(log.isTraceEnabled()) {
            long now = System.currentTimeMillis();
            long elapsed = now - start;
            start = now;
            return elapsed;
        } else {
            return -1;
        }
        
    }

    /**
     * Resets and logs elapsed time in milliseconds.
     */
    public void resetAndLog(String label) {
        if(log.isTraceEnabled())
            log.trace(label + ": " + reset() + "ms");
    }
}

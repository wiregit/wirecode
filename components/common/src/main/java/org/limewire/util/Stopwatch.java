package org.limewire.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;

public class Stopwatch {
    
    private final Log log;
    private long start = System.nanoTime();
    
    public Stopwatch(final Log log) {
        this.log = log;
        final CountDownLatch threadStarted = new CountDownLatch(1);
        
        // start the timer interrupt hack earlier.  Make sure this is removed
        // once the startup times are improved.
        Thread timeHack = new Thread() {
            public void run() {
                resetAndLog("enabling timehack");
                threadStarted.countDown();
                try {
                    Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException bleh){}
            }
        };
        timeHack.start();
        try {
            threadStarted.await();
            // sleep some extra to make sure
            // the time hack is engaged.
            Thread.sleep(100); 
        } catch (InterruptedException ignore){}
    }

    /**
     * Resets and returns elapsed time in milliseconds.
     */
    public synchronized long reset() {
        if(log.isTraceEnabled()) {
            long now = System.nanoTime();
            long elapsed = now - start;
            start = now;
            return TimeUnit.NANOSECONDS.toMillis(elapsed);
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

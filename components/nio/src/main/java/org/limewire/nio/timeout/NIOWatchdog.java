package org.limewire.nio.timeout;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.NIODispatcher;
import org.limewire.nio.observer.Shutdownable;

public class NIOWatchdog {

    private static final Log LOG = LogFactory.getLog(NIOWatchdog.class);

    // public for testing
    public static int DEFAULT_DELAY_TIME = 1000 * 60 * 2; // 2 minutes

    private final Runnable runner;

    private long delay;

    private final NIODispatcher dispatcher;

    private ScheduledFuture<?> future;

    public NIOWatchdog(NIODispatcher dispatcher, Runnable runner, long delay) {
        this.dispatcher = dispatcher;
        this.runner = runner;
        this.delay = delay;
    }

    public NIOWatchdog(NIODispatcher dispatcher, Shutdownable shutdownable,
            long delay) {
        this.dispatcher = dispatcher;
        this.runner = new Closer(shutdownable);
        this.delay = delay;
    }

    public NIOWatchdog(NIODispatcher dispatcher, Shutdownable shutdownable) {
        this(dispatcher, shutdownable, DEFAULT_DELAY_TIME);
    }

    public synchronized void activate() {
        deactivate();
        future = dispatcher.getScheduledExecutorService().schedule(runner,
                delay, TimeUnit.MILLISECONDS);
    }

    public synchronized void deactivate() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    public synchronized void setDelay(long delay) {
        this.delay = delay;
    }
    
    public synchronized long getDelay() {
        return delay;
    }
    
    private class Closer implements Runnable {

        private final Shutdownable shutdownable;

        private Closer(Shutdownable shutdownable) {
            this.shutdownable = shutdownable;
        }

        public void run() {
            if (LOG.isDebugEnabled())
                LOG.debug("STALLED! Killing: " + shutdownable);
            shutdownable.shutdown();
        }
    }

}

package com.limegroup.gnutella.dht.db;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.ExecutorsHelper;

/**
 * 
 */
public abstract class Publisher implements Closeable {

    /**
     * 
     */
    private static final ScheduledExecutorService EXECUTOR 
        = Executors.newSingleThreadScheduledExecutor(
            ExecutorsHelper.defaultThreadFactory(
                "PublisherThread"));
    
    protected final long frequency;
    
    protected final TimeUnit unit;
    
    private ScheduledFuture<?> future = null;
    
    private boolean open = true;
    
    /**
     * Creates a {@link Publisher} with the given arguments.
     */
    public Publisher(long frequency, TimeUnit unit) {
        this.frequency = frequency;
        this.unit = unit;
    }
    
    /**
     * Returns the publishing frequency in the given {@link TimeUnit}.
     */
    public long getFrequency(TimeUnit unit) {
        return unit.convert(frequency, this.unit);
    }
    
    /**
     * Returns the publishing frequency in milliseconds.
     */
    public long getFrequencyInMillis() {
        return getFrequency(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns true if the {@link Publisher} is open.
     */
    public synchronized boolean isOpen() {
        return open;
    }
    
    /**
     * Returns true if the {@link Publisher} is running.
     */
    public synchronized boolean isRunning() {
        return open && future != null && !future.isDone();
    }
    
    /**
     * Starts the {@link Publisher}
     */
    public synchronized void start() {
        if (!open) {
            throw new IllegalStateException();
        }
        
        if (isRunning()) {
            return;
        }
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                publish();
            }
        };
        
        future = EXECUTOR.scheduleWithFixedDelay(
                task, frequency, frequency, unit);
    }
    
    /**
     * Stops the {@link Publisher}
     */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(true);
        }
    }
    
    @Override
    public synchronized void close() {
        open = false;
        stop();
    }
    
    /**
     * 
     */
    protected abstract void publish();
}

package org.limewire.mojito.concurrent2;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 
 */
public class AsyncExchanger<V, E extends Throwable> {

    private static final Object THIS = new Object();
    
    private final Object lock;
    
    private V value;
    
    private E exception;
    
    private boolean done = false;
    
    private boolean cancelled = false;
    
    /**
     * 
     */
    public AsyncExchanger() {
        this(THIS);
    }
    
    /**
     * 
     */
    public AsyncExchanger(Object lock) {
        if (lock == null) {
            throw new NullArgumentException("lock");
        }
        
        this.lock = (lock != THIS ? lock : this);
    }
    
    /**
     * 
     */
    public boolean setValue(V value) {
        synchronized (lock) {
            if (done || cancelled) {
                return false;
            }
            
            this.value = value;
            this.done = true;
            
            lock.notifyAll();
            return true;
        }
    }
    
    /**
     * 
     */
    public boolean setException(E exception) {
        if (exception == null) {
            throw new NullArgumentException("exception");
        }
        
        synchronized (lock) {
            if (done || cancelled) {
                return false;
            }
            
            this.exception = exception;
            this.done = true;
            
            lock.notifyAll();
            return true;
        }
    }
    
    /**
     * 
     */
    public boolean cancel() {
        synchronized (lock) {
            if (done) {
                return cancelled;
            }
            
            done = true;
            cancelled = true;
            
            lock.notifyAll();
            return true;
        }
    }
    
    /**
     * 
     */
    public V get() throws InterruptedException, E {
        try {
            return get(0L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new Error("TimeoutException", e);
        }
    }
    
    /**
     * 
     */
    public V get(long timeout, TimeUnit unit) 
            throws InterruptedException, TimeoutException, E {
        
        synchronized (lock) {
            if (!done) {
                if (timeout == 0L) {
                    lock.wait();
                } else {
                    unit.timedWait(lock, timeout);
                }
                
                if (!done) {
                    throw new TimeoutException();
                }
            }
            
            if (cancelled) {
                throw new CancellationException();
            }
            
            if (exception != null) {
                throw exception;
            }
            
            return value;
        }
    }
    
    /**
     * 
     */
    public V tryGet() throws InterruptedException, E {
        synchronized (lock) {
            if (done) {
                return get();
            }
            
            return null;
        }
    }
    
    /**
     * 
     */
    public boolean isDone() {
        synchronized (lock) {
            return done || cancelled;
        }
    }
    
    /**
     * 
     */
    public boolean isCancelled() {
        synchronized (lock) {
            return cancelled;
        }
    }
    
    /**
     * 
     */
    public boolean isCompletedAbnormally() {
        synchronized (lock) {
            return cancelled || exception != null;
        }
    }
}

package org.limewire.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@link AsyncFuture} that implements the {@link Runnable} interface.
 * 
 * @see AsyncValueFuture
 * @see FutureTask
 */
public class AsyncFutureTask<V> extends AsyncValueFuture<V> 
        implements RunnableAsyncFuture<V> {
    
    private final AtomicReference<Interruptible> thread 
        = new AtomicReference<Interruptible>(Interruptible.INIT);
    
    private final Callable<V> callable;
    
    /**
     * Creates an {@link AsyncFutureTask} with the given 
     * {@link Runnable} and result value
     */
    public AsyncFutureTask(Runnable task, V result) {
        this(Executors.callable(task, result));
    }
    
    /**
     * Creates an {@link AsyncFutureTask} with the given {@link Callable}
     */
    public AsyncFutureTask(Callable<V> callable) {
        if (callable == null) {
            throw new NullPointerException("callable");
        }
        
        this.callable = callable;
    }

    @Override
    public final void run() {
        if (thread.compareAndSet(Interruptible.INIT, new CurrentThread())) {
            try {
                doRun();
            } finally {
                thread.set(Interruptible.DONE);
            }
        }
    }
    
    /**
     * 
     */
    protected void doRun() {
        try {
            V value = callable.call();
            setValue(value);
        } catch (Exception err) {
            setException(err);
        }
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean success = super.cancel(mayInterruptIfRunning);
        
        if (success && mayInterruptIfRunning) {
            thread.getAndSet(Interruptible.DONE).interrupt();
        }
        
        return success;
    }
    
    private static interface Interruptible {
        
        public static final Interruptible INIT = new Interruptible() {
            @Override
            public void interrupt() {
            }
        };
        
        public static final Interruptible DONE = new Interruptible() {
            @Override
            public void interrupt() {
            }
        };
        
        public void interrupt();
    }
    
    private static class CurrentThread implements Interruptible {
        
        private final Thread currentThread = Thread.currentThread();
        
        @Override
        public void interrupt() {
            currentThread.interrupt();
        }
    }
}

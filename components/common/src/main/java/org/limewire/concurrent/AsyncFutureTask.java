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
    
    /**
     * We use this {@link AtomicReference} to manage the {@link Thread}
     * that is executing this {@link AsyncFutureTask}.
     * 
     * NOTE: It doesn't have to be an {@link AtomicReference}. We could
     * use a plain handle but this is very convenient.
     */
    private final AtomicReference<Interruptible> thread 
        = new AtomicReference<Interruptible>(Interruptible.INIT);
    
    /**
     * The {@link Callable} that will provide the result value
     * or throw an {@link Exception}.
     */
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
        if (preRun()) {
            try {
                doRun();
            } finally {
                postRun();
            }
        }
    }
    
    /**
     * Called before {@link #doRun()} to initialize the 
     * current {@link Thread}. Returns true upon success.
     */
    private boolean preRun() {
        synchronized (thread) {
            return thread.compareAndSet(Interruptible.INIT, new CurrentThread());
        }
    }
    
    /**
     * Called after {@link #doRun()} to cleanup the current {@link Thread}.
     */
    private void postRun() {
        synchronized (thread) {
            thread.set(Interruptible.DONE);
        }
    }
    
    /**
     * The actual implementation of the {@link AsyncFutureTask}'s 
     * run method.
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
            synchronized (thread) {
                thread.getAndSet(Interruptible.DONE).interrupt();
            }
        }
        
        return success;
    }

    /**
     * An interface for interruptible objects such as {@link Thread}s
     */
    private static interface Interruptible {
        
        /**
         * An initial value for {@link AtomicReference}s that does nothing but 
         * allows us to use {@link AtomicReference#compareAndSet(Object, Object)}.
         */
        public static final Interruptible INIT = new Interruptible() {
            @Override
            public void interrupt() {
            }
        };
        
        /**
         * An final value for {@link AtomicReference}s that does nothing but 
         * allows us to use {@link AtomicReference#compareAndSet(Object, Object)}.
         */
        public static final Interruptible DONE = new Interruptible() {
            @Override
            public void interrupt() {
            }
        };
        
        /**
         * Interrupt!
         */
        public void interrupt();
    }
    
    /**
     * An implementation of {@link Interruptible} that holds a reference
     * to the {@link Thread} (as returned by {@link Thread#currentThread()})
     * the created it.
     */
    private static class CurrentThread implements Interruptible {
        
        private final Thread currentThread = Thread.currentThread();
        
        @Override
        public void interrupt() {
            currentThread.interrupt();
        }
    }
}

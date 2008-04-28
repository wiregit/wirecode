package org.limewire.concurrent;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.service.ErrorService;



/**
 * An extension for {@link Timer}, allowing you to schedule a {@link Runnable} task instead
 * of scheduling a {@link TimerTask}.
 * 
 * This also exposes all the functionality of a {@link ScheduledExecutorService}.
 */
public class SimpleTimer extends AbstractExecutorService implements ScheduledExecutorService {
            
    /** The underlying Timer of this SimpleTimer. */
    private final Timer TIMER;
    
    /** Whether or not we actively cancelled the timer. */
    private volatile boolean cancelled = false;
    
    /**
     * Creates a new active SimpleTimer with a callback for internal errors.
     * @param isDaemon true if this' thread should be a daemon.
     */
    public SimpleTimer(boolean isDaemon) {
        TIMER = new Timer(isDaemon);
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        ScheduledTimerTask<?> future = new ScheduledTimerTask<Object>(command);
        scheduleInternal(future, unit.toMillis(delay), 0, false);
        return future;
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        ScheduledTimerTask<V> future = new ScheduledTimerTask<V>(callable);
        scheduleInternal(future, unit.toMillis(delay), 0, false);
        return future;
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        ScheduledTimerTask<?> future = new ScheduledTimerTask<Object>(command);
        scheduleInternal(future, unit.toMillis(initialDelay), unit.toMillis(period), true);
        return future;
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        ScheduledTimerTask<?> future = new ScheduledTimerTask<Object>(command);
        scheduleInternal(future, unit.toMillis(initialDelay), unit.toMillis(delay), false);
        return future;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    public boolean isShutdown() {
        return cancelled;
    }

    public boolean isTerminated() {
        return cancelled;
    }

    public void shutdown() {
        cancelled = true;
        TIMER.cancel();
    }

    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    // Does not use ScheduledTimerTask so as to avoid creating the Future.
    public void execute(final Runnable command) {
        TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                try {
                    command.run();
                } catch(Throwable t) {
                    ErrorService.error(t);
                }
            }
        };
        scheduleInternal(tt, 0, 0, false);
    }
    
    /** Schedules the task as necessary. */
    private void scheduleInternal(TimerTask task, long delay, long period, boolean fixedRate) {
        try {
            if(period == 0) {
                if(fixedRate) {
                    throw new IllegalArgumentException("cannot support 0 period w/ fixedRate");
                } else {
                    TIMER.schedule(task, delay);
                }
            } else {
                if(fixedRate) {
                    TIMER.scheduleAtFixedRate(task, delay, period);
                } else {
                    TIMER.schedule(task, delay, period);
                }
            }
        } catch(IllegalStateException ise) {
            if(cancelled)
                throw ise;
        }
    }

    /** A TimerTask that delegates to a FutureTask for a ScheduledFuture. */
    private static class ScheduledTimerTask<V> extends TimerTask implements ScheduledFuture<V> {
        private final ResetableFutureTask<V> task;
        
        public ScheduledTimerTask(final Runnable r) {
            task = new ResetableFutureTask<V>(new Runnable() {
                public void run() {
                    try {
                        r.run();
                    } catch(Throwable t) {
                        ErrorService.error(t);
                    }
                }
            }, null);
        }
        
        public ScheduledTimerTask(final Callable<V> c) {
            task = new ResetableFutureTask<V>(new Callable<V>() {
                public V call() {
                    try {
                        V v = c.call();
                        return v;
                    } catch(Throwable t) {
                        ErrorService.error(t);
                        return null;
                    }
                }
            });
        }

        @Override
        public void run() {
            task.runAndReset();            
        }

        public long getDelay(TimeUnit unit) {
            return -1;
        }

        public int compareTo(Delayed o) {
            return 0;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            task.cancel(true);
            return cancel();
        }

        public V get() throws InterruptedException, ExecutionException {
            return task.get();
        }

        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return task.get(timeout, unit);
        }

        public boolean isCancelled() {
            return task.isCancelled();
        }

        public boolean isDone() {
            return task.isDone();
        }
    }
    
    /** A FutureTask that allows someone to call runAndReset. */
    private static class ResetableFutureTask<V> extends FutureTask<V> {
        @Override
        public boolean runAndReset() {
            return super.runAndReset();
        }

        public ResetableFutureTask(Runnable runnable, V result) {
            super(runnable, result);
        }

        public ResetableFutureTask(Callable<V> callable) {
            super(callable);
        }
        
    }
    
}
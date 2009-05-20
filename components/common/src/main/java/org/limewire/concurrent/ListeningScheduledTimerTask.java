package org.limewire.concurrent;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.limewire.listener.EventListener;
import org.limewire.service.ErrorService;

/** A TimerTask that delegates to a ListeningRunnableFuture for a ListeningScheduledFuture. */
class ListeningScheduledTimerTask<V> extends TimerTask implements
        ScheduledListeningFuture<V> {
    private final RunnableListeningFuture<V> task;

    public ListeningScheduledTimerTask(final Runnable r, long period) {
        task = new ListeningResetableFutureTask<V>(new Runnable() {
            public void run() {
                try {
                    r.run();
                } catch(RuntimeException e) {
                    ErrorService.error(e);
                    throw e;
                } catch(Error e) {
                    ErrorService.error(e);
                    throw e;
                } catch(Exception e) {
                    ErrorService.error(e);
                    throw new UndeclaredThrowableException(e);
                }
            }
        }, null, period != 0);
    }

    public ListeningScheduledTimerTask(final Callable<V> c, long period) {
        task = new ListeningResetableFutureTask<V>(new Callable<V>() {
            public V call() {
                try {
                    return c.call();
                } catch(RuntimeException e) {
                    ErrorService.error(e);
                    throw e;
                } catch(Error e) {
                    ErrorService.error(e);
                    throw e;
                } catch(Exception e) {
                    ErrorService.error(e);
                    throw new UndeclaredThrowableException(e);
                }
            }
        }, period != 0);
    }

    @Override
    public void run() {
        task.run();
    }

    public long getDelay(TimeUnit unit) {
        return -1;
    }

    public int compareTo(Delayed o) {
        return 0;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        cancel();
        return task.cancel(mayInterruptIfRunning);
    }

    public V get() throws InterruptedException, ExecutionException {
        return task.get();
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        return task.get(timeout, unit);
    }

    public boolean isCancelled() {
        return task.isCancelled();
    }

    public boolean isDone() {
        return task.isDone();
    }

    @Override
    public void addFutureListener(EventListener<FutureEvent<V>> listener) {
        task.addFutureListener(listener);
    }
}
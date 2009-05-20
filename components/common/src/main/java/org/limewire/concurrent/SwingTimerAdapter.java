package org.limewire.concurrent;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.Timer;

import org.limewire.listener.EventListener;
import org.limewire.service.ErrorService;

class SwingTimerAdapter<V> extends Timer implements ScheduledListeningFuture<V> {
    
    private final RunnableListeningFuture<V> task;

    public SwingTimerAdapter(final Runnable r, int period) {
        super(period, null);
        
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
        
        addActionListener(new ExecutorActionListener());
    }

    public SwingTimerAdapter(final Callable<V> c, int period) {
        super(period, null);
        
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
        
        addActionListener(new ExecutorActionListener());
    }

    private class ExecutorActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            task.run();
        }
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
        return -1;
    }

    @Override
    public int compareTo(Delayed o) {
        return 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        assert EventQueue.isDispatchThread();
        stop();
        return task.cancel(mayInterruptIfRunning);
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        assert !EventQueue.isDispatchThread();
        return task.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        assert !EventQueue.isDispatchThread();
        return task.get(timeout, unit);
    }

    @Override
    public boolean isCancelled() {
        assert EventQueue.isDispatchThread();
        return task.isCancelled();
    }

    @Override
    public boolean isDone() {
        assert EventQueue.isDispatchThread();
        return task.isDone();
    }

    @Override
    public void addFutureListener(EventListener<FutureEvent<V>> listener) {
        assert EventQueue.isDispatchThread();
        task.addFutureListener(listener);
    }
}

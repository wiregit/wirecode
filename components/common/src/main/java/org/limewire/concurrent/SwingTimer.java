package org.limewire.concurrent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

/**
 * An extension for {@link Timer}, allowing you to schedule a {@link Runnable}
 * task instead of scheduling a {@link TimerTask}.
 * 
 * This also exposes all the functionality of a {@link ScheduledListeningExecutorService}.
 */
public class SwingTimer extends AbstractListeningExecutorService implements
        ScheduledListeningExecutorService {

    /** Whether or not we actively cancelled the timer. */
    private volatile boolean cancelled = false;

    /**
     * List of the active Timers.
     */
    private final List<SwingTimerAdapter<?>> scheduledTasks
        = Collections.synchronizedList(new LinkedList<SwingTimerAdapter<?>>());
    
    
    /**
     * Constructs the SwingTimer.
     */
    public SwingTimer() {
    }
    
    @Override
    public ScheduledListeningFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduleInternal(command, null, unit.toMillis(delay), 0, false);
    }

    @Override
    public <V> ScheduledListeningFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return scheduleInternal(null, callable, unit.toMillis(delay), 0, false);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
            TimeUnit unit) {
        return scheduleInternal(command, null, unit.toMillis(initialDelay), unit.toMillis(period), true);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
            long delay, TimeUnit unit) {
        return scheduleInternal(command, null, unit.toMillis(initialDelay), unit.toMillis(delay), false);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShutdown() {
        return cancelled;
    }

    @Override
    public boolean isTerminated() {
        return cancelled;
    }

    @Override
    public void shutdown() {
        cancelled = true;
        synchronized (scheduledTasks) {
            for ( SwingTimerAdapter<?> task : scheduledTasks ) {
                task.cancel(true);
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    // Does not use ScheduledTimerTask so as to avoid creating the Future.
    @Override
    public void execute(Runnable command) {
        scheduleInternal(command, null, 0, 0, false);
    }

    /** Schedules the task as necessary. */
    private <V> ScheduledListeningFuture<V> scheduleInternal(Runnable runnable, Callable<V> callable, long delay, long period, boolean fixedRate) {

        if (fixedRate) {
            throw new UnsupportedOperationException("SwingTimer does not supported FixedRate mode");
        }
        
        if (delay > Integer.MAX_VALUE || period > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("SwingTimer only supports delay/period with INTEGERS" +
            		"zero and above. (possible long to int overflow)");
        }    
        
        final SwingTimerAdapter<V> timer;
        
        if (runnable != null) {
            timer = new SwingTimerAdapter<V>(runnable, (int)period);
        }
        else if (callable != null) {
            timer = new SwingTimerAdapter<V>(callable, (int)period);
        } 
        else {
            throw new IllegalArgumentException("Must provide a Runnable or Callable to scheduleInternal");
        }
            
        if (period == 0) {
            timer.setRepeats(false);
            
            timer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    scheduledTasks.remove(timer);
                }
            });
            
        } else {
            timer.setInitialDelay((int)delay);
        }
        
        scheduledTasks.add(timer);
                
        timer.start();
        
        return timer;
    }
    
}
package org.limewire.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/** 
 * Defines the interface for a class to schedule tasks to execute immediately or
 * after a delay; <code>SchedulingThreadPool</code> is a bare bones of
 * {@link ScheduledExecutorService}.
 */
public interface SchedulingThreadPool {
    /** Invokes the task as soon as possible. */
    public void invokeLater(Runnable r);
    /** Invokes the task after the given delay. */
	public Future invokeLater(Runnable r, long delay);
}

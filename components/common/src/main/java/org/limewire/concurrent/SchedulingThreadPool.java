package org.limewire.concurrent;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

/** 
 *                      
 * Defines a simplified {@link ScheduledExecutorService} to schedule tasks to 
 * execute immediately or after a delay.
 */
public interface SchedulingThreadPool {
    /** Invokes the task as soon as possible. */
    public void invokeLater(Runnable r);
    /** Invokes the task after the given delay. */
	public Future invokeLater(Runnable r, long delay);
}

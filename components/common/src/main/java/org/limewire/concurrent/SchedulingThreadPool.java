package org.limewire.concurrent;

import java.util.concurrent.Future;

/** A bare-boned version of ScheduledExecutorService */
public interface SchedulingThreadPool {
    /** Invokes the task as soon as possible. */
    public void invokeLater(Runnable r);
    /** Invokes the task after the given delay. */
	public Future invokeLater(Runnable r, long delay);
}

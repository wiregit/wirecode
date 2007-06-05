package org.limewire.collection;

import java.util.concurrent.Future;

import org.limewire.concurrent.SchedulingThreadPool;


/**
 * A utility to schedule, reschedule and cancel the execution of 
 * a task.
 */
public class Periodic {

	private final SchedulingThreadPool scheduler;
	private final Delegate d;
	
	private long nextExecuteTime;
	private Future future;
	
	/**
	 * Creates a periodic task 
	 * @param r the <tt>Runnable</tt> to execute
	 * @param scheduler the <tt>SchedulingThreadPool</tt> to schedule
	 * execution on.
	 */
	public Periodic(Runnable r,
			SchedulingThreadPool scheduler) {
		this.d = new Delegate(r);
		this.scheduler = scheduler;
	}
	
	/**
	 * changes the execution time of this Periodic task if it is
	 * later than the current execution time.
	 * 
	 * Note: some implementations of <tt>SchedulingThreadPool</tt> use nanoseconds
	 * as their time unit, so do not schedule anything for more than 292 years in the
	 * future. More practically, this means you should not use Long.MAX_VALUE as parameter.
	 * 
	 * @param newDelay the new delay from now when this should be executed
	 * @return true if the execution time changed
	 */
	public synchronized boolean rescheduleIfLater(long newDelay) {
		long newTime = System.currentTimeMillis() + newDelay;
		if (future == null) { 
			nextExecuteTime = newTime;
			if (newDelay > 0) 
				future = scheduler.invokeLater(d,newDelay);
			else
				scheduler.invokeLater(d);
			return true;
		} else if (newTime > nextExecuteTime) {
			nextExecuteTime = newTime;
			return true;
		}
		return false;
	}
	
	/**
	 * changes the execution time of this Periodic task if it is
	 * sooner than the current execution time.
	 * @param newDelay the new delay from now when this should be executed
	 * @return true if the execution time changed
	 */
	public synchronized boolean rescheduleIfSooner(long newDelay) {
		// if not scheduled at all, just schedule
		if (future == null) 
			return rescheduleIfLater(newDelay);
		
		long newTime = System.currentTimeMillis() + newDelay;
		if (newTime < nextExecuteTime) {
			future.cancel(false);
			nextExecuteTime = newTime;
			if (newDelay > 0)
				future = scheduler.invokeLater(d, newDelay);
			else { 
				future = null;
				scheduler.invokeLater(d);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Cancels any scheduled execution of the task.
	 */
	public synchronized void unschedule() {
		if (future != null) {
			future.cancel(true);
			future = null;
			nextExecuteTime = -1;
		}
	}
	
	private class Delegate implements Runnable {
		private final Runnable r;
		Delegate(Runnable r) {
			this.r = r;
		}
		
		public void run() {
			synchronized(Periodic.this) {
				future = null;
				
				long now = System.currentTimeMillis();
				if (now < nextExecuteTime) {
					future = scheduler.invokeLater(this, nextExecuteTime - now);
					return;
				}
			}
			r.run();
		}
	}
	
	protected Runnable getRunnable() {
		return d.r;
	}

}

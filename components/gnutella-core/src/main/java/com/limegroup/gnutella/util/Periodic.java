package com.limegroup.gnutella.util;


public class Periodic {

	private final long delay;
	private final SchedulingThreadPool scheduler;
	private final Delegate d;
	
	private long nextExecuteTime;
	private boolean scheduled;
	public Periodic(Runnable r,
			long delay, 
			SchedulingThreadPool scheduler) {
		this.d = new Delegate(r);
		this.delay = delay;
		this.scheduler = scheduler;
	}
	
	public synchronized void schedule() {
		schedule(delay);
	}
	
	public synchronized void schedule(long customDelay) {
		nextExecuteTime = System.currentTimeMillis() + customDelay;
		
		if (!scheduled) {
			scheduler.invokeLater(d,customDelay);
			scheduled = true;
		}
	}
	
	public synchronized void unschedule() {
		nextExecuteTime = -1;
	}
	
	private class Delegate implements Runnable {
		private final Runnable r;
		Delegate(Runnable r) {
			this.r = r;
		}
		
		public void run() {
			synchronized(Periodic.this) {
				scheduled = false;
				if (nextExecuteTime < 0)
					return;
				
				long now = System.currentTimeMillis();
				if (now < nextExecuteTime) {
					scheduler.invokeLater(this, nextExecuteTime - now);
					scheduled = true;
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

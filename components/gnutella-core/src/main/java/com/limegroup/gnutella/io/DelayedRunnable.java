package com.limegroup.gnutella.io;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class DelayedRunnable<T> implements Runnable, Delayed, java.util.concurrent.Future<T> {
	
	
	private static final AtomicLong sequencer = new AtomicLong(0);
	
	private static final long NANO_BASE = System.nanoTime();
	
	private volatile Runnable delegate;
	private final long time, nanoTime;
	private final long sequenceNumber;
	private volatile boolean executed;
	DelayedRunnable(Runnable delegate, long time) {
		this.delegate = delegate;
		this.time = time;
		sequenceNumber = sequencer.getAndDecrement();
		this.nanoTime = System.nanoTime() - NANO_BASE + time * 1000 * 1000;
	}
	
	public void run() {
		Runnable r = delegate;
		if (r == null)
			return;
		try {
			r.run();
		} finally {
			executed = true;
		}
		
	}
	
	public boolean cancel(boolean ignored) {
		delegate = null;
		return true;
	}
	
	public boolean isCancelled() {
		return delegate == null;
	}
	
	public boolean isDone() {
		return executed;
	}
	
	public T get() {
		return null;
	}
	
	public T get(long time, TimeUnit ignored) {
		return null;
	}
	
	public long getDelay(TimeUnit arg0) {
		if (arg0 == TimeUnit.MILLISECONDS)
			return time - System.currentTimeMillis();
		else if (arg0 == TimeUnit.NANOSECONDS)
			return nanoTime - System.nanoTime() + NANO_BASE;
		else 
			throw new IllegalArgumentException();
	}
	
	public int compareTo(Delayed other) {
		if (other == this) 
			return 0;
		DelayedRunnable x = (DelayedRunnable)other;
		long diff = nanoTime - x.nanoTime;
		if (diff < 0)
			return -1;
		else if (diff > 0)
			return 1;
		else if (sequenceNumber < x.sequenceNumber)
			return -1;
		else
			return 1;
	}
	
}


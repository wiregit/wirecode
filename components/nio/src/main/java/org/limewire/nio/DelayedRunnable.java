package org.limewire.nio;

import java.util.concurrent.Delayed;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class DelayedRunnable<T> implements Runnable, Delayed, Future<T> {
	
	
	private static final AtomicLong sequencer = new AtomicLong(0);
	
	private static final long NANO_BASE = System.nanoTime();
	
	private Runnable delegate;
	private final long nanoTime;
	private final long sequenceNumber;
	private volatile boolean executed;
	private boolean executing;
    
	DelayedRunnable(Runnable delegate, long time) {
		this.delegate = delegate;
		sequenceNumber = sequencer.getAndIncrement();
		this.nanoTime = System.nanoTime() - NANO_BASE + time * 1000 * 1000;
	}
	
	public void run() {
		if (isDone())
			return;
		synchronized(this){
			if (delegate == null)
				return;
			executing = true;
		}
		try {
			delegate.run();
		} finally {
			executed = true;
		}
	}
	
	public synchronized boolean cancel(boolean ignored) {
		if (executing)
			return false;
		delegate = null;
		return true;
	}
	
	public synchronized boolean isCancelled() {
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
		if (arg0 == TimeUnit.MILLISECONDS) {
			// round off to the next or previous millisecond
			long nanoDelay = getDelay(TimeUnit.NANOSECONDS);
			long milliDelay = nanoDelay / 1000000;
			milliDelay += (nanoDelay > 0 ? 1 : -1) *
			 (nanoDelay % 1000000 == 0 ? 0 : 1);
			return milliDelay;
		} else if (arg0 == TimeUnit.NANOSECONDS)
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


package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * A wrapper for FutureTask that facilitates keeping track of a collection of
 * tasks.
 */
public class StoredFutureTask<V> extends FutureTask<V> {
	
	/** The collection where to store this task */
	private final Collection collection;

	public StoredFutureTask(Callable<V> arg0, Collection collection) {
		super(arg0);
		this.collection = collection;
		if (collection != null)
			collection.add(this);
	}

	public StoredFutureTask(Runnable arg0, V arg1, Collection collection) {
		super(arg0, arg1);
		this.collection = collection;
		if (collection != null)
			collection.add(this);
	}
	
	public void run() {
		try {
			super.run();
		} finally {
			removeFromCollection();
		}
	}
	
	public boolean cancel(boolean arg0) {
		try {
			return super.cancel(arg0);
		} finally {
			removeFromCollection();
		}
	}
	
	private void removeFromCollection() {
		if (collection != null)
			collection.remove(this);
	}
}

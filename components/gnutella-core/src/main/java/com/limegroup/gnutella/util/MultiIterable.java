package com.limegroup.gnutella.util;

import java.util.Iterator;

public class MultiIterable<T> implements Iterable<T> {
	
	private final Iterable<T> []iterables;

	public MultiIterable(Iterable<T>... iterables) {
		this.iterables = iterables; 
	}
	
	@SuppressWarnings("unchecked")
	public Iterator<T> iterator() {
		Iterator<T> []iterators = new Iterator[iterables.length];
		for (int i = 0; i < iterables.length; i++)
			iterators[i] = iterables[i].iterator();
		return new MultiIterator<T>(iterators);
	}
}

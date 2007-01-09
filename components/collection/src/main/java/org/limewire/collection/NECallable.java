package org.limewire.collection;

import java.util.concurrent.Callable;

/**
 * A <tt>Callable</tt> that does not throw exceptions.
 */
public interface NECallable<T> extends Callable<T> {
	T call();
}

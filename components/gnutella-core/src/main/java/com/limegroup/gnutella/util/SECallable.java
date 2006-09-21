package com.limegroup.gnutella.util;

import java.util.concurrent.Callable;

/**
 * A <tt>Callable</tt> that throws a single exception of the specified type.
 */
public interface SECallable<Ret, Throw extends Exception> extends Callable<Ret> {
	Ret call() throws Throw;
}

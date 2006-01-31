package com.limegroup.gnutella.util;

/**
 * Allows events to be processed at a later point in time.
 * ThreadPool implementation may queue Runnables up for
 * invoking sequentially, asynchronously, or any other kind of
 * order.
 */
public interface ThreadPool {
    
    public void invokeLater(Runnable runner);
    
}
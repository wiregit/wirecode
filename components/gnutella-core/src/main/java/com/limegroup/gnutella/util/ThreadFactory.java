package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ErrorService;

/** Contains a global ThreadPool & methods for adding named threads to it. */
public class ThreadFactory {

    private static final ThreadFactory INSTANCE = new ThreadFactory();
    
    // create an unmanaged ThreadPool -- we'll manage it ourselves.
    private final ThreadPool POOL = new DefaultThreadPool("IdleThread", false);

    public static void startThread(final Runnable runner, final String name) {
        INSTANCE.POOL.invokeLater(new Runnable() {
            public void run() {
                try {
                    Thread.currentThread().setName(name);
                    runner.run();
                } catch(Throwable t) {
                    // must handle ErrorService internally, 
                    // otherwise the thread name will be lost.
                    ErrorService.error(t);
                } finally {
                    Thread.currentThread().setName("IdleThread");
                }
            }
        });
    }    
}

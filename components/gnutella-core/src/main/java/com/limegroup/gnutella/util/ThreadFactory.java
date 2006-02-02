package com.limegroup.gnutella.util;

/** Contains a global ThreadPool & methods for adding named threads to it. */
public class ThreadFactory {

    private static final ThreadFactory INSTANCE = new ThreadFactory();
    
    private final ThreadPool POOL = new DefaultThreadPool("IdleThread");

    public static void startThread(final Runnable runner, final String name) {
        INSTANCE.POOL.invokeLater(new Runnable() {
            public void run() {
                try {
                    Thread.currentThread().setName(name);
                    runner.run();
                } finally {
                    Thread.currentThread().setName("IdleThread");
                }
            }
        });
    }    
}

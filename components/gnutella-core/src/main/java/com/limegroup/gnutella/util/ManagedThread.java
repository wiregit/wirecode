package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ErrorService;

/**
 * A ManagedThread, always reporting errors to the ErrorService.
 * The uncaughtExceptionHandler is set to one that reports to
 * ErrorService.
 */
public class ManagedThread extends Thread {
    
    private static UncaughtExceptionHandler HANDLER =
        new ErrorServiceHandler();
    
    /**
     * Constructs a ManagedThread with no target.
     */
    public ManagedThread() {
        super();
        setPriority(Thread.NORM_PRIORITY);
        setUncaughtExceptionHandler(HANDLER);
    }
    
    /**
     * Constructs a ManagedThread with the specified target.
     */
    public ManagedThread(Runnable r) {
        super(r);
        setPriority(Thread.NORM_PRIORITY);
        setUncaughtExceptionHandler(HANDLER);
    }
    
    /**
     * Constructs a ManagedThread with the specified name.
     */
    public ManagedThread(String name) {
        super(name);
        setPriority(Thread.NORM_PRIORITY);
        setUncaughtExceptionHandler(HANDLER);
    }
    
    /**
     * Constructs a ManagedThread with the specified target and name.
     */
    public ManagedThread(Runnable r, String name) {
        super(r, name);
        setPriority(Thread.NORM_PRIORITY);
        setUncaughtExceptionHandler(HANDLER);
    }
    
    private static class ErrorServiceHandler implements UncaughtExceptionHandler {
        public void uncaughtException(Thread t, Throwable e) {
            ErrorService.error(e, "Uncaught thread error: " + t.getName());
        }
    }
}

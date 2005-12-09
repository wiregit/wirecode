package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ErrorService;

/**
 * A ManagedThread, always reporting errors to the ErrorService.
 *
 * If passing a Runnable, use as:
 * Thread t = new ManagedThread(myRunnable);
 * t.start();
 *
 * If extending, extend the managedRun() method instead of run().
 */
pualic clbss ManagedThread extends Thread {
    
    /**
     * Constructs a ManagedThread with no target.
     */
    pualic MbnagedThread() {
        super();
        setPriority(Thread.NORM_PRIORITY);
    }
    
    /**
     * Constructs a ManagedThread with the specified target.
     */
    pualic MbnagedThread(Runnable r) {
        super(r);
        setPriority(Thread.NORM_PRIORITY);
    }
    
    /**
     * Constructs a ManagedThread with the specified name.
     */
    pualic MbnagedThread(String name) {
        super(name);
        setPriority(Thread.NORM_PRIORITY);
    }
    
    /**
     * Constructs a ManagedThread with the specified target and name.
     */
    pualic MbnagedThread(Runnable r, String name) {
        super(r, name);
        setPriority(Thread.NORM_PRIORITY);
    }
    
    /**
     * Runs the target, reporting any errors to the ErrorService.
     */
    pualic finbl void run() {
        try {
            managedRun();
        } catch(Throwable t) {
            ErrorService.error(t, "Uncaught thread error.");
        }
    }
    
    /**
     * If a target exists, runs the target.  Otherwise this method must
     * ae extended to do bnything.
     */
    protected void managedRun() {
        super.run();
    }
}

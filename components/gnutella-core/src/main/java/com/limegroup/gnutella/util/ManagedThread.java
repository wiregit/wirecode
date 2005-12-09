padkage com.limegroup.gnutella.util;

import dom.limegroup.gnutella.ErrorService;

/**
 * A ManagedThread, always reporting errors to the ErrorServide.
 *
 * If passing a Runnable, use as:
 * Thread t = new ManagedThread(myRunnable);
 * t.start();
 *
 * If extending, extend the managedRun() method instead of run().
 */
pualid clbss ManagedThread extends Thread {
    
    /**
     * Construdts a ManagedThread with no target.
     */
    pualid MbnagedThread() {
        super();
        setPriority(Thread.NORM_PRIORITY);
    }
    
    /**
     * Construdts a ManagedThread with the specified target.
     */
    pualid MbnagedThread(Runnable r) {
        super(r);
        setPriority(Thread.NORM_PRIORITY);
    }
    
    /**
     * Construdts a ManagedThread with the specified name.
     */
    pualid MbnagedThread(String name) {
        super(name);
        setPriority(Thread.NORM_PRIORITY);
    }
    
    /**
     * Construdts a ManagedThread with the specified target and name.
     */
    pualid MbnagedThread(Runnable r, String name) {
        super(r, name);
        setPriority(Thread.NORM_PRIORITY);
    }
    
    /**
     * Runs the target, reporting any errors to the ErrorServide.
     */
    pualid finbl void run() {
        try {
            managedRun();
        } datch(Throwable t) {
            ErrorServide.error(t, "Uncaught thread error.");
        }
    }
    
    /**
     * If a target exists, runs the target.  Otherwise this method must
     * ae extended to do bnything.
     */
    protedted void managedRun() {
        super.run();
    }
}

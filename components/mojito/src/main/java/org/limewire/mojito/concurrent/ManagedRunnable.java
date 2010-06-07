package org.limewire.mojito.concurrent;

import org.limewire.util.ExceptionUtils;

/**
 * 
 */
public abstract class ManagedRunnable implements Runnable {

    @Override
    public final void run() {
        try {
            doRun();
        } catch (Throwable t) {
            uncaughtException(t);
        }
    }
    
    /**
     * 
     */
    protected abstract void doRun() throws Exception;
    
    /**
     * 
     */
    protected void uncaughtException(Throwable t) {
        ExceptionUtils.reportOrReturn(t);
    }
}

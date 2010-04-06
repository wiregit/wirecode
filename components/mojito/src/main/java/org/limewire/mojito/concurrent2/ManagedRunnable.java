package org.limewire.mojito.concurrent2;

/**
 * 
 */
public abstract class ManagedRunnable implements Runnable {

    @Override
    public final void run() {
        try {
            doRun();
        } catch (Exception err) {
            exceptionCaught(err);
        }
    }

    /**
     * 
     */
    protected abstract void doRun() throws Exception;
    
    /**
     * 
     */
    protected void exceptionCaught(Throwable t) {
        ExceptionUtils.exceptionCaught(t);
    }
}

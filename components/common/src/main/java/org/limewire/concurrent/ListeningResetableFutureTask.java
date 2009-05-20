package org.limewire.concurrent;

import java.util.concurrent.Callable;

/** A ListeningRunnableFuture that may run periodically. */
class ListeningResetableFutureTask<V> extends ListeningFutureTask<V> {
    private final boolean periodic;
    
    public ListeningResetableFutureTask(Runnable runnable, V result, boolean periodic) {
        super(runnable, result);
        this.periodic = periodic;
    }

    public ListeningResetableFutureTask(Callable<V> callable, boolean periodic) {
        super(callable);
        this.periodic = periodic;
    }
    
    @Override
    public void run() {
        if(periodic) {
            super.runAndReset();
        } else {
            super.run();
        }
    }
}
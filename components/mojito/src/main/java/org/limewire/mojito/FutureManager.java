package org.limewire.mojito;

import java.io.Closeable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.IdentityHashSet;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.mojito.concurrent.AsyncProcess;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.DHTFutureTask;

/**
 * The {@link FutureManager} manges {@link DHTFuture}s.
 */
class FutureManager implements Closeable {

    /**
     * The {@link Executor} that is being used to start {@link DHTFuture}s
     */
    private static final ExecutorService EXECUTOR 
        = Executors.newSingleThreadExecutor(
            ExecutorsHelper.defaultThreadFactory("FutureManagerThread"));

    /**
     * A {@link Set} of all currently active {@link DHTFuture}s.
     */
    private final Set<DHTFuture<?>> futures 
        = Collections.synchronizedSet(new IdentityHashSet<DHTFuture<?>>());
    
    private boolean open = true;
    
    /**
     * Submits the given {@link AsyncProcess} for execution and returns
     * a {@link DHTFuture} for it.
     */
    public <T> DHTFuture<T> submit(AsyncProcess<T> process, long timeout, TimeUnit unit) {
        synchronized (futures) {
            if (!open) {
                throw new IllegalStateException();
            }
            
            DHTFutureTask<T> future = new ManagedFutureTask<T>(process, timeout, unit);
            futures.add(future);
            EXECUTOR.execute(future);
            return future;
        }
    }
    
    @Override
    public void close() {
        synchronized (futures) {
            if (!open) {
                return;
            }
            
            open = false;
            
            for (DHTFuture<?> future : futures) {
                future.cancel(true);
            }
            
            futures.clear();
        }
    }
    
    /**
     * 
     */
    private class ManagedFutureTask<T> extends DHTFutureTask<T> {

        public ManagedFutureTask(AsyncProcess<T> process, 
                long timeout, TimeUnit unit) {
            super(process, timeout, unit);
        }

        @Override
        protected void done0() {
            futures.remove(this);
            
            super.done0();
        }
    }
}

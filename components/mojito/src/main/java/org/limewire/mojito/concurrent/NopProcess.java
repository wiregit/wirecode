package org.limewire.mojito.concurrent;

/**
 * An {@link DHTFutureProcess} that does nothing.
 */
public class NopProcess implements DHTFutureProcess<Object> {

    private static final NopProcess NOP = new NopProcess();
    
    @SuppressWarnings("unchecked")
    public static <V> DHTFutureProcess<V> process() {
        return (DHTFutureProcess<V>)NOP;
    }
    
    private NopProcess() {}
    
    @Override
    public void start(DHTFuture<Object> future) {
    }
}
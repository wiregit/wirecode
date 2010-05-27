package org.limewire.mojito.concurrent;

/**
 * An {@link AsyncProcess} that does nothing.
 */
public class NopAsyncProcess implements AsyncProcess<Object> {

    private static final NopAsyncProcess NOP = new NopAsyncProcess();
    
    @SuppressWarnings("unchecked")
    public static <V> AsyncProcess<V> process() {
        return (AsyncProcess<V>)NOP;
    }
    
    private NopAsyncProcess() {}
    
    @Override
    public void start(DHTFuture<Object> future) {
    }
}
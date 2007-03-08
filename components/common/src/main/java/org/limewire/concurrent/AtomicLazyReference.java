package org.limewire.concurrent;

/**
 * A reference to an object that is lazily created.
 * The object is only created the first time it is retrieved,
 * and creation is atomic.
 */
public abstract class AtomicLazyReference<T> {
    
    private T obj;

    /** Retrieves the reference, creating it if necessary. */
    public synchronized T get() {
        if(obj == null) {
            obj = createObject();
        }
        return obj;
    }
    
    public abstract T createObject();

}

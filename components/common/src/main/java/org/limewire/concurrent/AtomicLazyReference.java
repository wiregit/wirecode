package org.limewire.concurrent;

/**
 * Provides a reference to an object that is created when first
 * needed. An abstract class, <code>AtomicLazyReference</code> includes an 
 * implementation to retrieve an object T. You must
 * implement {@link #createObject()} in a subclass.
 * 
 * 
 * For more 
 * information see <a href="http://en.wikipedia.org/wiki/Lazy_initialization">
 * Lazy initialization</a>.
 */
public abstract class AtomicLazyReference<T> {
    
    /** The backing object. */
    private T obj;

    /** Retrieves the reference, creating it if necessary. */
    public synchronized T get() {
        if(obj == null)
            obj = createObject();                
        return obj;
    }
    
    /** Creates the object this reference will use. */
    protected abstract T createObject();

}

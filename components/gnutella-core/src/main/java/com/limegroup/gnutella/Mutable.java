package com.limegroup.gnutella;

/**
 * Instances of this class represent an object that is mutable with one underlying value.
 * 
 * @param <T> the type of the underlying value.
 */
public class Mutable<T> {

    /** The underlying value. */
    private T value;
    
    /**
     * Initializes the underlying value to <code>null</code>.
     */
    public Mutable() {
        this(null);
    }
    
    /**
     * Initializes the underlying value to <code>value</code>.
     * 
     * @param value the initial underlying value.
     */    
    public Mutable(T value) {
        this.value = value;
    }    
    
    /**
     * Sets the new underlying value to <code>value</code>.
     * 
     * @param value new value of the underlying value
     */
    public final void set(T value) {
        this.value = value;
    }
    
    /**
     * Returns the underlying value.
     * 
     * @return the underlying value.
     */
    public final T get() {
        return value;
    }
}

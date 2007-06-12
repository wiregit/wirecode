package org.limewire.collection;

/**
 * Typed tuple that holds two objects of possibly different types.

 * @param <O1> the type of the first object
 * @param <O2> the type of the second object
 */
public class Tuple<O1, O2> {

    private final O1 obj1;
    
    private final O2 obj2;

    /**
     * Constructs a tuple for two objects.
     */
    public Tuple(O1 obj1, O2 obj2) {
        this.obj1 = obj1;
        this.obj2 = obj2;
    }

    /**
     * Returns the first object.
     */
    public O1 getFirst() {
        return obj1;
    }
    
    /**
     * Returns the second object. 
     */
    public O2 getSecond() {
        return obj2;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("first: ");
        builder.append(obj1);
        builder.append(", second: ");
        builder.append(obj2);
        return builder.toString();
    }
    
}
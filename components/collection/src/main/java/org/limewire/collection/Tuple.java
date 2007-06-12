package org.limewire.collection;

/**
 * Typed tuple that holds two objects of possibly different types.

 * @param <Type1> the type of the first object
 * @param <Type2> the type of the second object
 */
public class Tuple<Type1, Type2> {

    private final Type1 obj1;
    
    private final Type2 obj2;

    /**
     * Constructs a tuple for two objects.
     */
    public Tuple(Type1 obj1, Type2 obj2) {
        this.obj1 = obj1;
        this.obj2 = obj2;
    }

    /**
     * Returns the first object.
     */
    public Type1 getFirst() {
        return obj1;
    }
    
    /**
     * Returns the second object. 
     */
    public Type2 getSecond() {
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
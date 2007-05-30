package org.limewire.collection;

/**
 * A fixed size Last-In-First-Out (LIFO) Set.
 */
public class FixedSizeLIFOSet<E> extends LIFOSet<E> {

	/**
	 * The EjectionPolicy controls which element should
	 * be removed from the Set if has reached its maximum
	 * capacity.
	 */
	public static enum EjectionPolicy {
		/**
		 * Removes the last-in (newest) element from the
		 * Set if it has reached its maximum capacity.
		 */
		LIFO,
		
		/**
		 * Removes the first-in (eldest) element from the
		 * Set if has reached its maximum capacity.
		 */
		FIFO;
	}
	
    final int maxSize;
    
    private final EjectionPolicy policy;
    
    public FixedSizeLIFOSet(int maxSize) {
        this(maxSize, EjectionPolicy.LIFO);
    }

    public FixedSizeLIFOSet(int maxSize, EjectionPolicy policy) {
    	this.maxSize = maxSize;
    	this.policy = policy;
    }
    
    public FixedSizeLIFOSet(int maxSize, int initialCapacity, float loadFactor) {
    	this(maxSize, initialCapacity, loadFactor, EjectionPolicy.LIFO);
    }
    
    public FixedSizeLIFOSet(int maxSize, int initialCapacity, float loadFactor, EjectionPolicy policy) {
        super(initialCapacity, loadFactor);
        this.maxSize = maxSize;
        this.policy = policy;
    }
    
    @Override
    public boolean add(E o) {
    	boolean added = super.add(o);
    	if (added && size() > maxSize) {
    		if (policy == EjectionPolicy.FIFO) {
    			remove(0);
    			
    		} else { // EjectionPolicy.LIFO
    			remove(Math.max(0, size()-2));
    		}
    		
    		assert (size() <= maxSize);
    	}
        
        return added;
    }
}

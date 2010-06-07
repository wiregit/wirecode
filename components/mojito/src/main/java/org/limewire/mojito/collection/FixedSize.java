package org.limewire.mojito.collection;

/**
 * An interface for fixed-size Data-Structures.
 */
public interface FixedSize {

    /**
     * Returns the maximum size of the Data-Structure.
     */
    public int getMaxSize();
    
    /**
     * Returns {@code true} if the Data-Structure has 
     * reached its maximum capacity.
     */
    public boolean isFull();
}

package org.limewire.mojito.util;

/**
 * A {@link MaxStack} is a counter with a lower (0) and upper limit.
 * It's possible to exceed the upper ({@link #push(boolean)}) but
 * never the lower limit.
 * 
 * <p>This class is useful to count parallel processes.
 */
public class MaxStack {

    /**
     * The maximum value
     */
    private final int max;
    
    /**
     * The current value
     */
    private int count = 0;
    
    /**
     * Creates a {@link MaxStack} with the given max value
     */
    public MaxStack(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("max=" + max);
        }
        
        this.max = max;
    }
    
    /**
     * Returns true if the current count is greater or equal the max value
     */
    public boolean isMax() {
        return count >= max;
    }
    
    /**
     * Returns true if the current count is less than the max value.
     */
    public boolean hasFree() {
        return !isMax();
    }
    
    /**
     * Increments the internal counter by one and returns true on success.
     */
    public boolean push() {
        return push(false);
    }
    
    /**
     * Increments the internal counter by one and returns true on success.
     */
    public boolean push(boolean force) {
        if (count < max || force) {
            ++count;
            return true;
        }
        return false;
    }
    
    /**
     * Decrements the internal counter by one.
     */
    public int pop() {
        return pop(1);
    }
    
    /**
     * Decrements the internal counter by the given value.
     */
    public int pop(int value) {
        if (0 < value) {
            count = Math.max(count - value, 0);
        }
        return count;
    }
    
    /**
     * Returns the current value
     */
    public int poll() {
        return count;
    }
    
    @Override
    public String toString() {
        return count + ", " + max;
    }
}

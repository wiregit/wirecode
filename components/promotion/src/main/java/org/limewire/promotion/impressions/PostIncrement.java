package org.limewire.promotion.impressions;

/**
 * Instances of this hold an integer and increment the value in the
 * {@link #inc(int)} method, and <b>then</b> increment it.
 */
public final class PostIncrement {

    private int val;

    /**
     * Initializes the value to <code>0</code>.
     */
    public PostIncrement() {
        this(0);
    }

    /**
     * Initializes the value to <code>val</code>.
     */
    public PostIncrement(int val) {
        this.val = val;
    }

    /**
     * Returns the current value.
     * 
     * @return the current value.
     */
    public int val() {
        return val;
    }

    /**
     * Returns the current value and then increments it.
     * 
     * @return the current value and then increments it.
     */
    public int inc(int inc) {
        int ret = val;
        val += inc;
        return ret;
    }

    /**
     * Returns the current value and then increments it by <code>1</code>.
     * 
     * @return the current value and then increments it by <code>1</code>.
     * @see #inc(inc)
     */
    public int inc() {
        return inc(1);
    }
}

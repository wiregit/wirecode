package org.limewire.collection;

import java.io.Serializable;

public abstract class Range implements Serializable {

    private static final long serialVersionUID = -2562093104400487223L;
    /**
     * @return true if this Interval is a "subrange" of the other interval 
     */
    public final boolean isSubrange(Range other) {
        return (getLow() >= other.getLow() && getHigh() <= other.getHigh());
    }

    public abstract byte[] toBytes();

    public abstract void toBytes(byte[] dest, int offset);

    public abstract long getLow();

    public abstract long getHigh();

    public static Range createRange(long start, long end) {
        if (start <= Integer.MAX_VALUE && end <= Integer.MAX_VALUE)
            return new Interval(start, end);
        else
            return new LongInterval(start, end);
    }
    
    public static Range createRange(long singleton) {
        return createRange(singleton, singleton);
    }
    
    public static Range createLong(long start, long end) {
        return new LongInterval(start, end);
    }

    public String toString() {
        if (getLow()==getHigh())
            return String.valueOf(getLow());
        else
            return String.valueOf(getLow())+"-"+String.valueOf(getHigh());
    }

    public boolean equals(Object o) {
        if (! (o instanceof Range))
            return false;
        Range other=(Range)o;
        return getLow()==other.getLow() && getHigh()==other.getHigh();
    }
}
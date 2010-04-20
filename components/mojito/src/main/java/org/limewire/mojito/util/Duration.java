package org.limewire.mojito.util;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * An immutable class that holds a time duration.
 */
public class Duration implements Comparable<Duration>, Serializable, Cloneable {

    private static final long serialVersionUID = -8742316695654961802L;
    
    private final long duration;
    
    private final TimeUnit unit;
    
    public Duration(long duration, TimeUnit unit) {
        this.duration = duration;
        this.unit = unit;
    }
    
    public long getDuration(TimeUnit unit) {
        return unit.convert(duration, this.unit);
    }
    
    public long getDurationInMillis() {
        return getDuration(TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Duration o) {
        return (int)(duration - o.getDuration(unit));
    }
    
    @Override
    public int hashCode() {
        return (int)(unit.toMillis(duration));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof Duration)) {
            return false;
        }
        
        Duration other = (Duration)o;
        return compareTo(other) == 0;
    }
    
    @Override
    public Duration clone() {
        return this;
    }
    
    @Override
    public String toString() {
        return duration + " " + unit;
    }
}

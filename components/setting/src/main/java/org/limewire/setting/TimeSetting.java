package org.limewire.setting;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Setting} to hold a time duration.
 */
public class TimeSetting extends LongSetting {

    /**
     * Creates a {@link TimeSetting} with the given arguments.
     */
    public TimeSetting(Properties defaultProps, 
            Properties props, String key, 
            long defaultLong, TimeUnit defaultUnit, 
            long min, TimeUnit minUnit, 
            long max, TimeUnit maxUnit) {
        super(defaultProps, props, key, defaultUnit.toMillis(defaultLong), 
                minUnit.toMillis(min), maxUnit.toMillis(max));
    }

    /**
     * Creates a {@link TimeSetting} with the given arguments.
     */
    public TimeSetting(Properties defaultProps, Properties props, 
            String key, long defaultLong, TimeUnit unit) {
        super(defaultProps, props, key, unit.toMillis(defaultLong));
    }
    
    /**
     * Returns the time in the given {@link TimeUnit}
     */
    public long getTime(TimeUnit unit) {
        return unit.convert(getValue(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Returns the time in milliseconds.
     */
    public long getTimeInMillis() {
        return getTime(TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sets the time in the given {@link TimeUnit}
     */
    public void setTime(long time, TimeUnit unit) {
        setValue(unit.toMillis(time));
    }
}

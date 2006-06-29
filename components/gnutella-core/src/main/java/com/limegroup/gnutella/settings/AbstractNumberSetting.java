package com.limegroup.gnutella.settings;

import java.util.Properties;

import com.limegroup.gnutella.Assert;

public abstract class AbstractNumberSetting<T extends Number & Comparable<T>> extends Setting {

    /**
     * Adds a safeguard against simpp making a setting take a value beyond the
     * reasonable max 
     */
    protected final T MAX_VALUE;

    /**
     * Adds a safeguard against simpp making a setting take a value below the
     * reasonable min
     */
    protected final T MIN_VALUE;
    
    protected AbstractNumberSetting(Properties defaultProps, Properties props,
                                    String key, String defaultValue, 
                              String simppKey, T min, T max) {
        super(defaultProps, props, key, defaultValue, simppKey);
        if(max != null && min != null) {//do we need to check max, min?
            if(max.compareTo(min) < 0) //max less than min?
                throw new IllegalArgumentException("max less than min");
        }
        MAX_VALUE = max;
        MIN_VALUE = min;
    }

    /**
     * Set new property value
     * @param value new property value 
     *
     * Note: This is the method used by SimmSettingsManager to load the setting
     * with the value specified by Simpp 
     */
    protected void setValue(String value) {
        if(isSimppEnabled()) {
            Assert.that(MAX_VALUE != null, "simpp setting created with no max");
            Assert.that(MIN_VALUE != null, "simpp setting created with no min");
        }
        value = normalizeValue(value);
        super.setValue(value);
    }


    /**
     * Normalizes a value to an acceptable value for this setting.
     */
    protected String normalizeValue(String value) {
        Comparable<T> comparableValue = null;
        try {
            comparableValue = convertToComparable(value);
        } catch (NumberFormatException e) {
            return DEFAULT_VALUE;
        }
        if (MAX_VALUE != null && comparableValue.compareTo(MAX_VALUE) > 0) {
            return MAX_VALUE.toString();
        } else if (MIN_VALUE != null && comparableValue.compareTo(MIN_VALUE) < 0) {
            return MIN_VALUE.toString();
        }
        return value;
    }

    /**
     * Converts a String to a Comparable of the same type as MAX_VALUE and MIN_VALUE.
     */
    abstract protected Comparable<T> convertToComparable(String value);
    
}

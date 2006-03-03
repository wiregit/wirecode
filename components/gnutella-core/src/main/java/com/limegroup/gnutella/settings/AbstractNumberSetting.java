package com.limegroup.gnutella.settings;

import java.util.Properties;

import com.limegroup.gnutella.Assert;

public abstract class AbstractNumberSetting extends Setting {

    /**
     * Adds a safeguard against simpp making a setting take a value beyond the
     * reasonable max 
     */
    protected final Number MAX_VALUE;

    /**
     * Adds a safeguard against simpp making a setting take a value below the
     * reasonable min
     */
    protected final Number MIN_VALUE;
    
    protected AbstractNumberSetting(Properties defaultProps, Properties props,
                                    String key, String defaultValue, 
                              String simppKey, Number max, Number min) {
        super(defaultProps, props, key, defaultValue, simppKey);
        if(max != null && min != null) {//do we need to check max, min?
            // All standard library Numbers implement Comparable
            Comparable comparableMax = (Comparable) max;
            if(comparableMax.compareTo(min) < 0) //max less than min?
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
            if(!isAcceptableValue(value))
                return;
        }
        super.setValue(value);
    }


    /**
     * The various settings must decide for themselves if this value acceptable. 
     */
    abstract protected boolean isAcceptableValue(String value);

}

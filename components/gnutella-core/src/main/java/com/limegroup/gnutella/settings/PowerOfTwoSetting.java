package com.limegroup.gnutella.settings;

import java.util.Properties;

public final class PowerOfTwoSetting extends LongSetting {
    /**
     * Creates a new <tt>PowerOfTwoSetting</tt> instance with the specified
     * key and defualt value.  A PowerOfTwoSitting may take on only values
     * that are powers of two.
     *
     * @param key the constant key to use for the setting
     * @param defaultLong the default value to use for the setting, which 
     *            must be a power of two.
     */
    PowerOfTwoSetting(Properties defaultProps, Properties props, String key, 
                                         long defaultLong) {
        super(defaultProps, props, key, defaultLong);
        if (! isPowerOfTwo(defaultLong)) {
            throw new IllegalArgumentException("Default value is not a power of two");
        }
    }

    PowerOfTwoSetting(Properties defaultProps, Properties props, String key, 
                long defaultLong, String simppSetting, long max, long min) {
        super(defaultProps, props, key, defaultLong, simppSetting, max, min);
        if (! isPowerOfTwo(defaultLong)) {
            throw new IllegalArgumentException("Default value is not a power of two");
        }
    }
    
    /** Utility method to determine if a long is zero or a power of two */
    private static final boolean isPowerOfTwo(long x) {
        if (x <= 0) {
            return false;
        }
        return ((~x+1)&x) == x;
    }
    
    // isInRange(long) is slightly abused here, unless "range" is used in
    // the sense of the set of possible outputs (known as the "range" or
    // "co-domain" of a mathematical function)
    protected boolean isInRange(long value) {
        if (! isPowerOfTwo(value)) {
            return false;
        }
        return super.isInRange(value);
    }
}

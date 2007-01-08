package org.limewire.setting;

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
                long defaultLong, long min, long max) {
        super(defaultProps, props, key, defaultLong, min, max);
        if (! isPowerOfTwo(defaultLong)) {
            throw new IllegalArgumentException("Default value is not a power of two");
        }
        if (! isPowerOfTwo(max)) {
            throw new IllegalArgumentException("Max value is not a power of two");
        }
        if (! isPowerOfTwo(min)) {
            throw new IllegalArgumentException("Min value is not a power of two");
        }
    }
    
    /** Utility method to determine if a long is zero or a power of two */
    private static final boolean isPowerOfTwo(long x) {
        if (x <= 0) {
            return false;
        }
        return ((~x+1)&x) == x;
    }
    
    /** Makes value a power of two by rounding down if neccesary
     * and delegates the rest of the normalization to the superclass.
     * 
     * Non-positive values cannot be made made powers of two by rounding
     * down, and are special-cased to return MIN_VALUE, which is forced by
     * the constructor to be non-negative.
     * 
     * Strings that can't be parsed as longs will result in DEFALT_VALUE.
     */
    protected String normalizeValue(String value) {
        long longValue;
        
        try {
            longValue = Long.parseLong(value);
        } catch (NumberFormatException e) {
            // Attempts to set with non-numbers numbers will result in DEFAULT_VALUE.
            return DEFAULT_VALUE;
        }
        
        if (longValue <= 0) {
            if (MIN_VALUE != null) {
                return MIN_VALUE.toString();
            }
            return super.normalizeValue("1"); // The smallest power of two
        }
        
        long lowestSetBit = (~longValue+1) & longValue;
        if (lowestSetBit != longValue) {
            do {
                // take away lowest set bit until we get a power of two or zero
                longValue -= lowestSetBit;
                lowestSetBit = (~longValue+1) & longValue;
            } while (lowestSetBit  != longValue);
            if (longValue == 0) {
                longValue = 1;
            }
            value = String.valueOf(longValue);
        }
        return super.normalizeValue(value);
    }
}

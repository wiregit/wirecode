package org.limewire.ui.swing.search.filter;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Range filter format for length.
 */
class LengthFilterFormat<E extends FilterableItem> implements RangeFilterFormat<E> {
    /** Array of length options in seconds. */
    private static final long[] LENGTHS = {
        0, 
        10,
        30,
        60, 
        60 * 10, 
        60 * 30, 
        60 * 60,
        60 * 60 * 24
    };

    @Override
    public String getHeaderText() {
        return I18n.tr("Length");
    }

    @Override
    public Matcher<E> getMatcher(long minValue, long maxValue) {
        return new LengthMatcher<E>(minValue, maxValue);
    }

    @Override
    public long[] getValues() {
        return LENGTHS;
    }

    @Override
    public String getValueText(int valueIndex) {
        return CommonUtils.seconds2time(LENGTHS[valueIndex]);
    }
    
    @Override
    public boolean isUpperLimitEnabled() {
        return true;
    }

    /**
     * A matcher used to filter an item by length.
     */
    private static class LengthMatcher<E extends FilterableItem> implements Matcher<E> {
        private final long minLength;
        private final long maxLength;
        
        /**
         * Constructs a LengthMatcher for the specified length range.
         */
        public LengthMatcher(long minLength, long maxLength) {
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        /**
         * Returns true if the specified item is within the length range.
         */
        @Override
        public boolean matches(E item) {
            Object value = item.getProperty(FilePropertyKey.LENGTH);
            
            if (value instanceof Long) {
                long length = ((Long) value).longValue();
                boolean minValid = (minLength == LENGTHS[0]) || (length >= minLength);
                boolean maxValid = (maxLength == LENGTHS[LENGTHS.length - 1]) || (length <= maxLength);
                return (minValid && maxValid);
                
            } else {
                return false;
            }
        }
    }
}
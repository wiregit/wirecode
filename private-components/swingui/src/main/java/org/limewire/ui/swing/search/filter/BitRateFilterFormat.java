package org.limewire.ui.swing.search.filter;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Range filter format for bit rate.
 */
class BitRateFilterFormat implements RangeFilterFormat {
    /** Array of bit rate options. */
    private static final long[] RATES = {
        0, 
        64,
        96,
        128, 
        160,
        192, 
        256
    };

    @Override
    public String getHeaderText() {
        return I18n.tr("Bitrate");
    }

    @Override
    public Matcher<VisualSearchResult> getMatcher(long minValue, long maxValue) {
        return new BitRateMatcher(minValue);
    }

    @Override
    public String getValueText(int valueIndex) {
        return String.valueOf(RATES[valueIndex]);
    }

    @Override
    public long[] getValues() {
        return RATES;
    }
    
    @Override
    public boolean isUpperLimitEnabled() {
        return false;
    }

    /**
     * A matcher used to filter a search result by bit rate.
     */
    private static class BitRateMatcher implements Matcher<VisualSearchResult> {
        private final long bitRate;
        
        /**
         * Constructs a BitRateMatcher for the specified bit rate.
         */
        public BitRateMatcher(long bitRate) {
            this.bitRate = bitRate;
        }

        /**
         * Returns true if the specified search result matches or exceeds the 
         * bit rate.
         */
        @Override
        public boolean matches(VisualSearchResult vsr) {
            if (bitRate == 0) return true;
            
            Object rate = vsr.getProperty(FilePropertyKey.BITRATE);
            if (rate instanceof Long) {
                return (((Long) rate).longValue() >= bitRate);
            } else {
                return false;
            }
        }
    }
}

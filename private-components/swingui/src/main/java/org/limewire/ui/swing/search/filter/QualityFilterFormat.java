package org.limewire.ui.swing.search.filter;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Range filter format for quality.
 */
class QualityFilterFormat implements RangeFilterFormat {
    /** Array of quality options. */
    private static final long[] QUALITIES = {
        0, // spam
        1, // poor
        2, // good
        3  // excellent
    };

    @Override
    public String getHeaderText() {
        return I18n.tr("Quality");
    }

    @Override
    public Matcher<VisualSearchResult> getMatcher(long minValue, long maxValue) {
        return new QualityMatcher(minValue);
    }

    @Override
    public String getValueText(int valueIndex) {
        return GuiUtils.toQualityStringShort(QUALITIES[valueIndex]);
    }

    @Override
    public long[] getValues() {
        return QUALITIES;
    }
    
    @Override
    public boolean isUpperLimitEnabled() {
        return false;
    }

    /**
     * A matcher used to filter a search result by quality.
     */
    private static class QualityMatcher implements Matcher<VisualSearchResult> {
        private final long quality;
        
        /**
         * Constructs a QualityMatcher for the specified quality.
         */
        public QualityMatcher(long quality) {
            this.quality = quality;
        }

        /**
         * Returns true if the specified search result matches or exceeds the 
         * quality.
         */
        @Override
        public boolean matches(VisualSearchResult vsr) {
            if (quality == 0) return true;
            if (vsr.isSpam()) return false;
            
            Object value = vsr.getProperty(FilePropertyKey.QUALITY);
            if (value instanceof Long) {
                return (((Long) value).longValue() >= quality);
            } else {
                return false;
            }
        }
    }
}

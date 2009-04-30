package org.limewire.ui.swing.search.filter;

import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Range filter format for file size.
 */
class FileSizeFilterFormat<E extends FilterableItem> implements RangeFilterFormat<E> {
    /** Array of size options in bytes. */
    private static final long[] SIZES = {
        0, 
        512,
        1024, 
        1024 * 512,
        1024 * 1024, 
        1024 * 1024 * 512,
        1024 * 1024 * 1024, 
        (long) 1024 * 1024 * 1024 * 512,  
        (long) 1024 * 1024 * 1024 * 1024
    };

    @Override
    public String getHeaderText() {
        return I18n.tr("Size");
    }

    @Override
    public Matcher<E> getMatcher(long minValue, long maxValue) {
        return new FileSizeMatcher<E>(minValue, maxValue);
    }

    @Override
    public long[] getValues() {
        return SIZES;
    }

    @Override
    public String getValueText(int valueIndex) {
        return GuiUtils.toUnitbytes(SIZES[valueIndex]);
    }
    
    @Override
    public boolean isUpperLimitEnabled() {
        return true;
    }

    /**
     * A matcher used to filter an item by file size.
     */
    private static class FileSizeMatcher<E extends FilterableItem> implements Matcher<E> {
        private final long minSize;
        private final long maxSize;
        
        /**
         * Constructs a FileSizeMatcher for the specified file size range.
         */
        public FileSizeMatcher(long minSize, long maxSize) {
            this.minSize = minSize;
            this.maxSize = maxSize;
        }

        /**
         * Returns true if the specified item is within the file size range.
         */
        @Override
        public boolean matches(E item) {
            long size = item.getSize();
            boolean minValid = (minSize == SIZES[0]) || (size >= minSize);
            boolean maxValid = (maxSize == SIZES[SIZES.length - 1]) || (size <= maxSize);
            return (minValid && maxValid); 
        }
    }
}

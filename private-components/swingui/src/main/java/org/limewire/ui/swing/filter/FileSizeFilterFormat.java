package org.limewire.ui.swing.filter;

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
        512,  // 512 bytes
        1024, // 1 KB
        1024 * 512,  // 512 KB
        1024 * 1024, // 1 MB
        1024 * 1024 * 512,  // 512 MB
        1024 * 1024 * 1024, // 1 GB
        (long) 1024 * 1024 * 1024 * 512, // 512 GB  
        (long) 1024 * 1024 * 1024 * 1024 // 1 TB
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

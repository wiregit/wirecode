package org.limewire.ui.swing.search.filter;

import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Defines the configuration format for a RangeFilter.
 */
public interface RangeFilterFormat {

    /**
     * Returns the header text.
     */
    String getHeaderText();
    
    /**
     * Returns a Matcher that uses the specified minimum and maximum values 
     * for filtering search results.
     */
    Matcher<VisualSearchResult> getMatcher(long minValue, long maxValue);
    
    /**
     * Returns an array of range values.
     */
    long[] getValues();
    
    /**
     * Returns a text string for the value at the specified index.
     */
    String getValueText(int valueIndex);
    
    /**
     * Returns true if the upper limit is enabled.
     */
    boolean isUpperLimitEnabled();
    
}

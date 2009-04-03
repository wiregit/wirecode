package org.limewire.ui.swing.search.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher used to filter a search result using a collection of property 
 * values.
 */
class PropertyMatcher implements Matcher<VisualSearchResult> {
    private final FilterType filterType;
    private final FilePropertyKey propertyKey;
    private final Object allListItem;
    
    /** Property values to match. */
    private final Set<Object> values = new HashSet<Object>();

    /**
     * Constructs a PropertyMatcher for the specified filter type, property 
     * key, "all" list item, and collection of property values.
     */
    public PropertyMatcher(FilterType filterType, FilePropertyKey propertyKey,
            Object allListItem, Collection<Object> values) {
        this.filterType = filterType;
        this.propertyKey = propertyKey;
        this.allListItem = allListItem;
        this.values.addAll(values);
    }
    
    /**
     * Returns true if the specified search result matches any of the property
     * values.
     */
    @Override
    public boolean matches(VisualSearchResult vsr) {
        if (vsr == null) return false;
        if (values.isEmpty()) return true;
        if (values.contains(allListItem)) return true;
        
        switch (filterType) {
        case EXTENSION:
            return values.contains(vsr.getFileExtension().toLowerCase());
        case PROPERTY:
            return values.contains(vsr.getProperty(propertyKey));
        default:
            return false;
        }
    }
}

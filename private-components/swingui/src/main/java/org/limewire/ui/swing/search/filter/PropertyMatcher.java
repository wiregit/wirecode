package org.limewire.ui.swing.search.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.IconManager;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher used to filter a search result using a collection of property 
 * values.
 */
class PropertyMatcher implements Matcher<VisualSearchResult> {
    private final FilterType filterType;
    private final FilePropertyKey propertyKey;
    private final IconManager iconManager;
    
    /** Property values to match. */
    private final Set<Object> values = new HashSet<Object>();

    /**
     * Constructs a PropertyMatcher for the specified filter type, property 
     * key, icon manager, and collection of property values.
     */
    public PropertyMatcher(FilterType filterType, FilePropertyKey propertyKey,
            IconManager iconManager, Collection<Object> values) {
        this.filterType = filterType;
        this.propertyKey = propertyKey;
        this.iconManager = iconManager;
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
        
        switch (filterType) {
        case EXTENSION:
            return values.contains(vsr.getFileExtension().toLowerCase());
        case PROPERTY:
            return values.contains(vsr.getProperty(propertyKey));
        case TYPE:
            String type = iconManager.getMIMEDescription(vsr.getFileExtension());
            return values.contains(type);
        default:
            return false;
        }
    }
}

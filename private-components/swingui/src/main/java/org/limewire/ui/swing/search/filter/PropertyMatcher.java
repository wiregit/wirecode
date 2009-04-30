package org.limewire.ui.swing.search.filter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.util.IconManager;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher used to filter an item using a collection of property values.
 */
class PropertyMatcher<E extends FilterableItem> implements Matcher<E> {
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
     * Returns true if the specified item matches any of the property values.
     */
    @Override
    public boolean matches(E item) {
        if (item == null) return false;
        if (values.isEmpty()) return true;
        
        switch (filterType) {
        case EXTENSION:
            return values.contains(item.getFileExtension().toLowerCase());
        case PROPERTY:
            return values.contains(item.getProperty(propertyKey));
        case FILE_TYPE:
            String type = iconManager.getMIMEDescription(item.getFileExtension());
            return values.contains(type);
        default:
            return false;
        }
    }
}

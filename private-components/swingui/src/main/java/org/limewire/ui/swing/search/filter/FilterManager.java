package org.limewire.ui.swing.search.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.util.IconManager;

/**
 * A manager for search result filters.
 */
public class FilterManager implements Disposable {

    /** Search results data model. */
    private final SearchResultsModel searchResultsModel;
    
    /** Icon manager for determining file types. */
    private final IconManager iconManager;
    
    /** Map containing non-property filters. */
    private final Map<FilterType, Filter> filterMap = new EnumMap<FilterType, Filter>(FilterType.class);
    
    /** Map containing property filters. */
    private final Map<FilePropertyKey, Filter> propertyFilterMap = new HashMap<FilePropertyKey, Filter>();
    
    /**
     * Constructs a FilterManager using the specified search results data model
     * and icon manager.
     */
    public FilterManager(SearchResultsModel searchResultsModel, IconManager iconManager) {
        this.searchResultsModel = searchResultsModel;
        this.iconManager = iconManager;
    }
    
    @Override
    public void dispose() {
        // Dispose of all non-property filters.
        Collection<Filter> filters = filterMap.values();
        for (Filter filter : filters) {
            filter.dispose();
        }
        
        // Dispose of all property filters.
        Collection<Filter> propertyFilters = propertyFilterMap.values();
        for (Filter filter : propertyFilters) {
            filter.dispose();
        }
    }
    
    /**
     * Returns a filter for file categories.
     */
    public CategoryFilter getCategoryFilter() {
        return (CategoryFilter) getFilter(FilterType.CATEGORY);
    }
    
    /**
     * Returns a filter for file sources.
     */
    public Filter getSourceFilter() {
        return getFilter(FilterType.SOURCE);
    }
    
    /**
     * Returns the minimum number of property filters for the specified search
     * category.  A value less than 1 means that there is no minimum and all 
     * filters are displayed.
     */
    public int getPropertyFilterMinimum(SearchCategory searchCategory) {
        if (searchCategory == null) {
            return -1;
        }
        
        switch (searchCategory) {
        case AUDIO:
            return 3;
        default:
            return -1;
        }
    }
    
    /**
     * Returns an array of filters for the specified search category.
     */
    public Filter[] getPropertyFilters(SearchCategory searchCategory) {
        // Return empty array if null.
        if (searchCategory == null) {
            return new Filter[0];
        }
        
        // Create filter list.
        List<Filter> filterList = new ArrayList<Filter>();
        
        switch (searchCategory) {
        case AUDIO:
            filterList.add(getPropertyFilter(FilePropertyKey.AUTHOR));
            filterList.add(getPropertyFilter(FilePropertyKey.ALBUM));
            filterList.add(getPropertyFilter(FilePropertyKey.GENRE));
            filterList.add(getFilter(FilterType.FILE_SIZE));
            filterList.add(getFilter(FilterType.EXTENSION));
            filterList.add(getFilter(FilterType.BIT_RATE));
            filterList.add(getFilter(FilterType.LENGTH));
            break;
            
        case VIDEO:
            filterList.add(getFilter(FilterType.FILE_SIZE));
            filterList.add(getFilter(FilterType.EXTENSION));
            filterList.add(getFilter(FilterType.QUALITY));
            filterList.add(getFilter(FilterType.LENGTH));
            break;

        case DOCUMENT:
        case OTHER:
            filterList.add(getFilter(FilterType.FILE_TYPE));
            filterList.add(getFilter(FilterType.FILE_SIZE));
            filterList.add(getFilter(FilterType.EXTENSION));
            break;
            
        case ALL:
        case IMAGE:
        case PROGRAM:
        default:
            filterList.add(getFilter(FilterType.FILE_SIZE));
            filterList.add(getFilter(FilterType.EXTENSION));
            break;
        }
        
        return filterList.toArray(new Filter[filterList.size()]);
    }
    
    /**
     * Returns the filter for the specified filter type.
     */
    private Filter getFilter(FilterType filterType) {
        Filter filter = filterMap.get(filterType);
        if (filter == null) {
            filter = createFilter(filterType, null);
            filterMap.put(filterType, filter);
        }
        return filter;
    }
    
    /**
     * Return the property filter for the specified property key.
     */
    private Filter getPropertyFilter(FilePropertyKey propertyKey) {
        Filter filter = propertyFilterMap.get(propertyKey);
        if (filter == null) {
            filter = createFilter(FilterType.PROPERTY, propertyKey);
            propertyFilterMap.put(propertyKey, filter);
        }
        return filter;
    }
    
    /**
     * Creates a new filter for the specified filter type and property key.
     * For FilterType.PROPERTY, <code>propertyKey</code> must be non-null.
     */
    private Filter createFilter(FilterType filterType, FilePropertyKey propertyKey) {
        switch (filterType) {
        case BIT_RATE:
            return new RangeFilter(new BitRateFilterFormat());
            
        case CATEGORY:
            return new CategoryFilter(searchResultsModel.getFilteredSearchResults());
            
        case EXTENSION:
            return new PropertyFilter(searchResultsModel.getFilteredSearchResults(), 
                    FilterType.EXTENSION, null, iconManager);
            
        case FILE_SIZE:
            return new RangeFilter(new FileSizeFilterFormat());
            
        case FILE_TYPE:
            return new PropertyFilter(searchResultsModel.getFilteredSearchResults(), 
                    FilterType.FILE_TYPE, null, iconManager);
            
        case LENGTH:
            return new RangeFilter(new LengthFilterFormat());
            
        case PROPERTY:
            return new PropertyFilter(searchResultsModel.getFilteredSearchResults(), 
                    FilterType.PROPERTY, propertyKey, iconManager);
            
        case QUALITY:
            return new RangeFilter(new QualityFilterFormat());
            
        case SOURCE:
            return new SourceFilter();
            
        default:
            throw new IllegalArgumentException("Invalid filter type " + filterType);
        }
    }
}

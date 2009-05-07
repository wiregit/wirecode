package org.limewire.ui.swing.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.util.IconManager;

/**
 * A manager for filters.  FilterManager maintains a set of filters that can be
 * applied to a filterable data source.  A variety of filter types are
 * supported.  Filters are internally cached within the manager for reuse by 
 * different search categories.  When a search category is selected, the 
 * <code>getPropertyFilterList()</code> method returns a list of the available
 * filters for the category.
 */
public class FilterManager<E extends FilterableItem> implements Disposable {

    /** Filterable data source. */
    private final FilterableSource<E> filterableSource;
    
    /** Icon manager for determining file types. */
    private final IconManager iconManager;
    
    /** Map containing non-property filters. */
    private final Map<FilterType, Filter<E>> filterMap = 
        new EnumMap<FilterType, Filter<E>>(FilterType.class);
    
    /** Map containing property filters. */
    private final Map<FilePropertyKey, Filter<E>> propertyFilterMap = 
        new EnumMap<FilePropertyKey, Filter<E>>(FilePropertyKey.class);
    
    /**
     * Constructs a FilterManager using the specified filterable data source
     * and icon manager.
     */
    public FilterManager(FilterableSource<E> filterableSource, IconManager iconManager) {
        this.filterableSource = filterableSource;
        this.iconManager = iconManager;
    }
    
    @Override
    public void dispose() {
        // Dispose of all non-property filters.
        Collection<Filter<E>> filters = filterMap.values();
        for (Filter<E> filter : filters) {
            filter.dispose();
        }
        
        // Dispose of all property filters.
        Collection<Filter<E>> propertyFilters = propertyFilterMap.values();
        for (Filter<E> filter : propertyFilters) {
            filter.dispose();
        }
    }
    
    /**
     * Returns a filter for file categories.
     */
    public CategoryFilter<E> getCategoryFilter() {
        return (CategoryFilter<E>) getFilter(FilterType.CATEGORY);
    }
    
    /**
     * Returns a filter for file sources.
     */
    public Filter<E> getSourceFilter() {
        return getFilter(FilterType.SOURCE);
    }
    
    /**
     * Returns the minimum number of property filters for the specified search
     * category.  A value less than 1 means that there is no minimum and all 
     * filters are displayed.
     */
    public int getPropertyFilterMinimum(SearchCategory searchCategory) {
        return (searchCategory == SearchCategory.AUDIO) ? 3 : -1;
    }
    
    /**
     * Returns a list of filters for the specified search category.
     */
    public List<Filter<E>> getPropertyFilterList(SearchCategory searchCategory) {
        // Create filter list.
        List<Filter<E>> filterList = new ArrayList<Filter<E>>();
        
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
        
        return filterList;
    }
    
    /**
     * Returns the filter for the specified filter type.
     */
    private Filter<E> getFilter(FilterType filterType) {
        Filter<E> filter = filterMap.get(filterType);
        if (filter == null) {
            filter = createFilter(filterType, null);
            filterMap.put(filterType, filter);
        }
        return filter;
    }
    
    /**
     * Return the property filter for the specified property key.
     */
    private Filter<E> getPropertyFilter(FilePropertyKey propertyKey) {
        Filter<E> filter = propertyFilterMap.get(propertyKey);
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
    private Filter<E> createFilter(FilterType filterType, FilePropertyKey propertyKey) {
        switch (filterType) {
        case BIT_RATE:
            return new RangeFilter<E>(new BitRateFilterFormat<E>());
            
        case CATEGORY:
            return new CategoryFilter<E>(filterableSource.getFilteredList());
            
        case EXTENSION:
            return new PropertyFilter<E>(filterableSource.getFilteredList(), 
                    FilterType.EXTENSION, null, iconManager);
            
        case FILE_SIZE:
            return new RangeFilter<E>(new FileSizeFilterFormat<E>());
            
        case FILE_TYPE:
            return new PropertyFilter<E>(filterableSource.getFilteredList(), 
                    FilterType.FILE_TYPE, null, iconManager);
            
        case LENGTH:
            return new RangeFilter<E>(new LengthFilterFormat<E>());
            
        case PROPERTY:
            return new PropertyFilter<E>(filterableSource.getFilteredList(), 
                    FilterType.PROPERTY, propertyKey, iconManager);
            
        case QUALITY:
            return new RangeFilter<E>(new QualityFilterFormat<E>());
            
        case SOURCE:
            return new SourceFilter<E>();
            
        default:
            throw new IllegalArgumentException("Invalid filter type " + filterType);
        }
    }
}

package org.limewire.ui.swing.search.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.IconManager;

import ca.odell.glazedlists.EventList;

/**
 * A factory for creating filters.
 */
public class FilterFactory implements Disposable {

    private final SearchResultsModel searchResultsModel;
    
    private final IconManager iconManager;
    
    private CategoryFilter categoryFilter;
    private Filter bitRateFilter;
    private Filter extensionFilter;
    private Filter fileSizeFilter;
    private Filter fileTypeFilter;
    private Filter lengthFilter;
    private Filter qualityFilter;
    private Map<FilePropertyKey, Filter> propertyFilterMap = new HashMap<FilePropertyKey, Filter>();
    
    /**
     * Constructs a FilterFactory using the specified search results data model
     * and icon manager.
     */
    public FilterFactory(SearchResultsModel searchResultsModel, IconManager iconManager) {
        this.searchResultsModel = searchResultsModel;
        this.iconManager = iconManager;
    }
    
    @Override
    public void dispose() {
        if (categoryFilter != null) {
            categoryFilter.dispose();
        }
        if (bitRateFilter != null) {
            bitRateFilter.dispose();
        }
        if (extensionFilter != null) {
            extensionFilter.dispose();
        }
        if (fileSizeFilter != null) {
            fileSizeFilter.dispose();
        }
        if (fileTypeFilter != null) {
            fileTypeFilter.dispose();
        }
        if (lengthFilter != null) {
            lengthFilter.dispose();
        }
        if (qualityFilter != null) {
            qualityFilter.dispose();
        }
        Collection<Filter> filters = propertyFilterMap.values();
        for (Filter filter : filters) {
            filter.dispose();
        }
    }
    
    /**
     * Returns a filter for file categories.
     */
    public CategoryFilter getCategoryFilter() {
        if (categoryFilter == null) {
            categoryFilter = new CategoryFilter(searchResultsModel.getFilteredSearchResults());
        }
        return categoryFilter;
    }
    
    /**
     * Returns a filter for file sources.
     */
    public Filter getSourceFilter() {
        return new SourceFilter();
    }
    
    /**
     * Returns an array of filters for the specified search category and list 
     * of search results.
     */
    public Filter[] getPropertyFilters(SearchCategory searchCategory) {
        // Get filtered results list.
        EventList<VisualSearchResult> filteredList = searchResultsModel.getFilteredSearchResults();
        
        // Create filter array.
        Filter[] filters = new Filter[0];
        
        // Return empty array if null.
        if (searchCategory == null) {
            return filters;
        }
        
        switch (searchCategory) {
        case AUDIO:
            filters = new Filter[7];
            filters[0] = getPropertyFilter(filteredList, FilePropertyKey.AUTHOR);
            filters[1] = getPropertyFilter(filteredList, FilePropertyKey.ALBUM);
            filters[2] = getPropertyFilter(filteredList, FilePropertyKey.GENRE);
            filters[3] = getFileSizeFilter();
            filters[4] = getExtensionFilter(filteredList);
            filters[5] = getBitRateFilter();
            filters[6] = getLengthFilter();
            return filters;
            
        case VIDEO:
            filters = new Filter[4];
            filters[0] = getFileSizeFilter();
            filters[1] = getExtensionFilter(filteredList);
            filters[2] = getQualityFilter();
            filters[3] = getLengthFilter();
            return filters;

        case DOCUMENT:
        case OTHER:
            filters = new Filter[3];
            filters[0] = getFileTypeFilter(filteredList);
            filters[1] = getFileSizeFilter();
            filters[2] = getExtensionFilter(filteredList);
            return filters;
            
        case ALL:
        case IMAGE:
        case PROGRAM:
        default:
            filters = new Filter[2];
            filters[0] = getFileSizeFilter();
            filters[1] = getExtensionFilter(filteredList);
            return filters;
        }
    }
    
    /**
     * Returns the bit rate filter.
     */
    private Filter getBitRateFilter() {
        if (bitRateFilter == null) {
            bitRateFilter = new BitRateFilter();
        }
        return bitRateFilter;
    }
    
    /**
     * Returns the file extension filter for the specified results list.
     */
    private Filter getExtensionFilter(EventList<VisualSearchResult> filteredList) {
        if (extensionFilter == null) {
            extensionFilter = new PropertyFilter(filteredList, FilterType.EXTENSION, null, iconManager);
        }
        return extensionFilter;
    }
    
    /**
     * Returns the file size filter.
     */
    private Filter getFileSizeFilter() {
        if (fileSizeFilter == null) {
            fileSizeFilter = new FileSizeFilter();
        }
        return fileSizeFilter;
    }
    
    /**
     * Returns the file type filter for the specified results list.
     */
    private Filter getFileTypeFilter(EventList<VisualSearchResult> filteredList) {
        if (fileTypeFilter == null) {
            fileTypeFilter = new PropertyFilter(filteredList, FilterType.TYPE, null, iconManager);
        }
        return fileTypeFilter;
    }
    
    /**
     * Returns the property value filter for the specified results list and
     * property key.
     */
    private Filter getPropertyFilter(EventList<VisualSearchResult> filteredList, 
            FilePropertyKey propertyKey) {
        Filter propertyFilter = propertyFilterMap.get(propertyKey); 
        if (propertyFilter == null) {
            propertyFilter = new PropertyFilter(filteredList, FilterType.PROPERTY, propertyKey, iconManager);
            propertyFilterMap.put(propertyKey, propertyFilter);
        }
        return propertyFilter;
    }
    
    /**
     * Returns the length filter.
     */
    private Filter getLengthFilter() {
        if (lengthFilter == null) {
            lengthFilter = new LengthFilter();
        }
        return lengthFilter;
    }
    
    /**
     * Returns the quality filter.
     */
    private Filter getQualityFilter() {
        if (qualityFilter == null) {
            qualityFilter = new QualityFilter();
        }
        return qualityFilter;
    }
}

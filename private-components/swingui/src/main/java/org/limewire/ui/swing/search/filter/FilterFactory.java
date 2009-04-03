package org.limewire.ui.swing.search.filter;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * A factory for creating filters.
 */
public class FilterFactory {

    private final SearchResultsModel searchResultsModel;
    
    /**
     * Constructs a FilterFactory using the specified search results data model.
     */
    public FilterFactory(SearchResultsModel searchResultsModel) {
        this.searchResultsModel = searchResultsModel;
    }
    
    /**
     * Returns a filter for file sources.
     */
    public Filter getSourceFilter() {
        return new SourceFilter();
    }
    
    /**
     * Returns an array of filters for the specified search category.
     */
    public Filter[] getFilters(SearchCategory searchCategory) {
        // Get results list for search category.
        FilterList<VisualSearchResult> categoryList = getUnfilteredCategoryList(searchCategory);
        
        Filter[] filters = new Filter[0];
        switch (searchCategory) {
        case AUDIO:
            filters = new Filter[5];
            filters[0] = new PropertyFilter(categoryList, FilterType.PROPERTY, FilePropertyKey.AUTHOR);
            filters[1] = new PropertyFilter(categoryList, FilterType.PROPERTY, FilePropertyKey.ALBUM);
            filters[2] = new PropertyFilter(categoryList, FilterType.PROPERTY, FilePropertyKey.GENRE);
            filters[3] = new PropertyFilter(categoryList, FilterType.EXTENSION, null);
            filters[4] = new FileSizeFilter();
            return filters;
            
        default:
            filters = new Filter[1];
            filters[0] = new PropertyFilter(categoryList, FilterType.EXTENSION, null);
            return filters;
        }
    }
    
    /**
     * Returns an unfiltered list of search results for the specified search
     * category.
     */
    private FilterList<VisualSearchResult> getUnfilteredCategoryList(SearchCategory searchCategory) {
        EventList<VisualSearchResult> unfilteredList = searchResultsModel.getObservableSearchResults();
        
        if (searchCategory == SearchCategory.ALL) {
            return GlazedListsFactory.filterList(unfilteredList);
        } else {
            Category category = searchCategory.getCategory();
            return GlazedListsFactory.filterList(unfilteredList, new CategoryMatcher(category));
        }
    }

    /**
     * A matcher used to filter search results by category.
     */
    private static class CategoryMatcher implements Matcher<VisualSearchResult> {
        private final Category category;
        
        public CategoryMatcher(Category category) {
            this.category = category;
        }

        @Override
        public boolean matches(VisualSearchResult vsr) {
            return (vsr.getCategory() == category);
        }
    }
}

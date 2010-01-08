package org.limewire.core.impl.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.inject.EagerSingleton;

import com.google.inject.Inject;

/**
 * Implementation of SearchManager for the live core.
 */
@EagerSingleton
public class CoreSearchManager implements SearchManager {

    private final List<SearchResultList> threadSafeSearchList;
    
    @Inject
    public CoreSearchManager() {
        this.threadSafeSearchList = new CopyOnWriteArrayList<SearchResultList>();
    }
    
    @Override
    public SearchResultList addSearch(Search search) {
        // Create result list.
        SearchResultList resultList = new CoreSearchResultList(search);
        
        // Add result list to collection.
        threadSafeSearchList.add(resultList);
        
        return resultList;
    }

    @Override
    public void removeSearch(Search search) {
        // Stop search.
        search.stop();
        
        // Remove result list from collection.
        for (Iterator<SearchResultList> iter = threadSafeSearchList.iterator(); iter.hasNext(); ) {
            SearchResultList resultList = iter.next();
            if (search.equals(resultList.getSearch())) {
                threadSafeSearchList.remove(resultList);
                break;
            }
        }
    }

    @Override
    public List<Search> getSearches() {
        List<Search> list = new ArrayList<Search>();
        
        for (Iterator<SearchResultList> iter = threadSafeSearchList.iterator(); iter.hasNext(); ) {
            SearchResultList resultList = iter.next();
            list.add(resultList.getSearch());
        }
        
        return list;
    }

    @Override
    public SearchResultList getSearchResultList(Search search) {
        // Return result list from collection.
        for (Iterator<SearchResultList> iter = threadSafeSearchList.iterator(); iter.hasNext(); ) {
            SearchResultList resultList = iter.next();
            if (search.equals(resultList.getSearch())) {
                return resultList;
            }
        }
        
        // Return null if search not found.
        return null;
    }
}

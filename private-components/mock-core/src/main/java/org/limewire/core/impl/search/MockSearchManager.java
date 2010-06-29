package org.limewire.core.impl.search;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.inject.LazySingleton;
import org.limewire.io.GUID;

import com.google.inject.Inject;

/**
 * Implementation of SearchManager for the mock core.
 */
@LazySingleton
public class MockSearchManager implements SearchManager {

    private final List<SearchResultList> threadSafeSearchList;

    @Inject
    public MockSearchManager() {
        this.threadSafeSearchList = new CopyOnWriteArrayList<SearchResultList>();
    }
    
    @Override
    public SearchResultList addMonitoredSearch(Search search, SearchDetails searchDetails) {
        return addSearch(search, searchDetails);
    }
    
    @Override
    public SearchResultList addSearch(Search search, SearchDetails searchDetails) {
        // Create result list.
        SearchResultList resultList = new MockSearchResultList(search, searchDetails);
        
        // Add result list to collection.
        threadSafeSearchList.add(resultList);
        
        return resultList;
    }

    @Override
    public void removeSearch(Search search) {
        // Dispose of result list and remove from collection.
        for (SearchResultList resultList : threadSafeSearchList) {
            if (search.equals(resultList.getSearch())) {
                resultList.dispose();
                threadSafeSearchList.remove(resultList);
                break;
            }
        }
    }
    
    @Override
    public void stopSearch(SearchResultList resultList) {
        resultList.getSearch().stop();
        resultList.dispose();
        threadSafeSearchList.remove(resultList);
    }
    
    @Override
    public List<SearchResultList> getActiveSearchLists() {
        return Collections.emptyList();
    }
    
    @Override
    public SearchResultList getSearchResultList(GUID guid) {
        // Return result list from collection.
        for (SearchResultList resultList : threadSafeSearchList) {
            if (guid.equals(resultList.getGuid())) {
                return resultList;
            }
        }
        
        // Return null if search not found.
        return null;
    }

    @Override
    public SearchResultList getSearchResultList(Search search) {
        // Return result list from collection.
        for (SearchResultList resultList : threadSafeSearchList) {
            if (search.equals(resultList.getSearch())) {
                return resultList;
            }
        }
        
        // Return null if search not found.
        return null;
    }
}

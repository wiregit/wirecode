package org.limewire.core.api.search;

import java.util.Collection;

/**
 * A listener that handles SearchResultList events.
 */
public interface SearchResultListListener {

    /**
     * Invoked when the specified collection of grouped results is added to 
     * the list.
     */
    void resultsCreated(Collection<GroupedSearchResult> results);
}

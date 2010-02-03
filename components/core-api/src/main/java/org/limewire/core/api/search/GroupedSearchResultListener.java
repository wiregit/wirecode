package org.limewire.core.api.search;

/**
 * A listener that is notified when a grouped search result changes.
 */
public interface GroupedSearchResultListener {

    /**
     * Invoked when sources are added to the grouped result.
     */
    void sourceAdded();
}

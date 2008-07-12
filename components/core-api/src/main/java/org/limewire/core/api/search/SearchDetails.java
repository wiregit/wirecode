package org.limewire.core.api.search;

/**
 * Details about how the search should be constructed.
 */
public interface SearchDetails {
    
    public SearchCategory getSearchCategory();
    
    public String getSearchQuery();

}

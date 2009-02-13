package org.limewire.core.api.search;

import org.limewire.listener.DefaultEvent;

public class SearchEvent extends DefaultEvent<Search, SearchEvent.Type> {

    public static enum Type {
        STARTED, STOPPED;
    }
    
    /** A simple search event. */
    // TODO: Change data from 'Search' to 'SearchController' or somesuch that
    //  has methods like:
    //     - addSearchResult(SearchResult)
    //     - addSponsoredResult(SponsoredResult)
    //     - isRepeatSearch() [boolean]
    //     - getSearch() [Search]
    // So that this event can be used to decouple who listens for searches and
    // supplies different kinds of results.
    public SearchEvent(Search data, Type type) {
        super(data, type);
    }
    
    

}

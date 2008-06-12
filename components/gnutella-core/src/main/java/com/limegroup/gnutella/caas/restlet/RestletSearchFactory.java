package com.limegroup.gnutella.caas.restlet;

import com.google.inject.Inject;
import com.limegroup.gnutella.caas.Search;
import com.limegroup.gnutella.caas.SearchFactory;
import com.limegroup.gnutella.caas.SearchParams;
import com.limegroup.gnutella.caas.SearchResultHandler;

public class RestletSearchFactory implements SearchFactory {

    @Inject
    public RestletSearchFactory() {
        
    }
    
    public Search createSearch(SearchParams params, SearchResultHandler handler) {
        return new RestletSearch(params, handler);
    }

}

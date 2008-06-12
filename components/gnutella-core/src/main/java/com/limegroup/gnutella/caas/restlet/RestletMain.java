package com.limegroup.gnutella.caas.restlet;

import com.limegroup.gnutella.caas.Search;
import com.limegroup.gnutella.caas.SearchParams;
import com.limegroup.gnutella.caas.SearchResult;
import com.limegroup.gnutella.caas.SearchResultHandler;

public class RestletMain {

    public static void main(String[] args) throws Exception {
        RestletConnector.setDefaultHost("127.0.0.1", 9090);
        SearchParams params = new SearchParams("limewire");
        SearchHandler handler = new SearchHandler();
        RestletSearch search = new RestletSearch(params, handler);
        
        search.start();
        
        while (true) {
            try { Thread.sleep(5000); }
            catch (Exception e) { }
            
            search.getMoreResults();
        }
    }
    
    
    
    /**
     * 
     */
    static class SearchHandler implements SearchResultHandler {
        
        public void handleSearchResult(Search s, SearchResult sr) {
            System.out.println("SearchHandler::handleSearchResult().. got one: " + sr.getFilename() + " @ " + sr.getHost() + ":" + sr.getPort());
        }
        
    }
    
}

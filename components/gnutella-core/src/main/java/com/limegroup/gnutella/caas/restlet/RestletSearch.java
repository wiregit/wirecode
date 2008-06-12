package com.limegroup.gnutella.caas.restlet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.limegroup.gnutella.caas.SearchParams;
import com.limegroup.gnutella.caas.SearchResultHandler;

public class RestletSearch implements com.limegroup.gnutella.caas.Search {

    private SearchParams _params;
    private SearchResultHandler _handler;
    private String _searchId;
    
    /**
     * 
     */
    public RestletSearch(SearchParams sp, SearchResultHandler srh) {
        _params = sp;
        _handler = srh;
    }
    
    /**
     * 
     */
    public void start() {
        Document document = RestletConnector.sendRequest("/search/?" + _params.getQueryString());
        Element searches = document.getDocumentElement();
        NodeList searchList = searches.getElementsByTagName("search");
        
        if (searchList.getLength() == 0)
            return;
        
        _searchId = searchList.item(0).getAttributes().getNamedItem("id").getTextContent();
    }
    
    /**
     * 
     */
    public void stop() {
        
    }
    
    /**
     * 
     */
    public void getMoreResults() {
        Document document = RestletConnector.sendRequest("/search/" + _searchId);
        Element searches = document.getDocumentElement();
        NodeList searchList = searches.getElementsByTagName("search");
        
        for (int i = 0; i < searchList.getLength(); ++i) {
            Element search = (Element)searchList.item(i);
            
            if (!_searchId.equals(search.getAttribute("id")))
                continue;
            
            NodeList resultList = search.getElementsByTagName("search_result");
            
            for (int j = 0; j < resultList.getLength(); ++j) {
                Element result = (Element)resultList.item(j);
                RestletSearchResult rsr = new RestletSearchResult(result);
                _handler.handleSearchResult(this, rsr);
            }
            
        }
    }
    
    /**
     * 
     */
    public SearchResultHandler getSearchResultHandler() {
        return _handler;
    }
    
    /**
     * 
     */
    public SearchParams getSearchParams() {
        return _params;
    }
    
}

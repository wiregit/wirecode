package org.limewire.rest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResultList;

import com.google.inject.Inject;

/**
 * Request handler for Search services.
 */
class SearchRequestHandler extends AbstractRestRequestHandler {

    private static final String ALL = "";
    
    private final SearchManager searchManager;
    
    @Inject
    public SearchRequestHandler(SearchManager searchManager) {
        this.searchManager = searchManager;
    }
    
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        
        String method = request.getRequestLine().getMethod();
        if (GET.equals(method)) {
            // Get uri target.
            String uriTarget = getUriTarget(request, RestPrefix.SEARCH.pattern());

            // Get query parameters.
            Map<String, String> queryParams = getQueryParams(request);

            // Set response.
            process(uriTarget, queryParams, response);

        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }

    /**
     * Processes the specified uri target and query parameters.
     */
    private void process(String uriTarget, Map<String, String> queryParams, HttpResponse response) 
            throws IOException {
        
        if (ALL.equals(uriTarget)) {
            // Get active search lists.
            List<SearchResultList> searchLists = searchManager.getActiveSearchLists();
            
            try {
                // Create JSON result.
                JSONArray jsonArr = new JSONArray();
                for (SearchResultList searchList : searchLists) {
                    jsonArr.put(createSearchDescription(searchList));
                }

                // Set response entity and status.
                HttpEntity entity = createStringEntity(jsonArr.toString(2));
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);

            } catch (JSONException ex) {
                throw new IOException(ex);
            }
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }
    
    /**
     * Creates the search description object for the specified search list.
     */
    private JSONObject createSearchDescription(SearchResultList searchList) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", searchList.getSearchQuery());
        jsonObj.put("size", searchList.getGroupedResults().size());
        jsonObj.put("id", searchList.getGuid());
        return jsonObj;
    }
}

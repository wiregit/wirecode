package org.limewire.rest;

import java.io.IOException;
import java.util.ArrayList;
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
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResultList;

import com.google.inject.Inject;

/**
 * Request handler for Search services.
 */
class SearchRequestHandler extends AbstractRestRequestHandler {

    private static final String ALL = "";
    private static final String FILES = "/files";
    private static final int MAX_LIMIT = 50;
    
    private final SearchManager searchManager;
    
    @Inject
    public SearchRequestHandler(SearchManager searchManager) {
        this.searchManager = searchManager;
    }
    
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context)
            throws HttpException, IOException {
        
        String method = request.getRequestLine().getMethod();
        if (RestUtils.GET.equals(method)) {
            // Get uri target.
            String uriTarget = RestUtils.getUriTarget(request, RestPrefix.SEARCH.pattern());

            // Get query parameters.
            Map<String, String> queryParams = RestUtils.getQueryParams(request);

            // Set response.
            processGet(uriTarget, queryParams, response);

        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }

    /**
     * Processes the specified uri target and query parameters.
     */
    private void processGet(String uriTarget, Map<String, String> queryParams, HttpResponse response) 
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
                HttpEntity entity = RestUtils.createStringEntity(jsonArr.toString(2));
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);

            } catch (JSONException ex) {
                throw new IOException(ex);
            }
            
        } else if (uriTarget.length() > 0) {
            // Get GUID string.
            String guidStr = parseGuid(uriTarget);
            
            // Get search result list; return empty object if null.
            SearchResultList searchList = searchManager.getSearchResultList(guidStr);
            if (searchList == null) {
                response.setEntity(RestUtils.createStringEntity("{}"));
                response.setStatusCode(HttpStatus.SC_OK);
                return;
            }
            
            try {
                if (uriTarget.indexOf(FILES) < 0) {
                    // Return search metadata.
                    JSONObject jsonObj = createSearchDescription(searchList);
                    HttpEntity entity = RestUtils.createStringEntity(jsonObj.toString());
                    response.setEntity(entity);
                    response.setStatusCode(HttpStatus.SC_OK);

                } else {
                    // Get query parameters.
                    String offsetStr = queryParams.get("offset");
                    String limitStr = queryParams.get("limit");
                    int offset = (offsetStr != null) ? Integer.parseInt(offsetStr) : 0;
                    int limit = (limitStr != null) ? Math.min(Integer.parseInt(limitStr), MAX_LIMIT) : MAX_LIMIT;
                    
                    // Create search result array.
                    List<GroupedSearchResult> resultList = new ArrayList<GroupedSearchResult>(searchList.getGroupedResults());
                    JSONArray jsonArr = new JSONArray();
                    for (int i = offset, max = Math.min(offset + limit, resultList.size()); i < max; i++) {
                        GroupedSearchResult result = resultList.get(i);
                        jsonArr.put(createResultDescription(result));
                    }
                    
                    // Set response entity and status.
                    HttpEntity entity = RestUtils.createStringEntity(jsonArr.toString(2));
                    response.setEntity(entity);
                    response.setStatusCode(HttpStatus.SC_OK);
                }
                
            } catch (JSONException ex) {
                throw new IOException(ex);
            }
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }
    
    /**
     * Creates the JSON description object for the specified grouped result.
     */
    private JSONObject createResultDescription(GroupedSearchResult result) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("filename", result.getFileName());
        jsonObj.put("category", result.getSearchResults().get(0).getCategory());
        jsonObj.put("sources", result.getSources().size());
        return jsonObj;
    }
    
    /**
     * Creates the JSON description object for the specified search list.
     */
    private JSONObject createSearchDescription(SearchResultList searchList) throws JSONException {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", searchList.getSearchQuery());
        jsonObj.put("size", searchList.getGroupedResults().size());
        jsonObj.put("id", searchList.getGuid());
        return jsonObj;
    }
    
    /**
     * Returns the GUID string from the specified URI target.
     */
    private String parseGuid(String uriTarget) {
        int pos = uriTarget.indexOf(FILES);
        return (pos < 0) ? uriTarget.substring(1) : uriTarget.substring(1, pos);
    }
}

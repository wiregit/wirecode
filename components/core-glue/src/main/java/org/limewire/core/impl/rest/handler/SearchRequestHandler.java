package org.limewire.core.impl.rest.handler;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.protocol.HttpContext;
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
            String uriTarget = getUriTarget(request, RestTarget.SEARCH.pattern());

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
            
            // Create result string.
            StringBuilder builder = new StringBuilder();
            builder.append("[\n");
            for (SearchResultList searchList : searchLists) {
                builder.append(createSearchDescription(searchList)).append("\n");
            }
            builder.append("]");
            
            // Set response entity and status.
            NStringEntity entity = new NStringEntity(builder.toString());
            response.setEntity(entity);
            response.setStatusCode(HttpStatus.SC_OK);
            
        } else {
            response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        }
    }
    
    /**
     * Creates the search description string for the specified search list.
     */
    private String createSearchDescription(SearchResultList searchList) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"name\": \"").append(searchList.getSearchQuery()).append("\", ");
        builder.append("\"size\": ").append(searchList.getGroupedResults().size()).append(", ");
        builder.append("\"id\": \"").append(searchList.getGuid()).append("\"");
        builder.append("}");
        return builder.toString();
    }
}

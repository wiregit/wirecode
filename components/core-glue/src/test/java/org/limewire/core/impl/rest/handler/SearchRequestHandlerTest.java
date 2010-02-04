package org.limewire.core.impl.rest.handler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.json.JSONArray;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * JUnit test case for LibraryRequestHandler.
 */
public class SearchRequestHandlerTest extends BaseTestCase {
    /** Instance of class being tested. */
    private SearchRequestHandler requestHandler;

    private Mockery context = new Mockery();

    /**
     * Constructs a test case for the specified method name.
     */
    public SearchRequestHandlerTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Create mock result list.
        final List<SearchResultList> mockSearchList = new ArrayList<SearchResultList>();
        final EventList<GroupedSearchResult> groupedResults = new BasicEventList<GroupedSearchResult>();
        
        // Create mock objects.
        final SearchManager mockSearchManager = context.mock(SearchManager.class);
        final SearchResultList mockResultList = context.mock(SearchResultList.class);
        final GroupedSearchResult mockResult = context.mock(GroupedSearchResult.class);
        mockSearchList.add(mockResultList);
        groupedResults.add(mockResult);
        
        context.checking(new Expectations() {{
            allowing(mockSearchManager).getActiveSearchLists();
            will(returnValue(mockSearchList));
            
            allowing(mockResultList).getGuid();
            will(returnValue(new GUID("BA8DB600AC11FE2EE3033F5AFF57F500")));
            allowing(mockResultList).getSearchQuery();
            will(returnValue("test"));
            allowing(mockResultList).getGroupedResults();
            will(returnValue(groupedResults));
            
            allowing(mockResult);
        }});
        
        // Create instance to be tested.
        requestHandler = new SearchRequestHandler(mockSearchManager);
    }

    @Override
    protected void tearDown() throws Exception {
        requestHandler = null;
        super.tearDown();
    }

    /** Tests method to handle request for searches. */
    public void testHandleSearches() throws Exception {
        final String testUri = "http://localhost/remote/search";
        
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        final HttpContext mockContext = context.mock(HttpContext.class);
        
        HttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, ""));
        
        context.checking(new Expectations() {{
            allowing(mockRequestLine).getMethod();
            will(returnValue(AbstractRestRequestHandler.GET));
            allowing(mockRequestLine).getUri();
            will(returnValue(testUri));
            
            allowing(mockRequest).getRequestLine();
            will(returnValue(mockRequestLine));
            
            allowing(mockContext);
        }});
        
        // Handle request.
        requestHandler.handle(mockRequest, response, mockContext);
        
        // Verify response values.
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        
        // Convert response entity to text.
        HttpEntity entity = response.getEntity();
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        String line = reader.readLine();
        while (line != null) {
            builder.append(line).append("\n");
            line = reader.readLine();
        }
        reader.close();
        
        // Convert text to JSON and verify.
        JSONArray jsonArr = new JSONArray(builder.toString());
        assertEquals(1, jsonArr.length());
    }
}

package org.limewire.rest;

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
import org.apache.http.util.EntityUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.json.JSONArray;
import org.json.JSONObject;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.io.GUID;
import org.limewire.io.URN;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * JUnit test case for SearchRequestHandler.
 */
public class SearchRequestHandlerTest extends BaseTestCase {
    private static final String SEARCH_GUID = "BA8DB600AC11FE2EE3033F5AFF57F500";
    private static final String SEARCH_QUERY = "test";
    private static final String SEARCH_FILENAME = "test.mpg";
    private static final URN SEARCH_URN = new MockURN("test");
    
    /** Instance of class being tested. */
    private SearchRequestHandler requestHandler;

    private Mockery context;
    private SearchFactory mockSearchFactory;
    private SearchManager mockSearchManager;
    private SearchResultList mockResultList;
    private GroupedSearchResult mockGroupedResult;
    private SearchResult mockSearchResult;
    
    private List<SearchResultList> searchResultLists;
    private EventList<GroupedSearchResult> groupedResults;
    private List<SearchResult> searchResults;
    private List<RemoteHost> remoteHosts;

    /**
     * Constructs a test case for the specified method name.
     */
    public SearchRequestHandlerTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Create mock objects.
        context = new Mockery();
        mockSearchFactory = context.mock(SearchFactory.class);
        mockSearchManager = context.mock(SearchManager.class);
        mockResultList = context.mock(SearchResultList.class);
        mockGroupedResult = context.mock(GroupedSearchResult.class);
        mockSearchResult = context.mock(SearchResult.class);
        
        // Create test collections.
        searchResultLists = new ArrayList<SearchResultList>();
        groupedResults = new BasicEventList<GroupedSearchResult>();
        searchResults = new ArrayList<SearchResult>();
        remoteHosts = new ArrayList<RemoteHost>();
        searchResultLists.add(mockResultList);
        groupedResults.add(mockGroupedResult);
        searchResults.add(mockSearchResult);

        // Create instance to be tested.
        requestHandler = new SearchRequestHandler(mockSearchManager, mockSearchFactory);
    }

    @Override
    protected void tearDown() throws Exception {
        requestHandler = null;
        mockSearchManager = null;
        mockResultList = null;
        mockGroupedResult = null;
        mockSearchResult = null;
        searchResultLists = null;
        groupedResults = null;
        searchResults = null;
        remoteHosts = null;
        context = null;
        super.tearDown();
    }

    /** Tests method to handle request for all searches. */
    public void testGetAllSearches() throws Exception {
        final String testUri = "http://localhost/remote/search";
        
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        final HttpContext mockContext = context.mock(HttpContext.class);
        
        HttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, ""));
        
        context.checking(new Expectations() {{
            allowing(mockSearchManager).getActiveSearchLists();
            will(returnValue(searchResultLists));
            
            allowing(mockResultList).getGuid();
            will(returnValue(new GUID(SEARCH_GUID)));
            allowing(mockResultList).getSearchQuery();
            will(returnValue(SEARCH_QUERY));
            allowing(mockResultList).getGroupedResults();
            will(returnValue(groupedResults));
            
            allowing(mockRequestLine).getMethod();
            will(returnValue(RestUtils.GET));
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
        String entityStr = EntityUtils.toString(entity);
        
        // Convert text to JSON and verify one search.
        JSONArray jsonArr = new JSONArray(entityStr);
        assertEquals(1, jsonArr.length());
    }
    
    /** Tests method to handle request for search metadata. */
    public void testGetSearchData() throws Exception {
        final String testUri = "http://localhost/remote/search/" + SEARCH_GUID;
        final GUID testGuid = new GUID(SEARCH_GUID);
        
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        final HttpContext mockContext = context.mock(HttpContext.class);
        
        HttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, ""));
        
        context.checking(new Expectations() {{
            allowing(mockSearchManager).getSearchResultList(testGuid);
            will(returnValue(mockResultList));
            
            allowing(mockResultList).getGuid();
            will(returnValue(new GUID(SEARCH_GUID)));
            allowing(mockResultList).getSearchQuery();
            will(returnValue(SEARCH_QUERY));
            allowing(mockResultList).getGroupedResults();
            will(returnValue(groupedResults));
            
            allowing(mockRequestLine).getMethod();
            will(returnValue(RestUtils.GET));
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
        String entityStr = EntityUtils.toString(entity);
        
        // Convert text to JSON and verify.
        JSONObject jsonObj = new JSONObject(entityStr);
        assertEquals(SEARCH_QUERY, jsonObj.getString("name"));
        assertEquals(SEARCH_GUID, jsonObj.getString("id"));
    }
    
    /** Tests method to handle request for search results. */
    public void testGetSearchResults() throws Exception {
        final String testUri = "http://localhost/remote/search/" + SEARCH_GUID + "/files";
        final GUID testGuid = new GUID(SEARCH_GUID);
        
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        final HttpContext mockContext = context.mock(HttpContext.class);
        
        HttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, ""));
        
        context.checking(new Expectations() {{
            allowing(mockSearchManager).getSearchResultList(testGuid);
            will(returnValue(mockResultList));
            
            allowing(mockResultList).getGroupedResults();
            will(returnValue(groupedResults));
            
            allowing(mockGroupedResult).getFileName();
            will(returnValue(SEARCH_FILENAME));
            allowing(mockGroupedResult).getSearchResults();
            will(returnValue(searchResults));
            allowing(mockGroupedResult).getSources();
            will(returnValue(remoteHosts));
            allowing(mockGroupedResult).getUrn();
            will(returnValue(SEARCH_URN));
            
            allowing(mockSearchResult).getCategory();
            will(returnValue(Category.AUDIO));
            allowing(mockSearchResult).getSize();
            will(returnValue(16384L));
            allowing(mockSearchResult).getMagnetURL();
            will(returnValue("magnet:?mt=whatever"));
            allowing(mockSearchResult).isSpam();
            will(returnValue(false));
            allowing(mockSearchResult).getProperty(FilePropertyKey.AUTHOR);
            will(returnValue("artist"));
            allowing(mockSearchResult).getProperty(FilePropertyKey.ALBUM);
            will(returnValue("album"));
            allowing(mockSearchResult).getProperty(FilePropertyKey.GENRE);
            will(returnValue("genre"));
            allowing(mockSearchResult).getProperty(FilePropertyKey.TITLE);
            will(returnValue("title"));
            
            allowing(mockRequestLine).getMethod();
            will(returnValue(RestUtils.GET));
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
        String entityStr = EntityUtils.toString(entity);
        
        // Convert text to JSON and verify one result.
        JSONArray jsonArr = new JSONArray(entityStr);
        assertEquals(1, jsonArr.length());
    }
}

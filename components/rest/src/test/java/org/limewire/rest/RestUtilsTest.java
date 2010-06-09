package org.limewire.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.json.JSONObject;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.search.GroupedSearchResult;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchResultList;
import org.limewire.io.GUID;
import org.limewire.io.URN;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * JUnit test case for RestUtils.
 */
public class RestUtilsTest extends BaseTestCase {
    private static final String SEARCH_GUID = "BA8DB600AC11FE2EE3033F5AFF57F500";

    private Mockery context = new Mockery();

    /**
     * Constructs a test case for the specified method name.
     */
    public RestUtilsTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /** Tests method to perform percent decoding. */
    public void testPercentDecode() {
        // Verify unreserved characters not decoded.
        String testStr = "first_name-last_name.bak~";
        assertEquals(testStr, RestUtils.percentDecode(testStr));
        
        // Verify URL string decoded.
        testStr = "http%3A%2F%2Fwww.limewire.com";
        String expected = "http://www.limewire.com";
        assertEquals(expected, RestUtils.percentDecode(testStr));
        
        // Verify parameter string decoded.
        testStr = "find%3Falpha%3D1%26beta%3D2%2B3";
        expected = "find?alpha=1&beta=2+3";
        assertEquals(expected, RestUtils.percentDecode(testStr));
        
        // Verify plus sign unchanged in decoded signature.
        testStr = "ozpkOJpFG+DnBl4IX4vHUJgWRUo%3D";
        expected = "ozpkOJpFG+DnBl4IX4vHUJgWRUo=";
        assertEquals(expected, RestUtils.percentDecode(testStr));
    }

    /** Tests method to perform percent encoding. */
    public void testPercentEncode() {
        // Verify unreserved characters not encoded.
        String testStr = "first_name-last_name.bak~";
        assertEquals(testStr, RestUtils.percentEncode(testStr));
        
        // Verify URL string encoded.
        testStr = "http://www.limewire.com";
        String expected = "http%3A%2F%2Fwww.limewire.com";
        assertEquals(expected, RestUtils.percentEncode(testStr));
        
        // Verify parameter string encoded.
        testStr = "find?alpha=1&beta=2+3";
        expected = "find%3Falpha%3D1%26beta%3D2%2B3";
        assertEquals(expected, RestUtils.percentEncode(testStr));
    }

    /** Tests method to perform percent encoding on space characters. */
    public void testPercentEncodeSpace() {
        // Verify plus sign converted to encoded space.
        String testStr = "parm=hello world";
        String expected = "parm%3Dhello%20world";
        assertEquals(expected, RestUtils.percentEncode(testStr));
    }
    
    /** Tests method to get base URI string. */
    public void testGetBaseUri() throws Exception {
        String testUri = "http://localhost/remote/library/files?offset=1";
        String baseUri = "http://localhost/remote/library/files";
        
        assertEquals(baseUri, RestUtils.getBaseUri(testUri));
    }

    /** Tests method to get URI target. */
    public void testGetUriTarget() throws Exception {
        final String testUri = "http://localhost/remote/library/files?offset=1";
        final String testPrefix = "/library";
        
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        context.checking(new Expectations() {{
            allowing(mockRequestLine).getUri();
            will(returnValue(testUri));
            allowing(mockRequest).getRequestLine();
            will(returnValue(mockRequestLine));
        }});
        
        assertEquals("/files", RestUtils.getUriTarget(mockRequest, testPrefix));
    }

    /** Tests method to get query parameters from HTTP request. */
    public void testGetQueryParamsFromRequest() throws Exception {
        final String testMethod = "GET";
        final String testUri = "http://localhost/remote/library/files?offset=1";
        
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        context.checking(new Expectations() {{
            allowing(mockRequestLine).getMethod();
            will(returnValue(testMethod));
            allowing(mockRequestLine).getUri();
            will(returnValue(testUri));
            allowing(mockRequest).getRequestLine();
            will(returnValue(mockRequestLine));
        }});
        
        Map<String, String> queryParams = RestUtils.getQueryParams(mockRequest);
        assertEquals(1, queryParams.size());
        assertEquals("1", queryParams.get("offset"));
    }

    /** Tests method to get query parameters from URI string. */
    public void testGetQueryParamsFromString() throws Exception {
        String testUri = "http://localhost/remote/library/files?offset=1";
        
        Map<String, String> queryParams = RestUtils.getQueryParams(testUri);
        assertEquals(1, queryParams.size());
        assertEquals("1", queryParams.get("offset"));
    }
    
    /** Tests method to create file item JSON. */
    public void testCreateFileItemJson() throws Exception {
        final String urn = "urn:sha1:BA8DB600AC11FE2EE3033F5AFF57F500";
        final String testFile = "test.mp3";
        final long testSize = 16384;
        final URN testUrn = new MockURN("urn:sha1:" + urn);
        final String testArtist = "musician";
        final String testAlbum = "hitsville";
        final String testGenre = "rock";
        final String testTitle = "numberone";
        
        // Create mock file item.
        final LocalFileItem mockFileItem = context.mock(LocalFileItem.class);
        context.checking(new Expectations() {{
            allowing(mockFileItem).getFileName();
            will(returnValue(testFile));
            allowing(mockFileItem).getCategory();
            will(returnValue(Category.AUDIO));
            allowing(mockFileItem).getSize();
            will(returnValue(testSize));
            allowing(mockFileItem).getUrn();
            will(returnValue(testUrn));
            allowing(mockFileItem).getProperty(FilePropertyKey.AUTHOR);
            will(returnValue(testArtist));
            allowing(mockFileItem).getProperty(FilePropertyKey.ALBUM);
            will(returnValue(testAlbum));
            allowing(mockFileItem).getProperty(FilePropertyKey.GENRE);
            will(returnValue(testGenre));
            allowing(mockFileItem).getProperty(FilePropertyKey.TITLE);
            will(returnValue(testTitle));
        }});
        
        // Create JSON.
        JSONObject jsonObj = RestUtils.createFileItemJson(mockFileItem);
        
        assertEquals(testFile, jsonObj.get("filename"));
        assertEquals(Category.AUDIO.getSchemaName(), jsonObj.get("category"));
        assertEquals(testSize, jsonObj.getLong("size"));
        assertEquals(testUrn, jsonObj.get("id"));
        assertEquals(testArtist, jsonObj.get("artist"));
        assertEquals(testAlbum, jsonObj.get("album"));
        assertEquals(testGenre, jsonObj.get("genre"));
        assertEquals(testTitle, jsonObj.get("title"));
    }
    
    /** Tests method to create library JSON. */
    public void testCreateLibraryJson() throws Exception {
        // Create mock library.
        final LibraryFileList mockFileList = context.mock(LibraryFileList.class);
        context.checking(new Expectations() {{
            allowing(mockFileList).size();
            will(returnValue(3));
        }});
        
        // Create JSON.
        JSONObject jsonObj = RestUtils.createLibraryJson(mockFileList);
        
        assertEquals("Library", jsonObj.get("name"));
        assertEquals(3, jsonObj.getInt("size"));
        assertEquals("library", jsonObj.get("id"));
    }
    
    /** Tests method to create search JSON. */
    public void testCreateSearchJson() throws Exception {
        // Create mock search.
        final GUID searchGuid = new GUID(SEARCH_GUID);
        final GroupedSearchResult mockSearchResult = context.mock(GroupedSearchResult.class);
        final SearchResultList mockSearchList = context.mock(SearchResultList.class);
        final EventList<GroupedSearchResult> groupedResults = new BasicEventList<GroupedSearchResult>();
        groupedResults.add(mockSearchResult);
        context.checking(new Expectations() {{
            allowing(mockSearchList).getSearchQuery();
            will(returnValue("test"));
            allowing(mockSearchList).getGroupedResults();
            will(returnValue(groupedResults));
            allowing(mockSearchList).getGuid();
            will(returnValue(searchGuid));
        }});
        
        // Create JSON.
        JSONObject jsonObj = RestUtils.createSearchJson(mockSearchList);
        
        assertEquals("test", jsonObj.get("name"));
        assertEquals(1, jsonObj.getInt("size"));
        assertEquals(searchGuid, jsonObj.get("id"));
    }
    
    /** Tests method to create search result JSON. */
    public void testCreateSearchResultJson() throws Exception {
        final String urnId = "BA8DB600AC11FE2EE3033F5AFF57F500";
        final String testFile = "test.mp3";
        final long testSize = 16384;
        final URN testUrn = new MockURN("urn:sha1:" + urnId);
        final String testMagnet = "magnet:?mt=whatever";
        final String testArtist = "musician";
        final String testAlbum = "hitsville";
        final String testGenre = "rock";
        final String testTitle = "numberone";
        
        // Create mock search result.
        final GroupedSearchResult mockGroupedResult = context.mock(GroupedSearchResult.class);
        final SearchResult mockSearchResult = context.mock(SearchResult.class);
        final List<SearchResult> searchResults = new ArrayList<SearchResult>();
        searchResults.add(mockSearchResult);
        context.checking(new Expectations() {{
            allowing(mockGroupedResult).getFileName();
            will(returnValue(testFile));
            allowing(mockGroupedResult).getSearchResults();
            will(returnValue(searchResults));
            allowing(mockGroupedResult).getSources();
            will(returnValue(Collections.emptyList()));
            allowing(mockGroupedResult).getUrn();
            will(returnValue(testUrn));
            
            allowing(mockSearchResult).getCategory();
            will(returnValue(Category.AUDIO));
            allowing(mockSearchResult).getSize();
            will(returnValue(testSize));
            allowing(mockSearchResult).getMagnetURL();
            will(returnValue(testMagnet));
            allowing(mockSearchResult).isSpam();
            will(returnValue(false));
            allowing(mockSearchResult).getProperty(FilePropertyKey.AUTHOR);
            will(returnValue(testArtist));
            allowing(mockSearchResult).getProperty(FilePropertyKey.ALBUM);
            will(returnValue(testAlbum));
            allowing(mockSearchResult).getProperty(FilePropertyKey.GENRE);
            will(returnValue(testGenre));
            allowing(mockSearchResult).getProperty(FilePropertyKey.TITLE);
            will(returnValue(testTitle));
        }});
        
        // Create JSON.
        JSONObject jsonObj = RestUtils.createSearchResultJson(mockGroupedResult);
        
        assertEquals(testFile, jsonObj.get("filename"));
        assertEquals(Category.AUDIO.getSchemaName(), jsonObj.get("category"));
        assertEquals(testSize, jsonObj.getLong("size"));
        assertEquals(testUrn, jsonObj.get("id"));
        assertEquals(testMagnet, jsonObj.get("magnetUrl"));
        assertEquals(0, jsonObj.getInt("sources"));
        assertEquals(false, jsonObj.getBoolean("spam"));
        assertEquals(testArtist, jsonObj.get("artist"));
        assertEquals(testAlbum, jsonObj.get("album"));
        assertEquals(testGenre, jsonObj.get("genre"));
        assertEquals(testTitle, jsonObj.get("title"));
    }
}

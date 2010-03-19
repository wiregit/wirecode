package org.limewire.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
import org.json.JSONObject;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.library.URNFactory;
import org.limewire.core.api.magnet.MagnetFactory;
import org.limewire.core.api.search.SearchManager;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * JUnit test case for DownloadRequestHandler.
 */
public class DownloadRequestHandlerTest extends BaseTestCase {
    private static final String DOWNLOAD_FILENAME = "test.jpg";
    private static final String DOWNLOAD_URN = "urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    
    /** Instance of class being tested. */
    private DownloadRequestHandler requestHandler;

    private Mockery context;
    private DownloadListManager mockDownloadListManager;
    private SearchManager mockSearchManager;
    private MagnetFactory mockMagnetFactory;
    private URNFactory mockUrnFactory;
    private DownloadItem mockDownloadItem;
    
    private EventList<DownloadItem> downloadItems;
    
    /**
     * Constructs a test case for the specified method name.
     */
    public DownloadRequestHandlerTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Create mock objects.
        context = new Mockery();
        mockDownloadListManager = context.mock(DownloadListManager.class);
        mockSearchManager = context.mock(SearchManager.class);
        mockMagnetFactory = context.mock(MagnetFactory.class);
        mockUrnFactory = context.mock(URNFactory.class);
        mockDownloadItem = context.mock(DownloadItem.class);
        
        // Create test collections.
        downloadItems = new BasicEventList<DownloadItem>();
        downloadItems.add(mockDownloadItem);
        
        // Create instance to be tested.
        requestHandler = new DownloadRequestHandler(mockDownloadListManager,
                mockSearchManager, mockMagnetFactory, mockUrnFactory);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /** Tests method to handle request for all downloads. */
    public void testGetAllDownloads() throws Exception {
        final String testUri = "http://localhost/remote/download";
        
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        final HttpContext mockContext = context.mock(HttpContext.class);
        
        HttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, ""));
        
        context.checking(new Expectations() {{
            allowing(mockDownloadListManager).getDownloads();
            will(returnValue(downloadItems));
            
            allowing(mockDownloadItem).getFileName();
            will(returnValue("filename"));
            allowing(mockDownloadItem).getUrn();
            will(returnValue(new MockURN(DOWNLOAD_URN)));
            allowing(mockDownloadItem).getTotalSize();
            will(returnValue(32768L));
            allowing(mockDownloadItem).getCurrentSize();
            will(returnValue(16384L));
            allowing(mockDownloadItem).getState();
            will(returnValue(DownloadState.DOWNLOADING));
            
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
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        String line = reader.readLine();
        while (line != null) {
            builder.append(line).append("\n");
            line = reader.readLine();
        }
        reader.close();
        
        // Convert text to JSON and verify one download.
        JSONArray jsonArr = new JSONArray(builder.toString());
        assertEquals(1, jsonArr.length());
    }

    /** Tests method to handle request for download progress. */
    public void testGetProgress() throws Exception {
        final String testUri = "http://localhost/remote/download/" + DOWNLOAD_URN;
        final URN testUrn = new MockURN(DOWNLOAD_URN);
        final Long testSize = Long.valueOf(32768L);
        final Long testBytes = Long.valueOf(16384L);
        final DownloadState testState = DownloadState.DOWNLOADING;
        
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        final HttpContext mockContext = context.mock(HttpContext.class);
        
        HttpResponse response = new BasicHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, ""));
        
        context.checking(new Expectations() {{
            allowing(mockUrnFactory).createSHA1Urn(DOWNLOAD_URN);
            will(returnValue(testUrn));
            
            allowing(mockDownloadListManager).getDownloadItem(testUrn);
            will(returnValue(mockDownloadItem));
            
            allowing(mockDownloadItem).getFileName();
            will(returnValue(DOWNLOAD_FILENAME));
            allowing(mockDownloadItem).getUrn();
            will(returnValue(testUrn));
            allowing(mockDownloadItem).getTotalSize();
            will(returnValue(testSize));
            allowing(mockDownloadItem).getCurrentSize();
            will(returnValue(testBytes));
            allowing(mockDownloadItem).getState();
            will(returnValue(testState));
            
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
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        String line = reader.readLine();
        while (line != null) {
            builder.append(line).append("\n");
            line = reader.readLine();
        }
        reader.close();
        
        // Convert text to JSON and verify.
        JSONObject jsonObj = new JSONObject(builder.toString());
        assertEquals(DOWNLOAD_FILENAME, jsonObj.getString("filename"));
        assertEquals(DOWNLOAD_URN, jsonObj.getString("id"));
        assertEquals(testSize.toString(), jsonObj.getString("size"));
        assertEquals(testBytes.toString(), jsonObj.getString("bytesDownloaded"));
        assertEquals(testState.toString(), jsonObj.getString("state"));
    }
}

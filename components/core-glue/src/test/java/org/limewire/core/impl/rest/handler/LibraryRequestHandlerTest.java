package org.limewire.core.impl.rest.handler;

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
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * JUnit test case for LibraryRequestHandler.
 */
public class LibraryRequestHandlerTest extends BaseTestCase {
    /** Instance of class being tested. */
    private LibraryRequestHandler requestHandler;

    private Mockery context = new Mockery();
    
    /**
     * Constructs a test case for the specified method name.
     */
    public LibraryRequestHandlerTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Create mock file list.
        final EventList<LocalFileItem> mockModel = new BasicEventList<LocalFileItem>();
        
        // Create mock objects.
        final LibraryManager mockLibraryManager = context.mock(LibraryManager.class);
        final LibraryFileList mockFileList = context.mock(LibraryFileList.class);
        final LocalFileItem mockFileItem1 = context.mock(LocalFileItem.class);
        final LocalFileItem mockFileItem2 = context.mock(LocalFileItem.class);
        mockModel.add(mockFileItem1);
        mockModel.add(mockFileItem2);
        
        context.checking(new Expectations() {{
            allowing(mockLibraryManager).getLibraryManagedList();
            will(returnValue(mockFileList));
            
            allowing(mockFileList).getModel();
            will(returnValue(mockModel));
            allowing(mockFileList).size();
            will(returnValue(mockModel.size()));
            
            allowing(mockFileItem1).getFileName();
            will(returnValue("test1.jpg"));
            allowing(mockFileItem1).getCategory();
            will(returnValue(Category.IMAGE));
            allowing(mockFileItem1).getSize();
            will(returnValue((long) 1024));
            allowing(mockFileItem2).getFileName();
            will(returnValue("test2.jpg"));
            allowing(mockFileItem2).getCategory();
            will(returnValue(Category.IMAGE));
            allowing(mockFileItem2).getSize();
            will(returnValue((long) 1024));
        }});
        
        // Create instance to be tested.
        requestHandler = new LibraryRequestHandler(mockLibraryManager);
    }

    @Override
    protected void tearDown() throws Exception {
        requestHandler = null;
        super.tearDown();
    }
    
    /** Tests method to handle request for library metadata. */
    public void testHandleMetadata() throws Exception {
        final String testUri = "http://localhost/remote/library";
        
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
        
        // Verify response status.
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
        assertEquals("Library", jsonObj.get("name"));
        assertEquals(2, jsonObj.getInt("size"));
    }
    
    /** Tests method to handle request for library files. */
    public void testHandleFiles() throws Exception {
        final String testUri = "http://localhost/remote/library/files";
        
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
        assertEquals(2, jsonArr.length());
    }
}

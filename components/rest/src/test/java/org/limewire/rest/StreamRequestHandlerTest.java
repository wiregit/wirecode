package org.limewire.rest;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.URNFactory;
import org.limewire.http.handler.MimeTypeProvider;
import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

/**
 * JUnit test case for StreamRequestHandler.
 */
public class StreamRequestHandlerTest extends BaseTestCase {    
    /** Instance of class being tested. */
    private StreamRequestHandler requestHandler;

    private Mockery context;
    private LibraryManager mockLibraryManager;
    private MimeTypeProvider mockMimeProvider;
    private URNFactory mockUrnFactory;
    private StatusLine line;
    private HttpResponse response;

    /**
     * Constructs a test case for the specified method name.
     */
    public StreamRequestHandlerTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Create mock objects.
        context = new Mockery();
        mockLibraryManager = context.mock(LibraryManager.class);
        mockMimeProvider   = context.mock(MimeTypeProvider.class);
        mockUrnFactory     = context.mock(URNFactory.class);
        
        line = new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "");
        response = new BasicHttpResponse(line);
                       
        // Create instance to be tested.
        requestHandler = new StreamRequestHandler(mockLibraryManager, mockMimeProvider, mockUrnFactory);
    }

    @Override
    protected void tearDown() throws Exception {
        requestHandler = null;
        mockLibraryManager = null;
        mockMimeProvider   = null;
        context = null;
        
        line = null;
        response = null;
        
        super.tearDown();
    }
    
    public void get(String uri) throws HttpException, IOException {
        HttpRequest request = new HttpGet(uri);
        
        // Handle request.
        requestHandler.handle(request, response, new BasicHttpContext());
    }
    
    public void testGettingFileInLibrary() throws Exception {
        final File file = TestUtils.getResourceFile("build.xml");
        final LocalFileItem mockFileItem = context.mock(LocalFileItem.class);     
        final LibraryFileList lml = context.mock(LibraryFileList.class);
        
        context.checking(new Expectations() {{
          allowing(mockFileItem).getFile();
          will(returnValue(file));
            
          allowing(lml).getFileItem(with(any(URN.class)));
          will(returnValue(mockFileItem));
          
          allowing(mockUrnFactory).createSHA1Urn(with(any(String.class)));
          
          allowing(lml);
          
          allowing(mockLibraryManager).getLibraryManagedList();
          will(returnValue(lml));

          allowing(mockLibraryManager);
          allowing(mockMimeProvider);
        }});

        get("http://localhost/remote/stream/00000000000000000000000000000000");
        
        // Verify response values.
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        
        byte[] fileData = FileUtils.readFileFully(file); 
        byte[] streamData = EntityUtils.toByteArray(response.getEntity());

        // Assert body of response is same as file body.
        assertEquals(fileData, streamData);
    }
    
    public void testGettingFileNotInLibrary() throws Exception {
        final LibraryFileList lml = context.mock(LibraryFileList.class);
        
        context.checking(new Expectations() {{            
          allowing(lml).getFileItem(with(any(URN.class)));
          will(returnValue(null));

          allowing(mockUrnFactory).createSHA1Urn(with(any(String.class)));
          
          allowing(lml);
          
          allowing(mockLibraryManager).getLibraryManagedList();
          will(returnValue(lml));

          allowing(mockLibraryManager);
          allowing(mockMimeProvider);
        }});

        get("http://localhost/remote/stream/00000000000000000000000000000000");
        
        // Verify response values.
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusLine().getStatusCode());
    }
}

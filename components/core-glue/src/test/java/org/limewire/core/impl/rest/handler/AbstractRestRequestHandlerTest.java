package org.limewire.core.impl.rest.handler;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.protocol.HttpContext;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for AbstractRestRequestHandler.
 */
public class AbstractRestRequestHandlerTest extends BaseTestCase {
    /** Instance of class being tested. */
    private AbstractRestRequestHandler requestHandler;
    
    private Mockery context = new Mockery();

    /**
     * Constructs a test case for the specified method name.
     */
    public AbstractRestRequestHandlerTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        requestHandler = new AbstractRestRequestHandler() {
            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
            }
        };
    }

    @Override
    protected void tearDown() throws Exception {
        requestHandler = null;
        super.tearDown();
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
        
        assertEquals("/files", requestHandler.getUriTarget(mockRequest, testPrefix));
    }
    
    /** Tests method to get URI query parameters. */
    public void testGetQueryParams() throws Exception {
        final String testUri = "http://localhost/remote/library/files?offset=1";
        
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine mockRequestLine = context.mock(RequestLine.class);
        context.checking(new Expectations() {{
            allowing(mockRequestLine).getUri();
            will(returnValue(testUri));
            allowing(mockRequest).getRequestLine();
            will(returnValue(mockRequestLine));
        }});
        
        Map<String, String> queryParams = requestHandler.getQueryParams(mockRequest);
        assertEquals(1, queryParams.size());
        assertEquals("1", queryParams.get("offset"));
    }
}

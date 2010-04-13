package org.limewire.rest;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.apache.http.protocol.BasicHttpContext;
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
                response.setStatusCode(HttpStatus.SC_OK);
            }
        };
    }

    @Override
    protected void tearDown() throws Exception {
        requestHandler = null;
        super.tearDown();
    }

    /** Tests method to handle requests. */
    public void testHandle() throws Exception {
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final NHttpResponseTrigger mockTrigger = context.mock(NHttpResponseTrigger.class);
        
        HttpContext httpContext = new BasicHttpContext();
        HttpResponse httpResponse = new BasicHttpResponse(
                new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, ""));
        
        context.checking(new Expectations() {{
            allowing(mockRequest);
            allowing(mockTrigger);
        }});
        
        // Verify response status when request not authorized.
        requestHandler.handle(mockRequest, httpResponse, mockTrigger, httpContext);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, httpResponse.getStatusLine().getStatusCode());
        
        // Verify response status when request authorized.
        httpContext.setAttribute(AuthorizationInterceptor.AUTHORIZED, Boolean.TRUE);
        
        requestHandler.handle(mockRequest, httpResponse, mockTrigger, httpContext);
        assertEquals(HttpStatus.SC_OK, httpResponse.getStatusLine().getStatusCode());
    }
}

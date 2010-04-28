package org.limewire.rest;

import junit.framework.TestCase;

import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jmock.Expectations;
import org.jmock.Mockery;

/**
 * JUnit test case for AuthorizationInterceptor.
 */
public class AuthorizationInterceptorTest extends TestCase {
    /** Instance of class being tested. */
    private AuthorizationInterceptor interceptor;

    private Mockery context = new Mockery();
    private RestAuthority mockAuthority;

    /**
     * Constructs a test case for the specified method name.
     */
    public AuthorizationInterceptorTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Create mock authority.
        mockAuthority = context.mock(RestAuthority.class);
        
        // Create interceptor.
        interceptor = new AuthorizationInterceptor(mockAuthority);
    }

    @Override
    protected void tearDown() throws Exception {
        interceptor = null;
        mockAuthority = null;
        super.tearDown();
    }

    /** Tests method to process authorized HTTP request. */
    public void testProcessAuthorized() throws Exception {
        // Create mock reqeust.
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine requestLine = new BasicRequestLine("GET", "/remote/hello", HttpVersion.HTTP_1_1);
        
        context.checking(new Expectations() {{
            allowing(mockAuthority).isAuthorized(mockRequest);
            will(returnValue(true));
            
            allowing(mockRequest).getRequestLine();
            will(returnValue(requestLine));
        }});
        
        // Process request and verify value.
        HttpContext httpContext = new BasicHttpContext();
        interceptor.process(mockRequest, httpContext);
        assertTrue(((Boolean) httpContext.getAttribute(AuthorizationInterceptor.AUTHORIZED)).booleanValue());
    }

    /** Tests method to process unauthorized HTTP request. */
    public void testProcessUnauthorized() throws Exception {
        // Create mock reqeust.
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine requestLine = new BasicRequestLine("GET", "/remote/hello", HttpVersion.HTTP_1_1);
        
        context.checking(new Expectations() {{
            allowing(mockAuthority).isAuthorized(mockRequest);
            will(returnValue(false));
            
            allowing(mockRequest).getRequestLine();
            will(returnValue(requestLine));
        }});
        
        // Process request and verify value.
        HttpContext httpContext = new BasicHttpContext();
        interceptor.process(mockRequest, httpContext);
        assertFalse(((Boolean) httpContext.getAttribute(AuthorizationInterceptor.AUTHORIZED)).booleanValue());
    }

    /** Tests method to process invalid HTTP request. */
    public void testProcessInvalid() throws Exception {
        // Create invalid request.
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine requestLine = new BasicRequestLine("GET", "/hello", HttpVersion.HTTP_1_1);
        
        context.checking(new Expectations() {{
            allowing(mockAuthority).isAuthorized(mockRequest);
            will(returnValue(true));
            
            allowing(mockRequest).getRequestLine();
            will(returnValue(requestLine));
        }});
        
        // Process request and verify value.
        HttpContext httpContext = new BasicHttpContext();
        interceptor.process(mockRequest, httpContext);
        assertNull(httpContext.getAttribute(AuthorizationInterceptor.AUTHORIZED));
    }
}

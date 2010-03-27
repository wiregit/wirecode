package org.limewire.rest;

import junit.framework.TestCase;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicRequestLine;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.rest.oauth.OAuthException;
import org.limewire.rest.oauth.OAuthRequest;
import org.limewire.rest.oauth.OAuthValidator;
import org.limewire.rest.oauth.OAuthValidatorFactory;

/**
 * JUnit test case for RestAuthorityImpl.
 */
public class RestAuthorityImplTest extends TestCase {
    /** Instance of class being tested. */
    private RestAuthority restAuthority;
    
    private Mockery context = new Mockery();
    private OAuthValidator mockValidator;

    /**
     * Constructs a test case for the specified method name.
     */
    public RestAuthorityImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // Create mock factory and validator.
        final String baseUrl = "http://localhost";
        final int port = 45100;
        final String secret = "abcde";
        final OAuthValidatorFactory validatorFactory = context.mock(OAuthValidatorFactory.class);
        mockValidator = context.mock(OAuthValidator.class);
        
        context.checking(new Expectations() {{
            allowing(validatorFactory).create(baseUrl, port, secret);
            will(returnValue(mockValidator));
        }});
        
        // Create REST authority.
        restAuthority = new RestAuthorityImpl(baseUrl, port, secret, validatorFactory);
    }

    @Override
    protected void tearDown() throws Exception {
        restAuthority = null;
        super.tearDown();
    }

    /** Tests method to verify authorization. */
    public void testIsAuthorized() {
        // Create mock reqeust.
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine requestLine = new BasicRequestLine("GET", "/remote/hello", HttpVersion.HTTP_1_1);
        final Header[] headers = new Header[0];
        
        context.checking(new Expectations() {{
            allowing(mockRequest).getRequestLine();
            will(returnValue(requestLine));
            
            allowing(mockRequest).getHeaders(OAuthRequest.AUTH_HEADER);
            will(returnValue(headers));
            
            allowing(mockValidator);
        }});
        
        // Verify result when validator validates request.
        assertTrue(restAuthority.isAuthorized(mockRequest));
    }

    /** Tests method to verify authorization failure. */
    public void testIsNotAuthorized() {
        // Create mock reqeust.
        final HttpRequest mockRequest = context.mock(HttpRequest.class);
        final RequestLine requestLine = new BasicRequestLine("GET", "/remote/hello", HttpVersion.HTTP_1_1);
        final Header[] headers = new Header[0];
        
        context.checking(new Expectations() {{
            allowing(mockRequest).getRequestLine();
            will(returnValue(requestLine));
            
            allowing(mockRequest).getHeaders(OAuthRequest.AUTH_HEADER);
            will(returnValue(headers));
            
            allowing(mockValidator);
            will(throwException(new OAuthException()));
        }});
        
        // Verify result when validator fails request.
        assertFalse(restAuthority.isAuthorized(mockRequest));
    }
}
